package org.messenger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Connection extends Thread{

    private Boolean isRunning = true;
    private final Socket socket;

    Connection(Socket socket){
        this.socket = socket; this.start();
    }

    @Override
    public void run() {
        ConnectionManager.handleConnection(socket, isRunning);
    }

    public void disconnect() {
        isRunning = false;
    }
}
