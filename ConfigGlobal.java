
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;


public class ConfigGlobal {

    public static final Random RANDOM = new Random();

    public static int cntr = 0;
    
    public static boolean on = false;
    public static int no_of_messages_sent = 0;
    public static int no_of_messages_rec = 0;

    
    public static boolean snapshotrep = false;
    public static boolean systemterm = false;
    public static boolean recvMarkMsg = false;
    public static boolean recvSnapReply = false;
    public static int receivedSnapshotReplies = 0;
    public static int sendMark;
    public static int recvMark = 0;
    public static HashSet<Integer> recmark = new HashSet<>();



    
    public static int id;
    public static int[] vectorclock;
    public static long mindelay_send;
    public static long delay_snap;
    public static int size_of_map;
    public static int minActive;
    public static int maxActive;
    public static int maxno;
    public static int no_of_neighbours;


    public static ArrayList<local_state> local = new ArrayList<>();

    private static FileWriter log = null;

    public static void initlog(String filename) {
        if(log != null) {
            return;
        }
        try {
            log = new FileWriter(filename);
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
        }
    }

    public static synchronized void log(String msg) {
        if(log == null) {
            initlog("config-log.out");
        }

        try {
            String prependString = cntr == 0 ? "" : "\n";
            log.write(prependString + msg);
            log.flush();
            cntr++;
        } catch (IOException e) {
            System.err.println(msg);
        }
    }

    public static String printGlobalClock() {
        StringBuilder builder = new StringBuilder("VectorClock : ( ");
        for(int i = 0; i < vectorclock.length; i++) {
            builder.append(vectorclock[i] + " ");
        }
        builder.append(")");
        return builder.toString();

    }

    public static synchronized int[] getvectclock() {
        return vectorclock;
    }

    public static synchronized int getno_of_sent_messages() {
        return no_of_messages_sent;
    }

    public static synchronized void incsent_message() {
        no_of_messages_sent++;
    }

    public static synchronized int getno_of_rec_messages() {
        return no_of_messages_rec;
    }

    public static synchronized void increcv_message() {
        no_of_messages_rec++;
    }

    public static synchronized boolean chkon() {
        return on;
    }

    public static synchronized void seton(boolean isNodeActive) {
        on = isNodeActive;
    }

    
    public static synchronized boolean recvMarkerMsg() {
        if(!recvMarkMsg) {
            if((recvMark % no_of_neighbours) == 1) {
                recvMarkMsg = true;
            }
            return false;
        }
        return recvMarkMsg;
    }

    public static synchronized boolean repsnap() {
        if (!snapshotrep) {
            snapshotrep = true;
            return false;
        }
        return snapshotrep;
    }

    public static synchronized void setmarkermsgrecv(boolean markerReceived) {
        recvMarkMsg = markerReceived;
    }

    public static synchronized boolean snaprepchk() {
        return recvSnapReply;
    }

    public static synchronized void setsnaprep(boolean allSnapshotReceived) {
        recvSnapReply = allSnapshotReceived;
    }

    public static synchronized boolean isSystemTerminated() {
        return systemterm;
    }

    public static synchronized void setIsSystemTerminated(boolean systemTerminated) {
        systemterm = systemTerminated;
    }

    public static synchronized int getMarkerSenderNode() {
        return sendMark;
    }

    public static synchronized void setMarkerSenderNode(int markerSender) {
        sendMark = markerSender;
    }

    public static synchronized int getReceivedSnapshotReplies() {
        return receivedSnapshotReplies;
    }

    public static synchronized void incrementReceivedSnapshotReplies() {
        receivedSnapshotReplies++;
    }

    public static synchronized int getMarkersReceivedSoFar() {
        return recvMark;
    }

    public static synchronized void incrementMarkersReceivedSoFar() {
        recvMark++;
    }

    public static synchronized ArrayList<local_state> getAll_localstate() {
        return local;
    }

    public static synchronized void add_all_local_state(ArrayList<local_state> payload) {
        local.addAll(payload);
    }

    public static synchronized void add_localstate(local_state payload) {
        local.add(payload);
    }

    public static synchronized void reset_snap() {
        local.clear();
        local = new ArrayList<>();
        receivedSnapshotReplies = 0;
        recvSnapReply = false;
        recvMarkMsg = id == 0;
        snapshotrep = false;
    }
}
