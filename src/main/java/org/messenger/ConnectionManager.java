package org.messenger;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ConnectionManager {

    private ConnectionManager(){}; //Construction unable

    static void connect(Socket socket){
        new Connection(socket);
    }
    static void handleConnection(Socket socket, Boolean isRunning){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            isRunning = false;
        }

        while (isRunning){
            String line;
            try {
                if((line = reader.readLine()) != null){
                    System.out.println(line);
                }else{
                    // Unexpected stream closing
                    isRunning = false;
                }
            } catch (IOException e) {
                // Socket closing
                isRunning = false;
            }

        }
        System.out.println("Connection released");
    }

}
