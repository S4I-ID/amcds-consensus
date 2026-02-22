package client.protobuf;

public class NNAtomicRegistryMessageUtils {
    public static P.Message createNNARReadReturnMessage(P.Value value, String abstractionId, String systemId) {
        P.NnarReadReturn nnarReadReturn = P.NnarReadReturn.newBuilder()
                .setValue(value)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.NNAR_READ_RETURN)
                .setNnarReadReturn(nnarReadReturn)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(abstractionId)
                .setToAbstractionId("app")
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createNNARWriteReturnMessage(String abstractionId, String systemId) {
        P.NnarWriteReturn nnarWriteReturn = P.NnarWriteReturn.newBuilder().build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.NNAR_WRITE_RETURN)
                .setNnarWriteReturn(nnarWriteReturn)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(abstractionId)
                .setToAbstractionId("app")
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createNNARInternalReadMessage(Integer readId, String abstractionId, String systemId) {
        P.NnarInternalRead internalRead = P.NnarInternalRead.newBuilder()
                .setReadId(readId)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.NNAR_INTERNAL_READ)
                .setNnarInternalRead(internalRead)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(abstractionId)
                .setToAbstractionId(abstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createNNARInternalWriteMessage(P.Value newValue, Integer readId, Integer newWriterRank,
                                                           Integer newTimestamp, String abstractionId, String systemId) {
        P.NnarInternalWrite nnarInternalWrite = P.NnarInternalWrite.newBuilder()
                .setValue(newValue)
                .setReadId(readId)
                .setWriterRank(newWriterRank)
                .setTimestamp(newTimestamp)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.NNAR_INTERNAL_WRITE)
                .setNnarInternalWrite(nnarInternalWrite)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(abstractionId)
                .setToAbstractionId(abstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.Message createNNARInternalAck(Integer readId, String fromAbstractionId, String toAbstractionId, String systemId) {
        P.NnarInternalAck internalAck = P.NnarInternalAck.newBuilder()
                .setReadId(readId)
                .build();
        return P.Message.newBuilder()
                .setType(P.Message.Type.NNAR_INTERNAL_ACK)
                .setNnarInternalAck(internalAck)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }

    public static P.NnarInternalValue createNNARInternalValue(Integer value, Integer readId, Integer writerRank, Integer timestamp) {
        return P.NnarInternalValue.newBuilder()
                .setValue(MessageUtils.createValue(value))
                .setReadId(readId)
                .setWriterRank(writerRank)
                .setTimestamp(timestamp)
                .build();
    }

    public static P.Message createNNARInternalValueMessage(P.NnarInternalValue nnarInternalValue,
                                                           String fromAbstractionId, String toAbstractionId, String systemId) {
        return P.Message.newBuilder()
                .setType(P.Message.Type.NNAR_INTERNAL_VALUE)
                .setNnarInternalValue(nnarInternalValue)
                .setMessageUuid(MessageUtils.generateId())
                .setFromAbstractionId(fromAbstractionId)
                .setToAbstractionId(toAbstractionId)
                .setSystemId(systemId)
                .build();
    }
}
