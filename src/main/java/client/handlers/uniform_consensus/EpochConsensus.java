package client.handlers.uniform_consensus;

import client.handlers.HandlerInterface;
import client.handlers.broadcast.BestEffortBroadcast;
import client.handlers.point_link.PerfectLink;
import client.model.HandleResult;
import client.model.ProcessingResult;
import client.model.SystemContext;
import client.protobuf.BroadcastMessageUtils;
import client.protobuf.EpochConsensusMessageUtils;
import client.protobuf.MessageUtils;
import client.protobuf.P;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

public class EpochConsensus extends HandlerInterface {
    private static final Logger log = LogManager.getLogger(EpochConsensus.class);

    private P.Value tempValue;
    private Integer epochTimestamp;
    private Integer accepted;
    private P.EpInternalState state;
    private Boolean aborted;
    private Map<P.ProcessId, P.EpInternalState> states;
    private P.ProcessId leader;

    private BestEffortBroadcast bestEffortBroadcast;
    private PerfectLink perfectLink;

    public EpochConsensus(SystemContext context, String baseAbstractionId, Integer epochTimestamp, P.EpInternalState state, P.ProcessId leader) {
        String abstractionId = String.format("%s.ep[%s]", baseAbstractionId, epochTimestamp);
        super(context, abstractionId);

        this.epochTimestamp = epochTimestamp;
        this.tempValue = MessageUtils.createValue(-1);
        this.accepted = 0;
        this.aborted = false;
        this.state = state;
        this.leader = leader;
        this.states = new HashMap<>(context.getProcesses().size() + 6);

        this.bestEffortBroadcast = new BestEffortBroadcast(context, abstractionId);
        this.perfectLink = new PerfectLink(context, abstractionId);
    }

    @Override
    public ProcessingResult handle(P.Message message, P.ProcessId sender) {
        if (aborted) {
            log.debug("[{}] - Consensus aborted, late message from {} will be discarded", abstractionId, MessageUtils.processIdToShortString(sender));
            return new ProcessingResult(HandleResult.UNABLE_TO_PROCESS, "Epoch " + abstractionId + " is aborted. ", message);
        }

        ArrayBlockingQueue<P.Message> messageQueue = context.getMessageQueue();
        String systemId = context.getSystemId();
        int processesCount = context.getProcesses().size();
        try {
            switch (message.getType()) {
                case EP_PROPOSE -> { // only leader
                    tempValue = message.getEpPropose().getValue();
                    log.info("[{}] - EP_PROPOSE - VALUE {} | {}", abstractionId, tempValue, message.getMessageUuid());
                    messageQueue.put(BroadcastMessageUtils.createBebBroadcastMessage(EpochConsensusMessageUtils.createEPInternalReadMessage(
                            abstractionId, abstractionId, systemId), abstractionId, abstractionId));
                }

                case EP_INTERNAL_READ -> {
                    log.info("[{}] - EP_INTERNAL READ - state {} - to LEADER {}", abstractionId, state, MessageUtils.processIdToShortString(leader));
                    messageQueue.put(MessageUtils.createPlSendMessage(EpochConsensusMessageUtils.createEPInternalStateMessage(
                            state, abstractionId, abstractionId, systemId), abstractionId, abstractionId, leader));
                }

                case EP_INTERNAL_STATE -> { // only leader
                    P.EpInternalState state = message.getEpInternalState();
                    log.info("[{}] - EP_INTERNAL_STATE - RECEIVED state {} | {}", abstractionId, state, message.getMessageUuid());
                    states.put(sender, state);
                    log.debug("[{}] - EP_INTERNAL_STATE - ALL STATES: {}", abstractionId, states);

                    if (states.size() > processesCount / 2) {
                        P.EpInternalState highestState = state;
                        for (var pid : states.keySet()) {
                            if (states.get(pid).getValueTimestamp() > highestState.getValueTimestamp()) {
                                highestState = states.get(pid);
                            }
                        }

                        if (highestState.getValue().getDefined()) {
                            tempValue = highestState.getValue();
                        }
                        log.debug("[{}] - EP_INTERNAL_STATE - HIGHEST state: {}", abstractionId, tempValue);
                        states = new HashMap<>(processesCount + 6);
                        messageQueue.put(BroadcastMessageUtils.createBebBroadcastMessage(EpochConsensusMessageUtils.createEPInternalWriteMessage(
                                tempValue, abstractionId, abstractionId, systemId), abstractionId, abstractionId));
                    }
                }

                case EP_INTERNAL_WRITE -> {
                    Integer newValue = MessageUtils.readValue(message.getEpInternalWrite().getValue());
                    log.info("[{}] - EP_INTERNAL_WRITE - value {} | {}", abstractionId, newValue, message.getMessageUuid());
                    state = EpochConsensusMessageUtils.createEPInternalState(newValue, epochTimestamp);
                    log.debug("[{}] - EP_INTERNAL_WRITE - NEW state {}", abstractionId, state);
                    messageQueue.put(MessageUtils.createPlSendMessage(EpochConsensusMessageUtils.createEPInternalAcceptMessage(
                            abstractionId, abstractionId, systemId), abstractionId, abstractionId, leader));
                }

                case EP_INTERNAL_ACCEPT -> {
                    log.info("[{}} - EP_INTERNAL_ACCEPT - sender {}", abstractionId, MessageUtils.processIdToShortString(sender));
                    accepted = accepted + 1;
                    if (accepted > processesCount / 2) {
                        accepted = 0;
                        messageQueue.put(BroadcastMessageUtils.createBebBroadcastMessage(EpochConsensusMessageUtils.createEPInternalDecidedMessage(
                                        tempValue, abstractionId, abstractionId, systemId), abstractionId, abstractionId));
                    }
                }

                case EP_INTERNAL_DECIDED -> {
                    P.Value value = message.getEpInternalDecided().getValue();
                    log.info("[{}] - EP_INTERNAL_DECIDED - EPOCH TIMESTAMP {} | {}", abstractionId, value, message.getMessageUuid());
                    messageQueue.put(EpochConsensusMessageUtils.createEPDecideMessage(
                            value, epochTimestamp, abstractionId, MessageUtils.removeOneAbstractionLevel(abstractionId), systemId));
                }

                case EP_ABORT -> {
                    log.info("[{}] - EP_ABORT - RECEIVED TS {} - DURING TS {} | {}", abstractionId, message.getEpAborted().getValueTimestamp(),
                            state.getValueTimestamp(), message.getMessageUuid());
                    messageQueue.put(EpochConsensusMessageUtils.createEPAbortedMessage(
                            message.getEpAborted().getValueTimestamp(), state, abstractionId, MessageUtils.removeOneAbstractionLevel(abstractionId), systemId));
                    aborted = true;
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

    public PerfectLink getPerfectLink() {
        return perfectLink;
    }

    public BestEffortBroadcast getBestEffortBroadcastUnit() {
        return bestEffortBroadcast;
    }
}
