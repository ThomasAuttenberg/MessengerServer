package org.messenger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    private static boolean isRunning = true;
    private static int serverPort = 9000;
    private static ServerSocket serverSocket;

    public static void main(String[] args) {

        try {
            serverSocket = new ServerSocket(serverPort);
            while(isRunning) {
                Socket connection = serverSocket.accept();
                ConnectionManager.connect(connection);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}