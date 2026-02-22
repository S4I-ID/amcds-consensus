package client.handlers.registry;

import client.handlers.HandlerInterface;
import client.handlers.broadcast.BestEffortBroadcast;
import client.handlers.point_link.PerfectLink;
import client.model.HandleResult;
import client.model.ProcessingResult;
import client.model.SystemContext;
import client.protobuf.BroadcastMessageUtils;
import client.protobuf.MessageUtils;
import client.protobuf.NNAtomicRegistryMessageUtils;
import client.protobuf.P;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

public class NNAtomicRegistry extends HandlerInterface {
    private static final Logger log = LogManager.getLogger(NNAtomicRegistry.class);

    private Integer timestamp;
    private Integer writerRank;
    private Integer value;

    private Integer acks;
    private Integer writeValue;
    private Integer readValue;
    private Integer readId;

    private Boolean reading;
    private Map<P.ProcessId, P.NnarInternalValue> readList;

    private final BestEffortBroadcast broadcast;
    private final PerfectLink perfectLink;

    public NNAtomicRegistry(SystemContext context, String abstractionId) {
        super(context, abstractionId);
        this.broadcast = new BestEffortBroadcast(context, this.abstractionId);
        this.perfectLink = new PerfectLink(context, this.abstractionId);

        this.writerRank = 0;
        this.value = -1;
        this.timestamp = 0;
        this.acks = 0;
        this.readId = 0;
        this.writeValue = -1;
        this.readValue = -1;
        this.reading = false;
        this.readList = new HashMap<>(context.getProcesses().size() + 5);
    }

