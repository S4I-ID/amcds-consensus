package client.model;

public class SocketData {
    private final String ipAddress;
    private final Integer port;

    public SocketData(String ipAddress, Integer port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public String toString() {
        return ipAddress + ":" + port;
    }
}
