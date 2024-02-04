
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class ProjectConfig {


    private static HashMap<Integer, NodeInfo> nodeMap = new HashMap<>();
    private static ArrayList<Integer> neighborNodes = new ArrayList<>();
    
    private static final char COMMENT = '#';
    public static void setupApplicationEnvironment(String configFileName, int id) {

        ConfigGlobal.id = id;
        Scanner lineScanner = null;
        try (
                Scanner scanner = new Scanner(new File(configFileName))
            ) {

            String input = getNextValidInputLine(scanner);

            lineScanner = new Scanner(input);
            int clusterSize = lineScanner.nextInt();
            ConfigGlobal.size_of_map = clusterSize;
            ConfigGlobal.vectorclock = new int[clusterSize];
            int minPerActive = lineScanner.nextInt();
            ConfigGlobal.minActive = minPerActive;
            int maxPerActive = lineScanner.nextInt();
            ConfigGlobal.maxActive = maxPerActive;
            int minSendDelay = lineScanner.nextInt();
            ConfigGlobal.mindelay_send = minSendDelay;
            int snapshotDelay = lineScanner.nextInt();
            ConfigGlobal.delay_snap = snapshotDelay;
            int maxNumber = lineScanner.nextInt();
            ConfigGlobal.maxno = maxNumber;
            lineScanner.close();

            input = getNextValidInputLine(scanner);

            lineScanner = new Scanner(input);
            int nodeNumber = lineScanner.nextInt();
            String machineName = lineScanner.next();
            int port = lineScanner.nextInt();
            NodeInfo info = new NodeInfo(machineName, port);
            nodeMap.put(nodeNumber, info);

            for (int i = 1; i < clusterSize; i++) {
                String line = scanner.nextLine();

                lineScanner = new Scanner(line);
                nodeNumber = lineScanner.nextInt();
                machineName = lineScanner.next();
                port = lineScanner.nextInt();

                info = new NodeInfo(machineName, port);
                nodeMap.put(nodeNumber, info);
            }
            lineScanner.close();

            input = getNextValidInputLine(scanner);

            int lineNumber = 0;
            ArrayList<Integer> neighbors = new ArrayList<>();
            while (input != null) {
                lineScanner = new Scanner(input);
                if (lineNumber != id) {
                    while (lineScanner.hasNext()) {
                        String neighbor = lineScanner.next();
                        if (neighbor.charAt(0) == COMMENT) {
                            break;
                        }
                        int neighborId = Integer.parseInt(neighbor);
                        if (neighborId == id && !neighbors.contains(lineNumber) && lineNumber != id) {
                            neighbors.add(lineNumber);
                        }
                    }
                } else {
                    while (lineScanner.hasNext()) {
                        String neighbor = lineScanner.next();
                        if (neighbor.charAt(0) == COMMENT) {
                            break;
                        }
                        int neighborId = Integer.parseInt(neighbor);
                        if (!neighbors.contains(neighborId) && neighborId != id) {
                            neighbors.add(neighborId);
                        }
                    }
                }

                input = getNextValidInputLine(scanner);
                lineScanner.close();
                lineNumber++;
            }
            neighborNodes = neighbors;
            ConfigGlobal.no_of_neighbours = neighbors.size();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static String getNextValidInputLine(Scanner scanner) {
        String input = null;
        while (scanner.hasNext()) {
            input = scanner.nextLine();
            if(input.isEmpty()) {
                continue;
            }
            if(input.charAt(0) != COMMENT) {
                break;
            }
       }
        return input;
    }

    public static HashMap<Integer, NodeInfo> getNodeMap() {
        return nodeMap;
    }


    public static String getLogFileName(final int nodeId, final String configFileName) {
        String fileName = Paths.get(configFileName).getFileName().toString();
        return String.format("%s-%s.out", 
                fileName.substring(0, fileName.lastIndexOf('.')), nodeId);
    }

    public static ArrayList<Integer> getNeighborNodes() {
        return neighborNodes;
    }
    

}
