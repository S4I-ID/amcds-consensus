package client.handlers.broadcast;

import client.handlers.HandlerInterface;
import client.handlers.point_link.PerfectLink;
import client.model.HandleResult;
import client.model.ProcessingResult;
import client.model.SystemContext;
import client.protobuf.MessageUtils;
import client.protobuf.P;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class BestEffortBroadcast extends HandlerInterface {
    private static final Logger log = LogManager.getLogger(BestEffortBroadcast.class);

    private final PerfectLink perfectLink;

    public BestEffortBroadcast(SystemContext context, String baseAbstractionId) {
        String abstractionId = String.format("%s.beb", baseAbstractionId);
        super(context, abstractionId);
        this.perfectLink = new PerfectLink(context, abstractionId);
    }

    @Override
    public ProcessingResult handle(P.Message message, P.ProcessId sender) {
        ArrayBlockingQueue<P.Message> messageQueue = context.getMessageQueue();
        log.trace("[{}] {} - sent from {}", abstractionId, message, sender);
        try {
            switch (message.getType()) {
                case NETWORK_MESSAGE -> {
                    log.debug("[{}] - NETWORK MESSAGE | {}", abstractionId, message.getMessageUuid());
                    messageQueue.put(MessageUtils.unmarshalMessage(message));
                }
                case BEB_BROADCAST -> {
                    log.info("[{}] - BEB_BROADCAST - to {} | {}", abstractionId, message.getToAbstractionId(), message.getMessageUuid());
                    context.getProcesses().forEach(processId -> {
                        try {
                            messageQueue.put(MessageUtils.createPlSendMessage(message.getBebBroadcast().getMessage(), abstractionId, abstractionId, processId));
                        } catch (InterruptedException exception) {
                            log.error(exception);
                        }
                    });
                }
                default -> {
                    return new ProcessingResult(HandleResult.UNABLE_TO_PROCESS, String.format("Message type not supported in %s - %s", abstractionId, message.getType()), message);
                }
            }
            return new ProcessingResult(HandleResult.MESSAGE_OK);
        }
        catch (Exception e) {
            return new ProcessingResult(HandleResult.ERROR, e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()), message );
        }
    }

    public PerfectLink getPerfectLink() {
        return perfectLink;
    }
}
