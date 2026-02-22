package client.protobuf;

public class UniformConsensusMessageUtils {

    public static P.Message createEPAbortMessage(String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpAbort epAbort = P.EpAbort.newBuilder().build();
        P.Message message = P.Message.newBuilder()
                .setType(P.Message.Type.EP_ABORT)
                .setEpAbort(epAbort)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
        return message;
    }

    public static P.EpInternalState createEPInternalState(P.Value value, Integer timestamp) {
        return P.EpInternalState.newBuilder()
                .setValue(value)
                .setValueTimestamp(timestamp)
                .build();
    }

    public static P.Message createUCDecideMessage(P.Value value, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.UcDecide ucDecide = P.UcDecide.newBuilder()
                .setValue(value)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.UC_DECIDE)
                .setUcDecide(ucDecide)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createEPProposeMessage(P.Value value, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpPropose epPropose = P.EpPropose.newBuilder()
                .setValue(value).build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EP_PROPOSE)
                .setEpPropose(epPropose)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }
}
