package client.protobuf;

public class EldMessageUtils {
    public static P.Message createELDTrustMessage(P.ProcessId processId, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EldTrust eldTrust = P.EldTrust.newBuilder()
                .setProcess(processId)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.ELD_TRUST)
                .setEldTrust(eldTrust)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }
}
