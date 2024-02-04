
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;


public class Node {

    private static final long WAIT_TIME = 3000L;

    private int id;
    private HashMap<Integer, NodeInfo> nodelist;
    private boolean chkinit;
    private NodeInfo host_node;
  
    private ServerSocket listenerSocket;
    private ArrayList<Integer> neighbors;

    public Node() {
    }   


    public void initNode(String config, int nodeId) throws IOException {
        id = nodeId;
        chkinit = nodeId == 0;
        nodelist = ProjectConfig.getNodeMap();
        host_node = nodelist.get(nodeId);
        neighbors = ProjectConfig.getNeighborNodes();
        listenerSocket = new ServerSocket(host_node.getPortNumber());
    }

    public void establishConnections() throws InterruptedException, IOException {
        Listener listener = new Listener(listenerSocket, neighbors);
        Thread listenerThread = new Thread(listener);
        listenerThread.start();
        Thread.sleep(WAIT_TIME);

        for (Integer nodeId : neighbors) {

            if (nodeId <= id || NetworkComponents.hasSocketEntry(nodeId)) {
                continue;
            }

            addToNetworkComponents(nodeId);
        }

        while (NetworkComponents.getSocketMapSize() < neighbors.size()) {
            //System.out.println(NetworkComponents.getSocketMapSize() + " Waiting... " + neighbors.size());
            Thread.sleep(WAIT_TIME);
        }

        listenerThread.interrupt();
    }

    private void addToNetworkComponents(int nodeId) throws UnknownHostException, IOException {
        NodeInfo info = nodelist.get(nodeId);

        //ConfigGlobal.log("Connecting... " + nodeId + " - " + info);
        boolean connected = false;
        Socket sock = null;
        while (!connected) {
            try {
                sock = new Socket(info.getHostName(), info.getPortNumber());
                connected = true;
            } catch (ConnectException ce) {
                ConfigGlobal.log("Connection Retrying...");
            }
        }
        ConfigGlobal.log("Connected successfully : " + nodeId);

        NetworkComponents.addSocketEntry(nodeId, sock);

        ByteBuffer dbuf = ByteBuffer.allocate(4);
        dbuf.putInt(id);
        ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
        byte[] bytes = dbuf.array();
        oos.write(bytes);
        oos.flush();
        oos.reset();
        NetworkComponents.addOutputStreamEntry(nodeId, oos);
        NetworkComponents.addInputStreamEntry(nodeId, new ObjectInputStream(sock.getInputStream()));
    }

    @Override
    public String toString() {
        return "Selected Node : " + id
                + "\nHost Node : " + host_node
                + "Neighbor Nodes: " + neighbors
                + "\nNode Lists : " + nodelist;
    }

    private ArrayList<Thread> launchReceiverThreads() throws InterruptedException {
        ArrayList<Thread> receiverThreadPool = new ArrayList<>();
        for (Integer neighborId : neighbors) {
            ObjectInputStream stream = NetworkComponents.getReaderStream(neighborId);
            recvMsg receiver = new recvMsg(stream, neighbors);
            Thread thread = new Thread(receiver);
            thread.start();
            receiverThreadPool.add(thread);
        }
        Thread.sleep(WAIT_TIME);
        return receiverThreadPool;

    }


    private Thread launchSenderThread() {
        SendMsg sender = new SendMsg();
        Thread thread = new Thread(sender);
        thread.start();
        return thread;
    }

    public static void main(String[] args) {

        int id = Integer.parseInt(args[0]);
        String configFileName = args[1];

        ConfigGlobal.initlog(ProjectConfig.getLogFileName(id, configFileName));
        ProjectConfig.setupApplicationEnvironment(configFileName, id);

        Node cNode = new Node();
        try {
            cNode.initNode(configFileName, id);
            ConfigGlobal.log(cNode.toString());

            cNode.establishConnections();
            cNode.launchReceiverThreads();
            cNode.launchSenderThread();

            ConfigGlobal.seton(id % 2 == 0);

            cNode.termdetector();

            while(!ConfigGlobal.isSystemTerminated()) {
            }
            Thread.sleep(WAIT_TIME);
            System.exit(0);

        } catch (IOException e) {
            System.err.println("Cannot proceed.");
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }

        System.exit(0);
    }

    private Thread termdetector() {
        if (!chkinit) {
            return null;
        }
        TermDetector snapshotInitiator = new TermDetector(neighbors);
        Thread thread = new Thread(snapshotInitiator);
        thread.start();
        return thread;
    }

}
