package client;

import client.handlers.point_link.PerfectLink;
import client.model.SocketPairData;
import client.model.SystemContext;
import client.protobuf.MessageUtils;
import client.protobuf.NodeMessageUtils;
import client.protobuf.P;
import client.threads.NodeNetworkThread;
import client.threads.SystemThread;
import client.threads.ThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

public class Node {
    private static final Logger log = LogManager.getLogger(Node.class);
    private static final String ABSTRACTION_ID = "app";

    private final SocketPairData socketPair;
    private final String refName;
    private final Integer index;

    private final ArrayBlockingQueue<P.Message> messages;
    private final Map<String, SystemThread> systemMap;
    private NodeNetworkThread nodeNetworkThread;

    public Node(SocketPairData socketPair, String refName, Integer index) {
        this.socketPair = socketPair;
        this.refName = refName;
        this.index = index;

        this.messages = new ArrayBlockingQueue<>(8192, true);
        this.systemMap = new LinkedHashMap<>();
    }

    public void start() {
        ThreadFactory threadFactory = ThreadFactory.getInstance();
        SystemContext nodeContext = createNewSystemContext(messages, null, socketPair, null, null);

        nodeNetworkThread = threadFactory.createNodeNetworkReaderThread(nodeContext, ABSTRACTION_ID);
        nodeNetworkThread.start();

        registerNodeToHub(nodeNetworkThread.getPerfectLink());
        handleNodeMessages();
    }

    /**
     * Registers node to hub
     */
    private void registerNodeToHub(PerfectLink perfectLink) {
        log.info("Registering with {} as {}", socketPair, refName);
        P.Message registrationMessage = NodeMessageUtils.createRegistrationMessage(socketPair.getHub(), refName, index);
        perfectLink.apply(registrationMessage);
    }

    /**
     * <p> Process messages received by the node network reader thread </p>
     * <p> Check message type, create or shutdown systems according to type </p>
     * <p> Pass other messages to appropriate subsystems according to received systemId </p>
     */
    private void handleNodeMessages() {
        while (true) {
            try {
                P.Message message = messages.take();
                String systemId = message.getSystemId();
                log.trace("[{}] - Taken message - {}", systemId, message);
                log.debug("[{}] Taken message of type {} | {} |", systemId, message.getType(), message.getMessageUuid());

                switch (message.getNetworkMessage().getMessage().getType()) {
                    case PROC_INITIALIZE_SYSTEM -> {
                        List<P.ProcessId> processes = message.getNetworkMessage().getMessage().getProcInitializeSystem().getProcessesList();
                        String processesString = processes.stream()
                                .map(MessageUtils::processIdToString)
                                .collect(Collectors.joining("\n  > "));
                        log.info("New system with processes - {}", processesString);
                        P.ProcessId selfProcessId = getSelfProcessIdFromList(processes);
                        log.info("Current process is - {}", MessageUtils.processIdToString(selfProcessId));

                        SystemContext newSystemContext = createNewSystemContext(
                                new ArrayBlockingQueue<>(8192, true), processes, socketPair, systemId, selfProcessId);
                        SystemThread newSystem = ThreadFactory.getInstance().createSystemThread(newSystemContext);

                        systemMap.put(systemId, newSystem);
                        newSystem.start();
                    }
                    case PROC_DESTROY_SYSTEM -> {
                        systemMap.get(systemId).join(1000);
                        systemMap.remove(systemId);
                        log.warn("Closed system - {}", systemId);
                    }
                    default -> {
                        log.trace("Adding message {} - to system {}", message.getMessageUuid(), systemId);
                        systemMap.get(systemId).addMessage(message);
                    }
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    private P.ProcessId getSelfProcessIdFromList(List<P.ProcessId> processes) {
        return processes.stream()
                .filter(pid -> pid.getOwner().equals(refName) && pid.getIndex() == index)
                .findFirst()
                .orElse(null);
    }

    private SystemContext createNewSystemContext(ArrayBlockingQueue<P.Message> messageQueue, List<P.ProcessId> processes, SocketPairData socketPair,
                                                 String systemId, P.ProcessId selfProcessId) {
        return new SystemContext(messageQueue, processes, socketPair, systemId, selfProcessId);
    }
}