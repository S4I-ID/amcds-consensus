package client.protobuf;

public class EpochConsensusMessageUtils {
    public static P.Message createEPInternalReadMessage(String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpInternalRead epInternalRead = P.EpInternalRead.newBuilder().build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EP_INTERNAL_READ)
                .setEpInternalRead(epInternalRead)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createEPInternalStateMessage(P.EpInternalState state, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpInternalState epInternalState = P.EpInternalState.newBuilder()
                .setValue(state.getValue())
                .setValueTimestamp(state.getValueTimestamp())
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EP_INTERNAL_STATE)
                .setEpInternalState(epInternalState)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createEPInternalWriteMessage(P.Value value, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpInternalWrite epInternalWrite = P.EpInternalWrite.newBuilder()
                .setValue(value).build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EP_INTERNAL_WRITE)
                .setEpInternalWrite(epInternalWrite)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.EpInternalState createEPInternalState(Integer value, Integer timestamp) {
        return P.EpInternalState.newBuilder()
                .setValue(MessageUtils.createValue(value))
                .setValueTimestamp(timestamp)
                .build();
    }

    public static P.Message createEPInternalDecidedMessage(P.Value value, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpInternalDecided epInternalDecided = P.EpInternalDecided.newBuilder()
                .setValue(value)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EP_INTERNAL_DECIDED)
                .setEpInternalDecided(epInternalDecided)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createEPInternalAcceptMessage(String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpInternalAccept epInternalAccept = P.EpInternalAccept.newBuilder().build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EP_INTERNAL_ACCEPT)
                .setEpInternalAccept(epInternalAccept)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createEPDecideMessage(P.Value value, Integer epochTimestamp, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpDecide epDecide = P.EpDecide.newBuilder()
                .setValue(value)
                .setEts(epochTimestamp)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EP_DECIDE)
                .setEpDecide(epDecide)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createEPAbortedMessage(Integer epochTimestamp, P.EpInternalState state, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpAborted epAborted = P.EpAborted.newBuilder()
                .setEts(epochTimestamp)
                .setValue(state.getValue())
                .setValueTimestamp(state.getValueTimestamp())
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EP_ABORTED)
                .setEpAborted(epAborted)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }
}
