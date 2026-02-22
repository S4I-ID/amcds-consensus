package client.handlers.app;

import client.handlers.HandlerInterface;
import client.handlers.broadcast.BestEffortBroadcast;
import client.handlers.point_link.PerfectLink;
import client.model.HandleResult;
import client.model.ProcessingResult;
import client.model.SocketPairData;
import client.model.SystemContext;
import client.protobuf.AppBaseMessageUtils;
import client.protobuf.BroadcastMessageUtils;
import client.protobuf.MessageUtils;
import client.protobuf.P;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class AppBase extends HandlerInterface {
    private static final Logger log = LogManager.getLogger(AppBase.class);
    private static final String ABSTRACTION_ID = "app";

    private final PerfectLink perfectLink;
    private final BestEffortBroadcast bestEffortBroadcast;

    public AppBase(SystemContext context) {
        super(context, ABSTRACTION_ID);
        this.perfectLink = new PerfectLink(context, ABSTRACTION_ID);
        this.bestEffortBroadcast = new BestEffortBroadcast(context, ABSTRACTION_ID);
    }

    @Override
    public ProcessingResult handle(P.Message message, P.ProcessId sender) {
        ArrayBlockingQueue<P.Message> messageQueue = context.getMessageQueue();
        String systemId = context.getSystemId();
        SocketPairData socketPair = context.getSocketPair();
        try {
            log.trace("[{}] {} - sent from {}", abstractionId, message, sender);
            switch (message.getType()) {
                case APP_BROADCAST -> {
                    log.info("APP_BROADCAST | {}", message.getMessageUuid());
                    messageQueue.put(BroadcastMessageUtils.createBebBroadcastMessage(
                            AppBaseMessageUtils.createAppValueMessage(message, systemId), abstractionId, abstractionId));
                }

                case APP_VALUE -> {
                    log.info("APP_VALUE - value {} | {} |", message.getAppValue().getValue().getV(), message.getMessageUuid());
                    messageQueue.put(MessageUtils.createPlSendMessage(
                            AppBaseMessageUtils.createAppValueMessage(message, systemId), abstractionId, abstractionId,
                            MessageUtils.createHubProcessId(socketPair.getHub())));
                }

                case APP_WRITE -> {
                    log.info("APP_WRITE - value {} | {} |", message.getAppWrite().getValue().getV(), message.getMessageUuid());
                    messageQueue.put(AppBaseMessageUtils.createNNARWriteMessage(message));
                }

                case APP_READ -> {
                    log.info("APP_READ - to register {} | {} |", message.getAppRead().getRegister(), message.getMessageUuid());
                    messageQueue.put(AppBaseMessageUtils.createNNARReadMessage(message));
                }

                case NNAR_READ_RETURN -> {
                    log.info("NNAR_READ_RETURN - value {} | {} | ", message.getNnarReadReturn().getValue().getV(), message.getMessageUuid());
                    messageQueue.put(MessageUtils.createPlSendMessage(AppBaseMessageUtils.createAppReadReturnMessage(message),
                            abstractionId, abstractionId, MessageUtils.createHubProcessId(socketPair.getHub())));
                }

                case NNAR_WRITE_RETURN -> {
                    log.info("NNAR_WRITE_RETURN | {}", message.getMessageUuid());
                    messageQueue.put(MessageUtils.createPlSendMessage(AppBaseMessageUtils.createAppWriteReturnMessage(message),
                            abstractionId, abstractionId, MessageUtils.createHubProcessId(socketPair.getHub())));
                }

                case APP_PROPOSE -> {
                    P.Value proposedValue = message.getAppPropose().getValue();
                    String topic = message.getAppPropose().getTopic();
                    log.info("APP_PROPOSE - value {} - topic {} | {} |", proposedValue.getV(), topic, message.getMessageUuid());
                    messageQueue.put(AppBaseMessageUtils.createUCProposeMessage(proposedValue, abstractionId, String.format("%s.uc[%s]", abstractionId, topic), systemId));
                }

                case UC_DECIDE -> {
                    log.info("UC_DECIDE - value {} | {} |", message.getUcDecide().getValue().getV(), message.getMessageUuid());
                    messageQueue.put(MessageUtils.createPlSendMessage(AppBaseMessageUtils.createAppDecideMessage(
                            message.getUcDecide().getValue(), abstractionId, abstractionId, systemId),
                            abstractionId, abstractionId, MessageUtils.createHubProcessId(socketPair.getHub())));
                }

                default -> {
                    return new ProcessingResult(HandleResult.UNABLE_TO_PROCESS, String.format("Message type not supported in %s - %s", abstractionId, message.getType()), message);
                }
            }
            return new ProcessingResult(HandleResult.MESSAGE_OK);
        }
        catch (Exception e) {
            return new ProcessingResult(HandleResult.ERROR, e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()), message);
        }
    }

    public PerfectLink getPerfectLink() {
        return perfectLink;
    }

    public BestEffortBroadcast getBestEffortBroadcast() {
        return bestEffortBroadcast;
    }

}

