import java.io.Serializable;

public enum MessageClassification implements Serializable{
        APPLICATION,

        MARKER,

        FINISHED,

        WASTE,

        LOCALSTATE;
}
