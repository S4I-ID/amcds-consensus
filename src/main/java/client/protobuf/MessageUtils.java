package client.protobuf;

import client.model.SocketData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class MessageUtils {
    private static final Logger log = LogManager.getLogger(MessageUtils.class);

    public static String generateId() {
        return String.valueOf(UUID.randomUUID());
    }

    public static String getRegisterFromAbstraction(String abstractionId) {
        return abstractionId.split("[\\[\\]]")[1];
    }

    public static String processIdToString(P.ProcessId processId) {
        if (processId != null) {
            return String.format("%s:%s %s-%s RANK %s", processId.getHost(), processId.getPort(), processId.getOwner(), processId.getIndex(), processId.getRank());
        } else {
            return null;
        }
    }

    public static String processIdToShortString(P.ProcessId processId) {
        if (processId != null) {
            return String.format("%s-%s (%s:%s)", processId.getOwner(), processId.getIndex(), processId.getHost(), processId.getPort());
        } else {
            return null;
        }
    }

    public static P.Value createValue(Integer writeValue) {
        P.Value value;
        if (writeValue!=-1) {
            value = P.Value.newBuilder()
                    .setV(writeValue)
                    .setDefined(true)
                    .build();
        } else {
            value = P.Value.newBuilder()
                    .setDefined(false)
                    .setV(-1)
                    .build();
        }
        return value;
    }

    public static Integer readValue(P.Value value) {
        if (value.getDefined()) {
            return value.getV();
        }
        else {
            return -1;
        }
    }

    public static String removeOneAbstractionLevel(String abstractionId) {
        return abstractionId.substring(0, abstractionId.lastIndexOf("."));
    }


    public static P.Message getInternalMessage(P.Message message) {
        return switch (message.getType()) {
            case PL_DELIVER -> message.getPlDeliver().getMessage();
            case BEB_DELIVER -> message.getBebDeliver().getMessage();
            default -> message;
        };
    }

    public static P.ProcessId createHubProcessId(SocketData hubSocket) {
        return P.ProcessId.newBuilder()
                .setOwner("hub")
                .setHost(hubSocket.getIpAddress())
                .setPort(hubSocket.getPort())
                .build();
    }

    public static P.Message createPlSendMessage(P.Message message, String fromAbstractionId, String toAbstractionId, P.ProcessId destination) {
        P.PlSend plSend = P.PlSend.newBuilder()
                .setMessage(message)
                .setDestination(destination)
                .build();
        log.debug("PlSendMessage TO - {}:{},", destination.getHost(), destination.getPort());
        return P.Message.newBuilder()
                .setType(P.Message.Type.PL_SEND)
                .setPlSend(plSend)
                .setMessageUuid(message.getMessageUuid())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId+".pl")
                .setSystemId(message.getSystemId())
                .build();
    }

    // MessageA(PlSend(MessageB)) -> MessageC(NetworkMessage(MessageB))
    public static P.Message marshalMessage(P.Message plSendMessage, SocketData self) {
        P.NetworkMessage networkMessage = P.NetworkMessage.newBuilder()
                .setSenderHost(self.getIpAddress())
                .setSenderListeningPort(self.getPort())
                .setMessage(plSendMessage.getPlSend().getMessage())
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.NETWORK_MESSAGE)
                .setSystemId(plSendMessage.getSystemId())
                .setToAbstractionId(plSendMessage.getToAbstractionId())
                .setNetworkMessage(networkMessage)
                .setMessageUuid(plSendMessage.getMessageUuid())
                .build();
    }

    public static P.Message unmarshalMessage(P.Message networkMessage) {
        P.Message interiorMessage = networkMessage.getNetworkMessage().getMessage();
        P.PlDeliver plDeliverMessage = P.PlDeliver.newBuilder()
                .setMessage(interiorMessage)
                .setSender(P.ProcessId.newBuilder()
                        .setHost(networkMessage.getNetworkMessage().getSenderHost())
                        .setPort(networkMessage.getNetworkMessage().getSenderListeningPort())
                        .build())
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.PL_DELIVER)
                .setPlDeliver(plDeliverMessage)
                .setMessageUuid(networkMessage.getMessageUuid())
                .setToAbstractionId(networkMessage.getToAbstractionId())
                .setSystemId(networkMessage.getSystemId())
                .build();
    }
}
