package client.handlers.uniform_consensus;

import client.handlers.HandlerInterface;
import client.model.HandleResult;
import client.model.ProcessingResult;
import client.model.SystemContext;
import client.protobuf.MessageUtils;
import client.protobuf.P;
import client.protobuf.UniformConsensusMessageUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Function;

public class UniformConsensus extends HandlerInterface {
    private static final Logger log = LogManager.getLogger(UniformConsensus.class);

    private Map<String, Function<P.Message, ProcessingResult>> abstractions;
    private P.ProcessId leader;
    private P.ProcessId newLeader;
    private P.Value value;
    private Integer epochTimestamp;
    private Integer newTimestamp;
    private Boolean proposed;
    private Boolean decided;

    private EpochChange epochChange;
    private EpochConsensus currentEpoch;

    public UniformConsensus(SystemContext context, Map<String, Function<P.Message, ProcessingResult>> abstractions, String baseAbstractionId, String topic) {
        String abstractionId = String.format("%s.uc[%s]", baseAbstractionId, topic);
        super(context, abstractionId);
        this.abstractions = abstractions;

        this.value = MessageUtils.createValue(-1);
        this.proposed = false;
        this.decided = false;
        this.epochTimestamp = 0;
        this.newTimestamp = 0;
        this.newLeader = null;
        this.leader = context.getSelfProcessId();
        for (var pid : context.getProcesses()) {
            if (leader.getRank() < pid.getRank())
                leader = pid;
        }
        this.epochChange = new EpochChange(context, abstractionId);
        this.currentEpoch = null;
    }

    private String getCurrentEpochConsensusAbstractionId() {
        return String.format("%s.ep[%s]", abstractionId, epochTimestamp);
    }

    public void createNewEpochConsensus(P.EpInternalState state) {
        this.currentEpoch = new EpochConsensus(context, abstractionId, epochTimestamp, state, leader);
        log.debug("[{}] - NEW EPOCH AT TIMESTAMP {}", abstractionId, epochTimestamp);
        abstractions.put(getCurrentEpochConsensusAbstractionId(), (P.Message message) -> currentEpoch.apply(message));
        abstractions.put(getCurrentEpochConsensusAbstractionId() + ".beb", (P.Message message) -> currentEpoch.getBestEffortBroadcastUnit().apply(message));
        abstractions.put(getCurrentEpochConsensusAbstractionId() + ".beb.pl", (P.Message message) -> currentEpoch.getBestEffortBroadcastUnit().getPerfectLink().apply(message));
        abstractions.put(getCurrentEpochConsensusAbstractionId() + ".pl", (P.Message message) -> currentEpoch.getPerfectLink().apply(message));
        log.trace("[{}} - New epoch consensus abstractions {}", abstractionId, abstractions.keySet());
    }


    @Override
    public ProcessingResult handle (P.Message message, P.ProcessId sender) {
        ArrayBlockingQueue<P.Message> messageQueue = context.getMessageQueue();
        String systemId = context.getSystemId();
        try {
            switch (message.getType()) {
                case UC_PROPOSE -> {
                    value = message.getUcPropose().getValue();
                    log.info("[{}] - UC_PROPOSE - value {} | {}", abstractionId, value, message.getMessageUuid());
                }

                case EC_START_EPOCH -> {
                    newTimestamp = message.getEcStartEpoch().getNewTimestamp();
                    newLeader = message.getEcStartEpoch().getNewLeader();
                    log.info("[{}] - EC_START_EPOCH - NEW TIMESTAMP {} WITH LEADER {} | {}", abstractionId, newTimestamp,
                            MessageUtils.processIdToString(newLeader), message.getMessageUuid());
                    messageQueue.put(UniformConsensusMessageUtils.createEPAbortMessage(abstractionId, getCurrentEpochConsensusAbstractionId(), systemId));
                }

                case EP_ABORTED -> {
                    int abortedEpochTimestamp = message.getEpAborted().getEts();
                    log.info("[{}] - EP_ABORTED - ABORTED EPOCH TIMESTAMP {} - CURRENT TIMESTAMP {} | {}", abstractionId, abortedEpochTimestamp,
                            epochTimestamp, message.getMessageUuid());
                    if (epochTimestamp == abortedEpochTimestamp) {
                        epochTimestamp = newTimestamp;
                        leader = newLeader;
                        proposed = false;
                        createNewEpochConsensus(UniformConsensusMessageUtils.createEPInternalState(message.getEpAborted().getValue(), epochTimestamp));
                    }
                }

                case EP_DECIDE -> {
                    int decideTimestamp = message.getEpDecide().getEts();
                    P.Value decidedValue = message.getEpDecide().getValue();
                    log.info("[{}] - EP_DECIDE - TIMESTAMP {} VALUE {} - DECIDED {} FOR CURRENT TIMESTAMP {} | {}",
                            abstractionId, decideTimestamp, decidedValue, decided, epochTimestamp, message.getMessageUuid());

                    if (epochTimestamp == decideTimestamp)
                        if (!decided) {
                            decided = true;
                            messageQueue.put(UniformConsensusMessageUtils.createUCDecideMessage(decidedValue, abstractionId, "app", systemId));
                        }
                }

                default -> {
                    return new ProcessingResult(HandleResult.UNABLE_TO_PROCESS, String.format("Message type not supported in %s - %s", abstractionId, message.getType()), message);
                }
            }

            if (leader == context.getSelfProcessId() && value.getDefined() && !proposed ) {
                log.info("[{}] - AM LEADER, WILL PROPOSE VALUE {}", abstractionId, value);
                proposed = true;
                messageQueue.put(UniformConsensusMessageUtils.createEPProposeMessage(value, abstractionId, getCurrentEpochConsensusAbstractionId(), systemId));
            }
            return new ProcessingResult(HandleResult.MESSAGE_OK);
        }
        catch (Exception e) {
            return new ProcessingResult(HandleResult.ERROR, e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()), message );
        }
    }

    public EpochChange getEpochChange() {
        return epochChange;
    }
}
