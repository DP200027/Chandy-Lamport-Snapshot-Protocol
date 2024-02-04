import java.util.Objects;


public class NodeInfo {

        private final String hostName;
        private final int portNumber;

        public NodeInfo(
                final String hostName,
                final int portNumber) {

            this.hostName = hostName;
            this.portNumber = portNumber;
        }

        public String getHostName() {
            return hostName;
        }

        public int getPortNumber() {
            return portNumber;
        }

        @Override
        public String toString() {
            return hostName + ":" + portNumber + "\n";
        }

        @Override
        public int hashCode() {
            return Objects.hash(hostName, portNumber);
        }

        @Override
        public boolean equals(Object other) {
            if(!(other instanceof NodeInfo))
                return false;

            NodeInfo that = (NodeInfo) other;
            return this.hostName.equalsIgnoreCase(that.hostName)
                && this.portNumber == that.portNumber;
        }

    }