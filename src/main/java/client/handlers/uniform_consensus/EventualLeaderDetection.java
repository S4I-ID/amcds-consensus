package client.handlers.uniform_consensus;

import client.handlers.HandlerInterface;
import client.model.HandleResult;
import client.model.ProcessingResult;
import client.model.SystemContext;
import client.protobuf.EldMessageUtils;
import client.protobuf.MessageUtils;
import client.protobuf.P;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventualLeaderDetection extends HandlerInterface {
    private static final Logger log = LogManager.getLogger(EventualLeaderDetection.class);

    private P.ProcessId leader;
    private final Map<P.ProcessId, Boolean> suspected;
    private final EventuallyPerfectFailureDetector eventuallyPerfectFailureDetector;

    public EventualLeaderDetection(SystemContext context, String baseAbstractionId) {
        String abstractionId = String.format("%s.eld", baseAbstractionId);
        super(context, abstractionId);

        this.leader = null;
        this.suspected = new HashMap<>();
        context.getProcesses().forEach(processId -> suspected.put(processId, false));
        this.eventuallyPerfectFailureDetector = new EventuallyPerfectFailureDetector(context, abstractionId);
    }


    @Override
    public ProcessingResult handle(P.Message message, P.ProcessId sender) {
        try {
            switch (message.getType()) {
                case EPFD_SUSPECT -> {
                    P.ProcessId suspect = message.getEpfdSuspect().getProcess();
                    log.debug("[{}] - EPFD_SUSPECT - {} | {}", abstractionId, MessageUtils.processIdToString(suspect), message.getMessageUuid());
                    suspected.put(suspect, true);
                }

                case EPFD_RESTORE -> {
                    P.ProcessId restored = message.getEpfdRestore().getProcess();
                    log.debug("[{}] - EPFD_RESTORE - {} | {}", abstractionId, MessageUtils.processIdToString(restored), message.getMessageUuid());
                    suspected.put(restored, false);
                }

                default -> {
                    return new ProcessingResult(HandleResult.UNABLE_TO_PROCESS, String.format("Message type not supported in %s - %s", abstractionId, message.getType()), message);
                }
            }

            P.ProcessId highest = getHighestRankedAliveProcess();
            log.debug("[{}] - Found highest ranked process - {}", abstractionId, MessageUtils.processIdToString(highest));
            if (leader != highest) {
                leader = highest;
                log.debug("[{}] - Updated leader to {}", abstractionId, MessageUtils.processIdToShortString(leader));
                if (leader != context.getSelfProcessId()) {
                    log.debug("[{}] - Sending leader ELD trust", abstractionId);
                    context.getMessageQueue().put(EldMessageUtils.createELDTrustMessage(
                            highest, abstractionId, MessageUtils.removeOneAbstractionLevel(abstractionId), context.getSystemId()));
                }
            }
            return new ProcessingResult(HandleResult.MESSAGE_OK);
        } catch (Exception e) {
            return new ProcessingResult(HandleResult.ERROR, e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()), message);
        }

    }

    public P.ProcessId getHighestRankedAliveProcess() {
        List<P.ProcessId> processes = context.getProcesses();
        P.ProcessId highest = processes.getFirst();

        for (P.ProcessId processId : processes) {
            if (!suspected.get(processId)) {
                if (processId.getRank() > highest.getRank()) {
                    highest = processId;
                }
            }
        }
        return highest;
    }

    public EventuallyPerfectFailureDetector getEventuallyPerfectFailureDetector() {
        return eventuallyPerfectFailureDetector;
    }
}
