package client.protobuf;

public class EpochChangeMessageUtils {
    public static P.Message createECStartEpochMessage(P.ProcessId leader, Integer newTimestamp, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EcStartEpoch ecStartEpoch = P.EcStartEpoch.newBuilder()
                .setNewLeader(leader)
                .setNewTimestamp(newTimestamp)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EC_START_EPOCH)
                .setEcStartEpoch(ecStartEpoch)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }


    public static P.Message createECInternalNAckMessage(String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EcInternalNack ecInternalNack = P.EcInternalNack.newBuilder().build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EC_INTERNAL_NACK)
                .setEcInternalNack(ecInternalNack)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createECInternalNewEpoch(Integer timestamp, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EcInternalNewEpoch ecInternalNewEpoch = P.EcInternalNewEpoch.newBuilder()
                .setTimestamp(timestamp).build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EC_INTERNAL_NEW_EPOCH)
                .setEcInternalNewEpoch(ecInternalNewEpoch)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }
}
