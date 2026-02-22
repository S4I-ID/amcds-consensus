package client.protobuf;

public class AppBaseMessageUtils {
    public static P.Message createAppValueMessage(P.Message originalMessage, String systemId) {
        P.Message appValueWrapper;
        if (originalMessage.getType() == P.Message.Type.APP_BROADCAST) {
            P.AppValue appValue = P.AppValue.newBuilder()
                    .setValue(originalMessage.getAppBroadcast().getValue())
                    .build();
            appValueWrapper = P.Message.newBuilder()
                    .setType(P.Message.Type.APP_VALUE)
                    .setAppValue(appValue)
                    .setMessageUuid(MessageUtils.generateId())
                    .setFromAbstractionId("app")
                    .setToAbstractionId("app")
                    .setSystemId(systemId)
                    .build();
        } else if (originalMessage.getType() == P.Message.Type.APP_VALUE) {
            appValueWrapper = originalMessage;
        } else {
            appValueWrapper = null;
        }

        return appValueWrapper;
    }

    public static P.Message createNNARWriteMessage(P.Message message) {
        String register = message.getAppWrite().getRegister();
        P.Value value = message.getAppWrite().getValue();
        P.NnarWrite nnarWrite = P.NnarWrite.newBuilder()
                .setValue(value)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.NNAR_WRITE)
                .setNnarWrite(nnarWrite)
                .setFromAbstractionId("app")
                .setToAbstractionId(String.format("app.nnar[%s]", register))
                .setMessageUuid(MessageUtils.generateId())
                .setSystemId(message.getSystemId())
                .build();
    }

    public static P.Message createNNARReadMessage(P.Message message) {
        String register = message.getAppRead().getRegister();
        P.NnarRead nnarRead = P.NnarRead.newBuilder()
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.NNAR_READ)
                .setNnarRead(nnarRead)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId("app")
                .setToAbstractionId(String.format("app.nnar[%s]", register))
                .setSystemId(message.getSystemId())
                .build();
    }

    public static P.Message createAppReadReturnMessage(P.Message message) {
        P.AppReadReturn appReadReturn = P.AppReadReturn.newBuilder()
                .setValue(message.getNnarReadReturn().getValue())
                .setRegister(MessageUtils.getRegisterFromAbstraction(message.getFromAbstractionId()))
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.APP_READ_RETURN)
                .setAppReadReturn(appReadReturn)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId("app")
                .setToAbstractionId("hub")
                .setSystemId(message.getSystemId())
                .build();
    }

    public static P.Message createAppWriteReturnMessage(P.Message message) {
        P.AppWriteReturn appWriteReturn = P.AppWriteReturn.newBuilder()
                .setRegister(MessageUtils.getRegisterFromAbstraction(message.getFromAbstractionId()))
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.APP_WRITE_RETURN)
                .setAppWriteReturn(appWriteReturn)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId("app")
                .setToAbstractionId("hub")
                .setSystemId(message.getSystemId())
                .build();
    }

    public static P.Message createUCProposeMessage(P.Value value, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.UcPropose ucPropose = P.UcPropose.newBuilder()
                .setValue(value).build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.UC_PROPOSE)
                .setUcPropose(ucPropose)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createAppDecideMessage(P.Value value, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.AppDecide appDecide = P.AppDecide.newBuilder()
                .setValue(value).build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.APP_DECIDE)
                .setAppDecide(appDecide)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }
}
