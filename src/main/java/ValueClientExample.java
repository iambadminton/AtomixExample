/**
 * Created by Sanya on 14.10.2018.
 */
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.client.ConnectionStrategies;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.copycat.client.RecoveryStrategies;
import io.atomix.copycat.client.ServerSelectionStrategies;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Value client example. Expects at least 1 argument:
 * <p>
 * <ul>
 * <li>host:port pairs - the host address and port of cluster members</li>
 * </ul>
 * <p>Example cluster arguments: <pre>10.0.1.10:5000 10.0.1.11:5001 10.0.1.12:5002</pre>
 * <p>Example single node arguments: <pre>localhost:5000</pre>
 *
 * @author <a href="http://github.com/kuujo>Jordan Halterman</a>
 */
public class ValueClientExample {
    /**
     * Starts the client.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1)
            throw new IllegalArgumentException("must supply a set of host:port tuples");
        // Build a list of all member addresses to which to connect.
        List<Address> members = new ArrayList<>();
        for (String arg : args) {
            String[] parts = arg.split(":");
            members.add(new Address(parts[0], Integer.valueOf(parts[1])));
        }
        CopycatClient client = CopycatClient.builder()
                .withTransport(new NettyTransport())
                .withConnectionStrategy(ConnectionStrategies.FIBONACCI_BACKOFF)
                .withRecoveryStrategy(RecoveryStrategies.RECOVER)
                .withServerSelectionStrategy(ServerSelectionStrategies.LEADER)
                .withSessionTimeout(Duration.ofSeconds(15))
                .build();
        client.serializer().register(SetCommand.class, 1);
        client.serializer().register(GetQuery.class, 2);
        client.serializer().register(DeleteCommand.class, 3);
        client.connect(members).join();
        recursiveSet(client);
        while (client.state() != CopycatClient.State.CLOSED) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    /**
     * Recursively sets state machine values.
     */
    private static void recursiveSet(CopycatClient client) {
        client.submit(new SetCommand(UUID.randomUUID().toString())).whenComplete((result, error) -> {
            client.context().schedule(Duration.ofSeconds(100), () -> recursiveSet(client));
        });
    }
}