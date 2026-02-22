package client.threads;

import client.handlers.point_link.PerfectLink;
import client.protobuf.P;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;

public class NodeNetworkThread extends Thread {
    private static final Logger log = LogManager.getLogger(NodeNetworkThread.class);

    private final ArrayBlockingQueue<P.Message> messageQueue;
    private final PerfectLink perfectLink;

    private ServerSocket serverSocket;

    public NodeNetworkThread(ArrayBlockingQueue<P.Message> messageQueue, PerfectLink perfectLink) {
        this.messageQueue = messageQueue;
        this.perfectLink = perfectLink;
    }

    @Override
    public void run() {
        this.setName("NodeNetwork");
        log.debug("Starting network thread for {}...", perfectLink);

        while (true) {
            try {
                serverSocket = perfectLink.createListenSocket();
                break;
            } catch (Exception e) {
                log.error(e);
                try {
                    Thread.sleep(Duration.ofSeconds(3));
                } catch (InterruptedException ignored) {}
            }
        }

        log.info("Node listening socket bound, waiting for messages...");

        while (true) {
            try (Socket socket = serverSocket.accept()) {
                socket.setReuseAddress(true);

                int messageSize = getIntFromByteArray(socket.getInputStream().readNBytes(4));
                P.Message receivedMessage = P.Message.parseFrom(socket.getInputStream().readNBytes(messageSize));

                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();

                log.debug("Received message - {}", receivedMessage);
                messageQueue.add(receivedMessage);
            } catch (Exception exception) {
                log.error(exception);
            }
        }
    }

    private Integer getIntFromByteArray(byte[] intArray) {
        ByteBuffer buffer = ByteBuffer.wrap(intArray);
        return buffer.getInt();
    }

    public PerfectLink getPerfectLink() {
        return perfectLink;
    }

    /**
     * Closes active server socket in thread
     * <p>Use before joining thread</p>
     */
    public void closeServerSocket() {
        try {
            serverSocket.close();
        } catch (IOException ignored) {}
    }
}