package client.handlers.point_link;

import client.handlers.HandlerInterface;
import client.model.HandleResult;
import client.model.ProcessingResult;
import client.model.SocketData;
import client.model.SystemContext;
import client.protobuf.MessageUtils;
import client.protobuf.P;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class PerfectLink extends HandlerInterface {
    private static final Logger log = LogManager.getLogger(PerfectLink.class);

    public PerfectLink(SystemContext context, String baseAbstractionId) {
        super(context, String.format("%s.pl", baseAbstractionId));
    }

    public Socket createSendSocket(String address, Integer port) throws IOException {
        Socket socket = new Socket();
        socket.setReuseAddress(true);
        socket.setKeepAlive(false);
        socket.connect(new InetSocketAddress(address, port));
        return socket;
    }

    public ServerSocket createListenSocket() throws IOException {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);

        SocketData self = context.getSocketPair().getSelf();
        log.debug("Binding listen socket to {}", self);
        serverSocket.bind(new InetSocketAddress(self.getIpAddress(), self.getPort()));
        return serverSocket;
    }

    @Override
    public ProcessingResult handle(P.Message message, P.ProcessId sender) {
        try {
            switch (message.getType()) {
                case NETWORK_MESSAGE -> {
                    log.debug("[{}] - NETWORK MESSAGE | {}", abstractionId, message.getMessageUuid());
                    context.getMessageQueue().put(MessageUtils.unmarshalMessage(message));
                }
                case PL_SEND -> {
                    log.info("[{}] - PL_SEND - {} to {} | {}", abstractionId, message.getPlSend().getMessage().getType(),
                            MessageUtils.processIdToShortString(message.getPlSend().getDestination()), message.getMessageUuid());

                    P.Message networkMessage = MessageUtils.marshalMessage(message, context.getSocketPair().getSelf());
                    String address = message.getPlSend().getDestination().getHost();
                    Integer port = message.getPlSend().getDestination().getPort();

                    try (Socket socket = createSendSocket(address, port)) {
                        socket.getOutputStream().write(getByteArrayFromInt(networkMessage.getSerializedSize()));
                        networkMessage.writeTo(socket.getOutputStream());
                    }
                }
                default -> {
                    return new ProcessingResult(HandleResult.UNABLE_TO_PROCESS, String.format("Message type not supported in %s - %s", abstractionId, message.getType()), message);
                }
            }
            return new ProcessingResult(HandleResult.MESSAGE_OK);
        } catch (Exception e) {
            return new ProcessingResult(HandleResult.ERROR, e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()), message);
        }
    }

    private byte[] getByteArrayFromInt(Integer integer) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(integer);
        return buffer.array();
    }


}
