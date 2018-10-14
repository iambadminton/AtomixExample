/*import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.AtomixBuilder;
import io.atomix.core.AtomixConfig;
import io.atomix.core.profile.Profile;
import io.atomix.core.profile.ProfileConfig;
import io.atomix.primitive.partition.PartitionGroup;
import io.atomix.protocols.backup.partition.PrimaryBackupPartitionGroup;*/

import io.atomix.AtomixClient;
import io.atomix.AtomixReplica;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;


import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;



/**
 * Created by Sanya on 14.10.2018.
 */
public class PrimaryExample {
    public static void main(String[] args) {
        Storage storage = Storage.builder().withDirectory(new File("logs"))
                .withStorageLevel(StorageLevel.DISK).build();
       /* AtomixReplica replica = AtomixReplica.builder(
                new Address("localhost", 8700))
                .withStorage(storage)
                .withTransport(new NettyTransport())
                .build();
        CompletableFuture<AtomixReplica> future = replica.bootstrap();
        future.join();

        AtomixReplica replica2 = AtomixReplica.builder(new Address("localhost", 8701)).withStorage(storage)
                .withTransport(new NettyTransport()).build();
        replica2.join(new Address("localhost",8700)).join();

        AtomixReplica replica3 = AtomixReplica.builder(new Address("localhost", 8701)).withStorage(storage)
                .withTransport(new NettyTransport()).build();
        replica3.join(new Address("localhost", 8700), new Address("localhost", 8701)).join();*/
        List<Address> cluster = Arrays.asList(
                new Address("localhost", 8700),
                new Address("localhost", 8701),
                new Address("localhsot", 8702));

        AtomixReplica replica1 = AtomixReplica
                .builder(cluster.get(0))
                .build();
        replica1.bootstrap(cluster).join();

        AtomixReplica replica2 = AtomixReplica
                .builder(cluster.get(1))
                .build();

        replica2.bootstrap(cluster).join();

        AtomixReplica replica3 = AtomixReplica
                .builder(cluster.get(2))
                .build();

        replica3.bootstrap(cluster).join();

        AtomixClient client = AtomixClient.builder().withTransport(new NettyTransport()).build();

        client.connect(cluster).thenRun(() -> {System.out.println("Client is connected to the cluster");});

        replica1.getMap("map")
                .thenCompose(m -> m.put("bar", "Hello world!"))
                .thenRun(() -> System.out.println("Value is set in Distributed Map"))
                .join();

    }

        /*AtomixBuilder builder = Atomix.builder();
        //builder.withMemberId("member1").build();
        builder.withMembershipProvider(BootstrapDiscoveryProvider.builder()
                .withNodes(
                        Node.builder()
                                .withId("member1")
                                .withAddress("10.192.19.181")
                                .build(),
                        Node.builder()
                                .withId("member2")
                                .withAddress("10.192.19.182")
                                .build(),
                        Node.builder()
                                .withId("member3")
                                .withAddress("10.192.19.183")
                                .build())
                .build());

        builder.addProfile(Profile.dataGrid());
        Atomix atomix = builder.build();
        atomix.start().join();

    }*/
}
