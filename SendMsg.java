
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class SendMsg implements Runnable {

    private static final ArrayList<Integer> NEIGHBORS = ProjectConfig.getNeighborNodes();
    private static final int MIN_PER_ACTIVE = ConfigGlobal.minActive;
    private static final int MAX_PER_ACTIVE = ConfigGlobal.maxActive;
    private static final int DIFF = MAX_PER_ACTIVE - MIN_PER_ACTIVE;
    private static final long MIN_SEND_DELAY = ConfigGlobal.mindelay_send;
    private static final HashMap<Integer, ObjectOutputStream> OUTPUTSTREAM_MAP = NetworkComponents.writerStreamMap;
    private static final Random RANDOM_GENERATOR = ConfigGlobal.RANDOM;
    private static final int ID = ConfigGlobal.id;
    private static final int CLUSTER_SIZE = ConfigGlobal.size_of_map;

    public static volatile boolean isRunning = true;

    public SendMsg() {
    }

    @Override
    public void run() {
        while (isRunning) {

            if (!ConfigGlobal.chkon()) {
                continue;
            }

            int numOfMsg = RANDOM_GENERATOR.nextInt(DIFF) + MIN_PER_ACTIVE;
            sendApplicationMessages(numOfMsg);

            ConfigGlobal.seton(false);

            if (ConfigGlobal.getno_of_sent_messages() >= ConfigGlobal.maxno) {

                isRunning = false;
            }
        }

    }

   
    private void sendApplicationMessages(int numOfMsg) {
        for (int i = 0; i < numOfMsg; i++) {

            int nextNodeId = selectRandomNeighbor();

            
            ObjectOutputStream outputStream = OUTPUTSTREAM_MAP.get(nextNodeId);
            Message message = null;
            int[] localClock = new int[CLUSTER_SIZE];
            synchronized (ConfigGlobal.vectorclock) {
                ConfigGlobal.vectorclock[ID]++;
                System.arraycopy(ConfigGlobal.vectorclock, 0, localClock, 0, CLUSTER_SIZE);
            }
            local_state p = new local_state(localClock);
            ArrayList<local_state> payloads = new ArrayList<>();
            payloads.add(p);
            message = new Message(ID, payloads, MessageClassification.APPLICATION);

            try {
                synchronized (outputStream) {
                    outputStream.writeObject(message);
                }
                ConfigGlobal.incsent_message();

                if (ConfigGlobal.getno_of_sent_messages() >= ConfigGlobal.maxno) {
                    isRunning = false;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(MIN_SEND_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    private int selectRandomNeighbor() {
        int size = NEIGHBORS.size();
        while(true) {
            int random = RANDOM_GENERATOR.nextInt(100) % size;
            return NEIGHBORS.get(random);
        }
    }
}
