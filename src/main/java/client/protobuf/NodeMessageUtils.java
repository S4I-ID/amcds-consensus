package client.protobuf;

import client.model.SocketData;

public class NodeMessageUtils {
    public static P.Message createRegistrationMessage ( SocketData hubSocket, String owner, Integer index) {
        P.ProcRegistration procRegistration = P.ProcRegistration.newBuilder()
                .setOwner(owner)
                .setIndex(index)
                .build();
        P.Message pMessage = P.Message.newBuilder()
                .setType(P.Message.Type.PROC_REGISTRATION)
                .setProcRegistration(procRegistration)
                .build();

        return MessageUtils.createPlSendMessage(pMessage, "", "hub", MessageUtils.createHubProcessId(hubSocket));
    }
}
