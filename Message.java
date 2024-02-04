
import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
	
    private static final long serialVersionUID = 1L;

    private final int id;
    private final ArrayList<local_state> local;
    private final MessageClassification classification;
    private int messageIdentification = 0;

    public Message(final int id, final ArrayList<local_state> local_state, final MessageClassification classification) {
        this.id = id;
        this.local = local_state;
        this.classification = classification;
    }

    public Message(final int id, final ArrayList<local_state> local_state, final MessageClassification classification, final int messageId) {
        this.id = id;
        this.local = local_state;
        this.classification = classification;
        this.messageIdentification = messageId;
    }

    public ArrayList<local_state> getLocal() {
        return local;
    }

    public MessageClassification getClassification() {
        return classification;
    }

    public int getmessageId() {
        return messageIdentification;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Id : " + id
                + " Data : " + local
                + " Type : " + classification;
    }

}
