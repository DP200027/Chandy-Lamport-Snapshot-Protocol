
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

public class TermDetector implements Runnable {

    private static int markerId = 1;
    private static class PayloadComparator implements Comparator<local_state> {

        public PayloadComparator() {
        }

        @Override
        public int compare(local_state p1, local_state p2) {
            return p1.getId() - p2.getId();
        }

    }

    private static final long SNAPSHOT_DELAY = ConfigGlobal.delay_snap;
    private static final PayloadComparator PAYLOAD_COMPARATOR = new PayloadComparator();
    private static final int ID = ConfigGlobal.id;
    private static final int CLUSTER_SIZE = ConfigGlobal.size_of_map;

    private final ArrayList<Integer> neighbors;

    public volatile boolean isRunning = true;

    public TermDetector(final ArrayList<Integer> neighbors) {
        this.neighbors = neighbors;
    }

    public void sendMarkerMessages() {
        ConfigGlobal.log("Starting snapshot...");
        
        int[] localClock = new int[CLUSTER_SIZE];
        synchronized (ConfigGlobal.vectorclock) {
            System.arraycopy(ConfigGlobal.vectorclock, 0, localClock, 0, CLUSTER_SIZE);
        }

        local_state myPayload = new local_state(ID, localClock,
                ConfigGlobal.chkon(), ConfigGlobal.getno_of_sent_messages(),
                ConfigGlobal.getno_of_rec_messages());
        ConfigGlobal.log(myPayload.toString());
        ConfigGlobal.add_localstate(myPayload);
        broadcastMessage(MessageClassification.MARKER);
        markerId++;
    }

    private void sendFinishMessages() {
        ConfigGlobal.log("Sent finish messages");
        broadcastMessage(MessageClassification.FINISHED);
    }

    private void broadcastMessage(MessageClassification type) {
        Message snapshotMessage = new Message(ID, null, type, markerId);
        for (Integer neighborId : neighbors) {
            SnapshotSender snapshotSender = new SnapshotSender(neighborId, snapshotMessage);
            Thread thread = new Thread(snapshotSender);
            thread.start();
        }
    }

    @Override
    public void run() {

        while (isRunning) {

            sendMarkerMessages();

            while (!ConfigGlobal.snaprepchk()) {
            }

            TreeSet<local_state> replyPayloadList = new TreeSet<>(PAYLOAD_COMPARATOR);
            replyPayloadList.addAll(ConfigGlobal.getAll_localstate());

            StringBuilder builder = new StringBuilder("Snapshot\n");
            for (local_state p : replyPayloadList) {
                builder.append(p.toString() + "\n");
            }
            //builder.append("...............................");
            ConfigGlobal.log(builder.toString());

            if (isSystemTerminated(replyPayloadList)) {
                ConfigGlobal.log("System is terminated");
                sendFinishMessages();
                ConfigGlobal.setIsSystemTerminated(true);
                break;
            }
            else {
                //ConfigGlobal.log("********************NOT terminated...");
            }

            
            ConfigGlobal.reset_snap();

            
            try {
                ConfigGlobal.log("Snapshot process is in wait mode" + SNAPSHOT_DELAY);
                Thread.sleep(SNAPSHOT_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }

        }
    }

    private boolean isSystemTerminated(TreeSet<local_state> payloads) {
        return isAllPassive(payloads) && isChannelsEmpty(payloads);
    }

    private boolean isAllPassive(TreeSet<local_state> payloads) {
        boolean isAnyActive = false;
        for (local_state payload : payloads) {
            isAnyActive |= payload.isActive();
        }
        return !isAnyActive;
    }

    private boolean isChannelsEmpty(TreeSet<local_state> payloads) {
        int totalSentCount = 0, totalReceiveCount = 0;
        for (local_state payload : payloads) {
            totalReceiveCount += payload.getReceivedMsgCount();
            totalSentCount += payload.getSentMsgCount();
        }
        return totalReceiveCount - totalSentCount == 0;
    }
    
}
