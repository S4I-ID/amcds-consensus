package client.model;

import client.protobuf.P;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class SystemContext {
    private final ArrayBlockingQueue<P.Message> messageQueue;
    private final List<P.ProcessId> processes;
    private final SocketPairData socketPair;
    private final String systemId;
    private final P.ProcessId selfProcessId;

    public SystemContext(ArrayBlockingQueue<P.Message> messageQueue, List<P.ProcessId> processes, SocketPairData socketPair, String systemId, P.ProcessId selfProcessId) {
        this.messageQueue = messageQueue;
        this.processes = processes;
        this.socketPair = socketPair;
        this.systemId = systemId;
        this.selfProcessId = selfProcessId;
    }
    public ArrayBlockingQueue<P.Message> getMessageQueue() {
        return messageQueue;
    }

    public List<P.ProcessId> getProcesses() {
        return processes;
    }

    public SocketPairData getSocketPair() {
        return socketPair;
    }

    public String getSystemId() {
        return systemId;
    }

    public P.ProcessId getSelfProcessId() {
        return selfProcessId;
    }
}
