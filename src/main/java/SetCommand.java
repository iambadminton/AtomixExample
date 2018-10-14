import io.atomix.copycat.Command;

import java.io.Serializable;

/**
 * Created by Sanya on 14.10.2018.
 */
public class SetCommand implements Command<String> {
    String key;
    String value;

    public SetCommand(String value) {
        this.value = value;
    }
}
