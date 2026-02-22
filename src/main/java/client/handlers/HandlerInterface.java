package client.handlers;

import client.model.ProcessingResult;
import client.model.SystemContext;
import client.protobuf.MessageUtils;
import client.protobuf.P;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.Function;

public abstract class HandlerInterface implements Function<P.Message, ProcessingResult> {
    private static final Logger log = LogManager.getLogger(HandlerInterface.class);

    protected SystemContext context;
    protected String abstractionId;

    public abstract ProcessingResult handle(P.Message message, P.ProcessId sender);

    public HandlerInterface(SystemContext context, String abstractionId) {
        this.context = context;
        this.abstractionId = abstractionId;
    }

    @Override
    public ProcessingResult apply(P.Message message) {
        return handle(MessageUtils.getInternalMessage(message), getSenderFromDeliverMessage(message));
    }

    private P.ProcessId getSenderFromDeliverMessage(P.Message message) {
        P.ProcessId senderId = switch (message.getType()) {
            case PL_DELIVER -> validateProcessIdWithContext(message.getPlDeliver().getSender());
            case BEB_DELIVER -> validateProcessIdWithContext(message.getBebDeliver().getSender());
            case NETWORK_MESSAGE -> getProcessIdFromSenderAddress(
                    message.getNetworkMessage().getSenderHost(), message.getNetworkMessage().getSenderListeningPort());
            default -> context.getSelfProcessId();
        };
        log.trace("[{}] - {} - {}/{} - sender {} | {}", abstractionId, message.getType(), message.getPlDeliver().getMessage().getType(),
                message.getBebDeliver().getMessage().getType(), MessageUtils.processIdToShortString(senderId), message.getMessageUuid());
        return senderId;
    }

    private P.ProcessId validateProcessIdWithContext(P.ProcessId givenId) {
        if (!context.getProcesses().contains(givenId)) {
            return findProcessInContextByAddress(givenId.getHost(), givenId.getPort()).orElse(givenId);
        }
        return givenId;
    }

    private P.ProcessId getProcessIdFromSenderAddress(String host, int port) {
        return findProcessInContextByAddress(host, port).orElse(P.ProcessId.newBuilder()
                .setHost(host)
                .setPort(port)
                .build());
    }

    private Optional<P.ProcessId> findProcessInContextByAddress(String host, int port) {
        Optional<P.ProcessId> foundId = context.getProcesses().stream()
                .filter(processId -> processId.getHost().equals(host) && processId.getPort() == port)
                .findFirst();
        log.trace("[{}] - foundId {} - for given {}:{}", abstractionId, MessageUtils.processIdToShortString(foundId.orElse(null)), host, port);
        return foundId;
    }
}