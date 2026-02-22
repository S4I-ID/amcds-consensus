package client.handlers.uniform_consensus;

import client.handlers.HandlerInterface;
import client.handlers.point_link.PerfectLink;
import client.model.HandleResult;
import client.model.ProcessingResult;
import client.model.SystemContext;
import client.protobuf.EpfdMessageUtils;
import client.protobuf.MessageUtils;
import client.protobuf.P;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class EventuallyPerfectFailureDetector extends HandlerInterface {
    private static final Logger log = LogManager.getLogger(EventuallyPerfectFailureDetector.class);

    private Map<P.ProcessId, Boolean> alive;
    private Map<P.ProcessId, Boolean> suspected;
    private Long delay;
    private Long delta;
    private ScheduledExecutorService executor;
    private TimerTask timeoutTask;

    private PerfectLink perfectLink;

    public EventuallyPerfectFailureDetector(SystemContext context, String baseAbstractionId) {
        String abstractionId = String.format("%s.epfd", baseAbstractionId);
        super(context, abstractionId);

        this.alive = new HashMap<>();
        this.suspected = new HashMap<>();
        for (var processId : context.getProcesses()) {
            alive.put(processId, true);
            suspected.put(processId, false);
        }
        this.delay = 100L;
        this.delta = 100L;
        this.perfectLink = new PerfectLink(context, abstractionId);


        timeoutTask = new TimerTask() {
            public void run() {
                log.debug("Timeout task performed on {}", LocalTime.now());
                try {
                    context.getMessageQueue().put(EpfdMessageUtils.createEPFDTimeoutMessage(abstractionId, abstractionId, context.getSystemId()));
                } catch (InterruptedException e) {
                    log.error(e);
                }
            }
        };

        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(timeoutTask, delay, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public ProcessingResult handle (P.Message message, P.ProcessId sender) {
        ArrayBlockingQueue<P.Message> messageQueue = context.getMessageQueue();
        List<P.ProcessId> processes = context.getProcesses();
        String systemId = context.getSystemId();
        try {
            switch (message.getType()) {
                case EPFD_TIMEOUT -> {
                    log.debug("[{}] - EPFD_TIMEOUT | {}", abstractionId, message.getMessageUuid());
                    for (var pid : suspected.keySet()) {
                        if (alive.get(pid)) {
                            delay = delay + delta;
                        }
                    }
                    for (var pid : processes) {
                        if (!alive.get(pid) && !suspected.get(pid)) {
                            suspected.put(pid, true);
                            messageQueue.put(EpfdMessageUtils.createEPFDSuspectMessage(
                                    pid, abstractionId, MessageUtils.removeOneAbstractionLevel(abstractionId), systemId));
                        } else if (alive.get(pid) && suspected.get(pid)) {
                            suspected.put(pid, false);
                            messageQueue.put(EpfdMessageUtils.createEPFDRestoreMessage(
                                    pid, abstractionId, MessageUtils.removeOneAbstractionLevel(abstractionId), systemId));
                        }
                        messageQueue.put(MessageUtils.createPlSendMessage(EpfdMessageUtils.createEPFDHeartbeatRequestMessage(
                                abstractionId, abstractionId, systemId), abstractionId, abstractionId, pid));
                    }
                    processes.forEach(processId -> alive.put(processId, false));
                }

                case EPFD_INTERNAL_HEARTBEAT_REQUEST -> {
                    log.debug("[{}] - EPFD_INTERNAL_HEARTBEAT_REQUEST | {}", abstractionId, message.getMessageUuid());
                    messageQueue.put(MessageUtils.createPlSendMessage(
                            EpfdMessageUtils.createEPFDHeartbeatReplyMessage(abstractionId, abstractionId, systemId), abstractionId, abstractionId, sender));
                }

                case EPFD_INTERNAL_HEARTBEAT_REPLY -> {
                    log.debug("[{}] - EPFD_INTERNAL_HEARTBEAT_REPLY | {}", abstractionId, message.getMessageUuid());
                    alive.put(sender, true);
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
