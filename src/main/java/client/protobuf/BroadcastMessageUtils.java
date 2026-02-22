package client.protobuf;

public class BroadcastMessageUtils {
    public static P.Message createBebBroadcastMessage(P.Message message, String fromAbstractionId, String toAbstractionId) {
        P.BebBroadcast bebBroadcast = P.BebBroadcast.newBuilder()
                .setMessage(message)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.BEB_BROADCAST)
                .setBebBroadcast(bebBroadcast)
                .setMessageUuid(message.getMessageUuid())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId + ".beb")
                .setSystemId(message.getSystemId())
                .build();
    }
}
