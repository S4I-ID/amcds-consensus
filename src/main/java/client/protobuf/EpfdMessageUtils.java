package client.protobuf;

public class EpfdMessageUtils {
    public static P.Message createEPFDTimeoutMessage(String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpfdTimeout epfdTimeout = P.EpfdTimeout.newBuilder().build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EPFD_TIMEOUT)
                .setEpfdTimeout(epfdTimeout)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createEPFDHeartbeatReplyMessage(String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpfdInternalHeartbeatReply epfdInternalHeartbeatReply = P.EpfdInternalHeartbeatReply.newBuilder()
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EPFD_INTERNAL_HEARTBEAT_REPLY)
                .setEpfdInternalHeartbeatReply(epfdInternalHeartbeatReply)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createEPFDSuspectMessage(P.ProcessId processId, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpfdSuspect epfdSuspect = P.EpfdSuspect.newBuilder()
                .setProcess(processId)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EPFD_SUSPECT)
                .setEpfdSuspect(epfdSuspect)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createEPFDRestoreMessage(P.ProcessId pid, String abstractionId, String toParentAbstractionId, String systemId) {
        P.EpfdRestore epfdRestore = P.EpfdRestore.newBuilder()
                .setProcess(pid)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EPFD_RESTORE)
                .setEpfdRestore(epfdRestore)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(abstractionId)
                .setToAbstractionId(toParentAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createEPFDHeartbeatRequestMessage(String fromAbstractionId, String toAbstractionId, String systemId) {
        P.EpfdInternalHeartbeatRequest heartbeatRequest = P.EpfdInternalHeartbeatRequest.newBuilder().build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.EPFD_INTERNAL_HEARTBEAT_REQUEST)
                .setEpfdInternalHeartbeatRequest(heartbeatRequest)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }
}
