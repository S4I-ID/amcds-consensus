package client.model;

public class SocketPairData {
    private final SocketData hub;
    private final SocketData self;

    public SocketPairData(SocketData hub, SocketData self) {
        this.hub = hub;
        this.self = self;
    }

    public SocketData getHub() {
        return hub;
    }

    public SocketData getSelf() {
        return self;
    }

    @Override
    public String toString() {
        return String.format("H/%s - P/%s", hub, self);
    }
}
