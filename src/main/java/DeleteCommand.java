import io.atomix.copycat.Command;

/**
 * Created by Sanya on 14.10.2018.
 */
public class DeleteCommand implements Command<String> {
    String key;
}