    @Override
    public ProcessingResult handle(P.Message message, P.ProcessId sender) {
        ArrayBlockingQueue<P.Message> messageQueue = context.getMessageQueue();
        String systemId = context.getSystemId();
        int processesCount = context.getProcesses().size();

        try {
            switch (message.getType()) {
                case NNAR_READ -> {
                    readId = readId + 1;
                    acks = 0;
                    readList = new HashMap<>(processesCount + 5);
                    reading = true;

                    log.info("[{}] - NNAR_READ - readId {} | {}", abstractionId, readId, message.getMessageUuid());
                    messageQueue.put(BroadcastMessageUtils.createBebBroadcastMessage(
                            NNAtomicRegistryMessageUtils.createNNARInternalReadMessage(readId, abstractionId, systemId), abstractionId, abstractionId));
                }

                case NNAR_INTERNAL_READ -> {
                    log.info("[{}] - NNAR_INTERNAL_READ - value {} - readId {} | {}", abstractionId, value, message.getNnarInternalRead().getReadId(),
                            message.getMessageUuid());
                    messageQueue.put(
                            MessageUtils.createPlSendMessage(
                                    NNAtomicRegistryMessageUtils.createNNARInternalValueMessage(
                                            NNAtomicRegistryMessageUtils.createNNARInternalValue(
                                                    value,
                                                    message.getNnarInternalRead().getReadId(),
                                                    writerRank,
                                                    timestamp),
                                            abstractionId, abstractionId, systemId
                                    ), abstractionId, abstractionId, sender
                            )
                    );
                }

                case NNAR_WRITE -> {
                    readId = readId + 1;
                    if (message.getNnarWrite().getValue().getDefined()) {
                        writeValue = message.getNnarWrite().getValue().getV();
                    } else {
                        writeValue = -1;
                    }
                    log.info("[{}] - NNAR_WRITE - writeValue {} | {}", abstractionId, writeValue, message.getMessageUuid());
                    acks = 0;
                    readList = new HashMap<>(processesCount + 4);
                    messageQueue.put(BroadcastMessageUtils.createBebBroadcastMessage(
                            NNAtomicRegistryMessageUtils.createNNARInternalReadMessage(readId, abstractionId, systemId), abstractionId, abstractionId));
                }

                case NNAR_INTERNAL_WRITE -> {
                    P.NnarInternalWrite recWrite = message.getNnarInternalWrite();

                    if (recWrite.getTimestamp() > timestamp || (recWrite.getTimestamp() == timestamp && recWrite.getWriterRank() > writerRank)) {
                        timestamp = recWrite.getTimestamp();
                        if (recWrite.getValue().getDefined())
                            value = recWrite.getValue().getV();
                        else
                            value = -1;
                        writerRank = recWrite.getWriterRank();
                    }
                    log.info("[{}] - NNAR_INTERNAL_WRITE - value {} - from {} | {}", abstractionId, value, MessageUtils.processIdToShortString(sender),
                            message.getMessageUuid());

                    log.debug("[{}] - Sending ACK to {}", abstractionId, MessageUtils.processIdToShortString(sender));
                    messageQueue.put(
                            MessageUtils.createPlSendMessage(
                                    NNAtomicRegistryMessageUtils.createNNARInternalAck(
                                            message.getNnarInternalWrite().getReadId(), abstractionId, abstractionId, systemId), abstractionId, abstractionId, sender)
                    );
                }

                case NNAR_INTERNAL_VALUE -> {
                    if (message.getNnarInternalValue().getReadId() == readId) {
                        P.NnarInternalValue receivedValue = message.getNnarInternalValue();
                        log.info("[{}] - NNAR_INTERNAL_VALUE - value {} from {} | {}", abstractionId, receivedValue.getValue().getV(),
                                MessageUtils.processIdToShortString(sender), message.getMessageUuid());
                        readList.put(sender, message.getNnarInternalValue());

                        if (readList.size() > processesCount / 2) {
                            String allProcesses = readList.keySet().stream()
                                    .map(MessageUtils::processIdToShortString)
                                    .collect(Collectors.joining(" > "));
                            log.info("[{}] - NNAR_INTERNAL_VALUE - readlist size passed half - {}", abstractionId, allProcesses);

                            P.NnarInternalValue latestInternalValue = null;
                            for (P.ProcessId key : readList.keySet()) {
                                if (latestInternalValue == null) {
                                    latestInternalValue = readList.get(key);
                                } else {
                                    if (largestInternalValueOf(readList.get(key), latestInternalValue)) {
                                        latestInternalValue = readList.get(key);
                                    }
                                }
                            }
                            P.Value latestValue = latestInternalValue.getValue();
                            readValue = MessageUtils.readValue(latestValue);
                            log.info("[{}] - NNAR_INTERNAL_VALUE - Latest readValue - {}", abstractionId, readValue);

                            readList = new HashMap<>(processesCount + 4);

                            if (reading) {
                                messageQueue.put(BroadcastMessageUtils.createBebBroadcastMessage(
                                        NNAtomicRegistryMessageUtils.createNNARInternalWriteMessage(
                                                latestValue,
                                                readId,
                                                latestInternalValue.getWriterRank(),
                                                latestInternalValue.getTimestamp(),
                                                abstractionId, systemId
                                        ), abstractionId, abstractionId
                                ));
                            } else {
                                messageQueue.put(BroadcastMessageUtils.createBebBroadcastMessage(
                                        NNAtomicRegistryMessageUtils.createNNARInternalWriteMessage(
                                                MessageUtils.createValue(writeValue),
                                                readId,
                                                context.getSelfProcessId().getRank(),
                                                latestInternalValue.getTimestamp() + 1,
                                                abstractionId, systemId
                                        ), abstractionId, abstractionId
                                ));
                            }
                        }
                    } else if (message.getNnarInternalValue().getReadId() > readId) {
                        return new ProcessingResult(HandleResult.PROCESS_LATER,
                                String.format("NNAR_INTERNAL_ACK | ReadId - %s | to %s", message.getNnarInternalValue().getReadId(), readId), message);
                    }
                }

                case NNAR_INTERNAL_ACK -> {
                    int receivedReadId = message.getNnarInternalAck().getReadId();
                    log.info("[{}] - NNAR_INTERNAL_ACK - for readId {} - from {} | {}", abstractionId, receivedReadId,
                            MessageUtils.processIdToShortString(sender), message.getMessageUuid());

                    if (receivedReadId == readId) {
                        acks = acks + 1;
                        if (acks > processesCount / 2) {
                            acks = 0;
                            if (reading) {
                                reading = false;
                                messageQueue.put(NNAtomicRegistryMessageUtils.createNNARReadReturnMessage(
                                        MessageUtils.createValue(readValue), abstractionId, systemId));
                            } else {
                                messageQueue.put(NNAtomicRegistryMessageUtils.createNNARWriteReturnMessage(abstractionId, systemId));
                            }
                        }
                    } else if (receivedReadId > readId) {
                        return new ProcessingResult(HandleResult.PROCESS_LATER, String.format("NNAR INTERNAL ACK WITH readId: %s to %s", receivedReadId, readId), message);
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

    private boolean largestInternalValueOf(P.NnarInternalValue primeVal, P.NnarInternalValue val) {
        if (primeVal.getTimestamp() > val.getTimestamp())
            return true;
        else if (primeVal.getTimestamp() == val.getTimestamp()) {
            if (primeVal.getWriterRank() > val.getWriterRank()) {
                return true;
            }
        }
        return false;
    }

    public PerfectLink getPerfectLink() {
        return perfectLink;
    }

    public BestEffortBroadcast getBroadcast() {
        return broadcast;
    }
}
