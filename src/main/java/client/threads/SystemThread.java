package client.threads;

import client.model.ProcessingResult;
import client.model.SystemContext;
import client.protobuf.MessageUtils;
import client.protobuf.P;
import client.service.AbstractionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;

public class SystemThread extends Thread {
    private static final Logger log = LogManager.getLogger(SystemThread.class);

    private final SystemContext context;
    private final AbstractionService abstractionService;

    public SystemThread(SystemContext systemContext, AbstractionService abstractionService) {
        this.context = systemContext;
        this.abstractionService = abstractionService;
    }

    public void addMessage(P.Message message) {
        while (true) {
            ArrayBlockingQueue<P.Message> messageQueue = context.getMessageQueue();
            try {
                messageQueue.add(message);
                log.trace("[{}] - added message - current system queue size: {}", context.getSystemId(), messageQueue.size());
                if (messageQueue.size() > 100) {
                    log.warn("Messages not consumed: {}", messageQueue.size());
                }
                break;
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    @Override
    public void run() {
        this.setName("SystemThread");
        while (true) {
            ArrayBlockingQueue<P.Message> messageQueue = context.getMessageQueue();
            try {
                P.Message message = messageQueue.take();
                log.trace("Started processing message - messages left in queue {}", messageQueue.size());

                P.Message internalMessage = MessageUtils.getInternalMessage(message);
                String toAbstraction = internalMessage.getToAbstractionId();

                log.trace("Processing message {} - with internal message {} - to {}", message, internalMessage, toAbstraction);
                log.debug("Processing message | {} | {}/{} to {}", message.getMessageUuid(), message.getType(), internalMessage.getType(), toAbstraction);

                ProcessingResult result = abstractionService.sendMessageToAbstraction(toAbstraction, message);

                switch (result.getResult()) {
                    case MESSAGE_OK -> log.debug("RESULT {} FOR {} | {}/{} to {}",
                            result, message.getMessageUuid(), message.getType(), internalMessage.getType(), toAbstraction);

                    case PROCESS_LATER -> {
                        messageQueue.put(result.getMessage());
                        log.debug("MESSAGE REINSERTED | {} - {} | {}", message.getType(), internalMessage.getType(), message.getMessageUuid());
                    }

                    case UNABLE_TO_PROCESS -> log.warn("{} | {} |", result, message.getMessageUuid());

                    case ERROR -> log.error("{} | {} |", result, message.getMessageUuid());
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }
}
