/**
 * Created by Sanya on 14.10.2018.
 */

import io.atomix.Atomix;
import io.atomix.AtomixReplica;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.coordination.DistributedLeaderElection;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.core.AtomixBuilder;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.oracle.jrockit.jfr.ContentType.Address;

/**
 * Leader election example.
 *
 * @author <a href="http://github.com/kuujo>Jordan Halterman</a>
 */
public class LeaderElectionExample {
    /**
     * Starts the server.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2)
            throw new IllegalArgumentException("must supply a path and set of host:port tuples");

        // Parse the address to which to bind the server.
        String[] mainParts = args[1].split(":");
        Address address = new Address(mainParts[0], Integer.valueOf(mainParts[1]));

        // Build a list of all member addresses to which to connect.
        List<Address> members = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            String[] parts = args[i].split(":");
            members.add(new Address(parts[0], Integer.valueOf(parts[1])));
        }

        // Create a stateful Atomix replica. The replica communicates with other replicas in the cluster
        // to replicate state changes.
        Atomix atomix = AtomixReplica.builder(address, members)
                .withTransport(new NettyTransport())
                .withStorage(new Storage(new File(args[0])))
                .build();

        // Open the replica. Once this operation completes resources can be created and managed.
        atomix.open().join();

        // Create a leader election resource.
        DistributedLeaderElection election = atomix.create("election", DistributedLeaderElection.class).get();

        // Register a callback to be called when this election instance is elected the leader
        election.onElection(epoch -> {
            System.out.println("Elected leader!");
        }).join();

        // Block while the replica is open.
        while (atomix.isOpen()) {
            Thread.sleep(1000);
        }


        }

    }

}

