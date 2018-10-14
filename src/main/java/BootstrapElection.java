/**
 * Created by Sanya on 14.10.2018.
 */
import com.google.common.io.Files;
import io.atomix.Atomix;
import io.atomix.cluster.Node;
import io.atomix.cluster.NodeId;
import io.atomix.core.election.LeaderElection;
import io.atomix.core.election.LeadershipEventListener;
import io.atomix.messaging.Endpoint;
import io.atomix.primitive.Consistency;
import io.atomix.primitive.Persistence;
import io.atomix.primitive.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type.Node;

public class BootstrapElection {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapElection.class);

    public static void main(String[] args) {
       /* Node[] bootstrapNodes = new Node[]{
                Node.builder("server1")
                        .withType(Node.Type.DATA)
                        .withEndpoint(Endpoint.from("127.0.0.1", 5001))
                        .build(),
                Node.builder("server2")
                        .withType(Node.Type.DATA)
                        .withEndpoint(Endpoint.from("127.0.0.1", 5002))
                        .build(),
                Node.builder("server3")
                        .withType(Node.Type.DATA)
                        .withEndpoint(Endpoint.from("127.0.0.1", 5003))
                        .build()
        };*/

       /* Atomix atomix1 = createAtomixNode("server1", 5001, bootstrapNodes);
        Atomix atomix2 = createAtomixNode("server2", 5002, bootstrapNodes);
        Atomix atomix3 = createAtomixNode("server3", 5003, bootstrapNodes);*/

        /*CompletableFuture.allOf(
                atomix1.start(),
                atomix2.start(),
                atomix3.start()
        ).whenCompleteAsync((aVoid, throwable) -> {
            logger.info("completed ", throwable);
        }).join();*/

        Atomix atomix = Atomix.builder().withAddress("10.192.19.180:5679").withMulticast.enabled().build();


        LeaderElection<NodeId> election1 = createElection(atomix1);
        LeaderElection<NodeId> election2 = createElection(atomix2);
        LeaderElection<NodeId> election3 = createElection(atomix3);

        logger.info("client joining cluster");

        Atomix client = createAtomixNode("client", 5004, bootstrapNodes, Node.Type.CLIENT);

        CompletableFuture
                .allOf(client.start())
                .whenCompleteAsync((aVoid, throwable) -> {
                    logger.info("completed adding client", throwable);
                })
                .join();

        election1.run(atomix1.clusterService().getLocalNode().id());
        election2.run(atomix2.clusterService().getLocalNode().id());
        election3.run(atomix3.clusterService().getLocalNode().id());

        NodeId leaderId = election1.getLeadership().leader().id();

        Atomix leader = Stream.of(atomix1, atomix2, atomix3)
                .filter(atomix -> atomix.clusterService().getLocalNode().id().equals(leaderId))
                .findFirst().get();

        CountDownLatch latch = new CountDownLatch(1);
        LeadershipEventListener<NodeId> listener = event -> {
            latch.countDown();
        };

        election1.addListener(listener);
        election2.addListener(listener);
        election3.addListener(listener);

        logger.info("Stopping leader");
        leader.stop().join();
        long leaderStoppedTime = System.currentTimeMillis();
        logger.info("Leader stopped");

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.exit(0);
        }

        if (latch.getCount() == 1) {
            logger.info("Failed to elect new leader");
            System.exit(0);
        }

        long electionTime = System.currentTimeMillis() - leaderStoppedTime;
        logger.info("New leader election time: {}", electionTime);

        System.exit(0);
    }

    private static Atomix createAtomixNode(String nodeId, int port, Node[] bootstrapNodes) {
        return createAtomixNode(nodeId, port, bootstrapNodes, Node.Type.DATA);
    }

    private static Atomix createAtomixNode(String nodeId, int port, Node[] bootstrapNodes, Node.Type type) {
        return Atomix.builder()
                .withLocalNode(Node.builder(nodeId)
                        .withType(type)
                        .withEndpoint(Endpoint.from("127.0.0.1", port))
                        .build())
                .withDataDirectory(Files.createTempDir())
                .withBootstrapNodes(bootstrapNodes)
                .build();
    }

    private static LeaderElection<NodeId> createElection(Atomix atomix) {
        return atomix.<NodeId>leaderElectionBuilder("leaderElection")
                .withElectionTimeout(100)
                .withPersistence(Persistence.EPHEMERAL)
                .withConsistency(Consistency.LINEARIZABLE)
                .withReplication(Replication.SYNCHRONOUS)
                .build();
    }
}
