package client.handlers.uniform_consensus;

import client.handlers.HandlerInterface;
import client.handlers.broadcast.BestEffortBroadcast;
import client.handlers.point_link.PerfectLink;
import client.model.HandleResult;
import client.model.ProcessingResult;
import client.model.SystemContext;
import client.protobuf.BroadcastMessageUtils;
import client.protobuf.EpochChangeMessageUtils;
import client.protobuf.MessageUtils;
import client.protobuf.P;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class EpochChange extends HandlerInterface {
    private static final Logger log = LogManager.getLogger(EpochChange.class);

    private P.ProcessId trusted;
    private Integer timestamp;
    private Integer lastTimestamp;

    private EventualLeaderDetection eventualLeaderDetector;
    private BestEffortBroadcast bestEffortBroadcast;
    private PerfectLink perfectLink;

    public EpochChange(SystemContext context, String baseAbstractionId) {
        String abstractionId = baseAbstractionId + ".ec";
        super(context, abstractionId);

        this.context = context;
        this.trusted = null;
        for (var pid : context.getProcesses()) {
            log.debug("EC LEADER EL {} - RANK {}", MessageUtils.processIdToShortString(pid), pid.getRank());
            if (trusted == null) {
                this.trusted = pid;
            } else if (pid.getRank() > trusted.getRank()) {
                this.trusted = pid;
            }
        }
        log.debug("EC LEADER FINAL - {}", MessageUtils.processIdToString(trusted));

        this.lastTimestamp = 0;
        this.timestamp = context.getSelfProcessId().getRank();
        this.eventualLeaderDetector = new EventualLeaderDetection(context, abstractionId);
        this.bestEffortBroadcast = new BestEffortBroadcast(context, abstractionId);
        this.perfectLink = new PerfectLink(context, abstractionId);
    }

    @Override
    public ProcessingResult handle(P.Message message, P.ProcessId sender) {
        ArrayBlockingQueue<P.Message> messageQueue = context.getMessageQueue();
        P.ProcessId selfProcessId = context.getSelfProcessId();
        int processesCount = context.getProcesses().size();
        try {
            switch (message.getType()) {
                case ELD_TRUST -> {
                    trusted = message.getEldTrust().getProcess();
                    log.info("[{}] - ELD_TRUST - TRUSTED {}:{} | {}", abstractionId, trusted.getHost(), trusted.getPort(), message.getMessageUuid());

                    if (message.getEldTrust().getProcess() == selfProcessId) {
                        timestamp = timestamp + processesCount;
                        log.debug("[{}] - Trusted self, new timestamp - {}", abstractionId, timestamp);
                        messageQueue.put(BroadcastMessageUtils.createBebBroadcastMessage(EpochChangeMessageUtils.createECInternalNewEpoch(
                                timestamp, abstractionId, abstractionId, context.getSystemId()), abstractionId, abstractionId));
                    }
                }

                case EC_INTERNAL_NEW_EPOCH -> {
                    log.info("[{}] - EC_INTERNAL_NEW_EPOCH - TRUSTED {} - SENDER {} | {}", abstractionId, MessageUtils.processIdToShortString(trusted),
                            MessageUtils.processIdToShortString(sender), message.getMessageUuid());
                    log.info("[{}] - LAST TIMESTAMP {} - IS SENDER TRUSTED? {}", abstractionId, lastTimestamp, trusted == sender);

                    if (trusted == sender && message.getEcInternalNewEpoch().getTimestamp() > lastTimestamp) {
                        lastTimestamp = message.getEcInternalNewEpoch().getTimestamp();
                        log.debug("[{}] - EC_INTERNAL_NEW_EPOCH_INT - LAST TIMESTAMP {}", abstractionId, lastTimestamp);
                        messageQueue.put(EpochChangeMessageUtils.createECStartEpochMessage(sender, message.getEcInternalNewEpoch().getTimestamp(),
                                abstractionId, MessageUtils.removeOneAbstractionLevel(abstractionId), context.getSystemId()
                        ));
                    } else {
                        log.debug("[{}] - EC_INTERNAL_NEW_EPOCH_INT - RETURN NACK", abstractionId);
                        messageQueue.put(MessageUtils.createPlSendMessage(EpochChangeMessageUtils.createECInternalNAckMessage(
                                abstractionId, abstractionId, message.getSystemId()), abstractionId, abstractionId, sender));
                    }
                }

                case EC_INTERNAL_NACK -> {
                    log.info("[{}] - EC_INTERNAL_NACK - IS SELF TRUSTED? {} | {}", abstractionId, trusted == selfProcessId, message.getMessageUuid());
                    if (trusted == selfProcessId) {
                        timestamp = timestamp + context.getProcesses().size();
                        log.info("[{}] - EC_INTERNAL_NACK - TRUSTED - NEW TIMESTAMP {}", abstractionId, timestamp);
                        messageQueue.put(BroadcastMessageUtils.createBebBroadcastMessage(EpochChangeMessageUtils.createECInternalNewEpoch(
                                timestamp, abstractionId, abstractionId, message.getSystemId()), abstractionId, abstractionId));
                    }
                }

                default -> {
                    return new ProcessingResult(HandleResult.UNABLE_TO_PROCESS, String.format("Message type not supported in %s - %s", abstractionId, message.getType()), message);
                }
            }
            return new ProcessingResult(HandleResult.MESSAGE_OK);
        } catch (Exception e) {
            return new ProcessingResult(HandleResult.ERROR, e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()), message);
        }
    }

    public EventualLeaderDetection getEventualLeaderDetector() {
        return eventualLeaderDetector;
    }

    public PerfectLink getPerfectLink() {
        return perfectLink;
    }

    public BestEffortBroadcast getBestEffortBroadcastUnit() {
        return bestEffortBroadcast;
    }

}
