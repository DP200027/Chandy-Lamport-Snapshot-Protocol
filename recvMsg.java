
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class recvMsg implements Runnable {

    private static final int ID = ConfigGlobal.id;
    private static final int CLUSTER_SIZE = ConfigGlobal.size_of_map;

    private final ObjectInputStream inputStream;
    private final ArrayList<Integer> neighbors;
    private final int neighborCount;
    private final int expectedSnapshotReplies;

    public volatile boolean isRunning = true;

    public recvMsg(final ObjectInputStream inputStream,
            final ArrayList<Integer> neighbors) {
        this.inputStream = inputStream;
        this.neighbors = neighbors;
        this.neighborCount = neighbors.size();
        this.expectedSnapshotReplies = ID == 0 ? this.neighborCount : this.neighborCount - 1;
    }

    @Override
    public void run() {

        while(isRunning) {
            try {
                Message message = (Message) inputStream.readObject();
                MessageClassification type = message.getClassification();
                if (type.equals(MessageClassification.APPLICATION)) {
                    handleApplicationMessage(message);
                }
                else if(type.equals(MessageClassification.MARKER)) {
                    handleMarkerMessage(message);
                }
                else if(type.equals(MessageClassification.FINISHED)) {
                    handleFinishMessage(message);
                }
                else {
                    handleSnapshotReplyMessage(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleFinishMessage(Message message) {
        Message broadcastMarkerMsg = new Message(ID, null, MessageClassification.FINISHED);
        for (Integer neighborId : neighbors) {
            if (neighborId != message.getId()) {
                launchSnapshotSender(neighborId, broadcastMarkerMsg);
            }
        }
        ConfigGlobal.setIsSystemTerminated(true);
        isRunning = false;
    }

    
    private void handleSnapshotReplyMessage(Message message) {
        
        ConfigGlobal.incrementReceivedSnapshotReplies();

        if(message.getClassification().equals(MessageClassification.WASTE)) {
            //ConfigGlobal.log("Received IGNORED reply from " + message.getId());
            
        }
        else {
            
            ConfigGlobal.add_all_local_state(message.getLocal());
            ConfigGlobal.log("Received LOCAL_STATE reply from " + message.getId() 
                    + " ---> Received payload : " + message.getLocal());
        }
        
        if((ConfigGlobal.getReceivedSnapshotReplies() == expectedSnapshotReplies)) { 

            
            if (ID == 0) {
                ConfigGlobal.setsnaprep(true);
            }
            else {
                
                //ConfigGlobal.log("Received expected number of replies");
                ArrayList<local_state> snapshotPayload = new ArrayList<>();
                snapshotPayload.addAll(ConfigGlobal.getAll_localstate());

                Message replyStateMsg = new Message(ID, snapshotPayload,
                        MessageClassification.LOCALSTATE);
                int markerSenderNode = ConfigGlobal.getMarkerSenderNode();
                ConfigGlobal.log("Send snapshot reply to " + markerSenderNode 
                        + " ---> Message : " + replyStateMsg);
                launchSnapshotSender(markerSenderNode, replyStateMsg);

            }

        }
    }

    
    private void handleApplicationMessage(Message message) {
        
        ConfigGlobal.increcv_message();
        mergeVectorClocks(message);

        ConfigGlobal.log("Received application message : " + message 
                + "\nMerged clock : " + ConfigGlobal.printGlobalClock());

        if (ConfigGlobal.chkon()) {
            
            //ConfigGlobal.log("Already active...");
            return;
        }
        if (ConfigGlobal.getno_of_sent_messages() >= ConfigGlobal.maxno) {
            
            ConfigGlobal.log("Maximum send limit reached");
            return;
        }

        
        //ConfigGlobal.log("Becoming active...");
        ConfigGlobal.seton(true);
    }

    
    private void handleMarkerMessage(Message message) {
        ConfigGlobal.incrementMarkersReceivedSoFar();

        if (ConfigGlobal.recmark.contains(message.getmessageId()) || ID == 0) {
            
            ConfigGlobal.log("Marker msg received from " + message.getId());
            Message replyMessage =  new Message(ID, null, MessageClassification.WASTE);
            launchSnapshotSender(message.getId(), replyMessage);
        }
        else {
            
            ConfigGlobal.recmark.add(message.getmessageId());
            ConfigGlobal.reset_snap();
            ConfigGlobal.setmarkermsgrecv(true);
            int[] localClock = new int[CLUSTER_SIZE];
            synchronized (ConfigGlobal.vectorclock) {
                System.arraycopy(ConfigGlobal.vectorclock, 0, localClock, 0, CLUSTER_SIZE);
            }

            local_state myPayload = new local_state(ID, localClock,
                    ConfigGlobal.chkon(), ConfigGlobal.getno_of_sent_messages(),
                    ConfigGlobal.getno_of_rec_messages());
            ConfigGlobal.log("Recording state : " + myPayload.toString());
            ConfigGlobal.add_localstate(myPayload);

            ConfigGlobal.setMarkerSenderNode(message.getId());
            ConfigGlobal.log("Marker msg received from " + message.getId() +"\n"
                    + "Expecting replies = " + expectedSnapshotReplies);
            if(expectedSnapshotReplies == 0) {
                
                ConfigGlobal.log("Received expected number of replies");
                ArrayList<local_state> snapshotPayload = new ArrayList<>();
                snapshotPayload.addAll(ConfigGlobal.getAll_localstate());

                Message replyStateMsg = new Message(ID, snapshotPayload,
                        MessageClassification.LOCALSTATE);
                int markerSenderNode = ConfigGlobal.getMarkerSenderNode();
                ConfigGlobal.log("Send snapshot reply to " + markerSenderNode 
                        + " ---> Message : " + replyStateMsg);
                launchSnapshotSender(markerSenderNode, replyStateMsg);

                return;
            }
            
            Message broadcastMarkerMsg = new Message(ID, null, MessageClassification.MARKER, message.getmessageId());
            for (Integer neighborId : neighbors) {
                if (neighborId != message.getId()) {
                    launchSnapshotSender(neighborId, broadcastMarkerMsg);
                }
            }
        }
    }

    
    private void mergeVectorClocks(Message message) {
        int[] piggybackVectorClock = message.getLocal().get(0).getVectorClock();
        synchronized (ConfigGlobal.vectorclock) {
            for (int i = 0; i < CLUSTER_SIZE; i++) {
                ConfigGlobal.vectorclock[i] = Math.max(ConfigGlobal.vectorclock[i], piggybackVectorClock[i]);
            }
            ConfigGlobal.vectorclock[ID]++;
        }
    }

    
    private void launchSnapshotSender(int id, Message message) {
        SnapshotSender snapshotSender = new SnapshotSender(id, message);
        Thread thread = new Thread(snapshotSender);
        thread.start();
    }
    
}
