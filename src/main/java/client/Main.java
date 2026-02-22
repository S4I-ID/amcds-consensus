package client;

import client.model.SocketData;
import client.model.SocketPairData;

public class Main {
    /**
     * @param args:
     *             0 - hub IP address;
     *             1 - hub port;
     *             2 - this process' address;
     *             3 - this process' port;
     *             4 - this process' name;
     *             5 - this process' index
     */
    static void main(String[] args) {
        String hubAddress = args[0];                        // hub IP address
        Integer hubPort = Integer.valueOf(args[1]);         // hub port
        String procAddress = args[2];                       // this process' address
        Integer procPort = Integer.valueOf(args[3]);        // this process' port
        String refName = args[4];                           // this process' name
        Integer index = Integer.valueOf(args[5]);           // this process' index

        SocketData hubSocket = new SocketData(hubAddress, hubPort);
        SocketData selfSocket = new SocketData(procAddress, procPort);
        SocketPairData socketPair = new SocketPairData(hubSocket, selfSocket);

        Node node = new Node(socketPair, refName, index);
        node.start();
    }
}