package org.messenger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;


public class ConnectionManager {

    public interface RequestHandler {public void handle(Connection connection);}

    static HashMap<String, RequestHandler> handlersMap = new HashMap<>();

    static{


        handlersMap.put("Authorization", new RequestHandler() {
            @Override
            public void handle(Connection connection) {
                try {

                    String userName;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.socket().getInputStream()));
                    userName = reader.readLine();
                    connection.user = new User(userName);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        handlersMap.put("GetThread", new RequestHandler() {
            @Override
            public void handle(Connection connection) {
            }
        });
        handlersMap.put("SendMessage", new RequestHandler() {
            @Override
            public void handle(Connection connection) {
                String line;
                char[] buffer = new char[1000];
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.socket().getInputStream()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private ConnectionManager(){}; //Unable to access


    static void connect(Socket socket){
        new Connection(socket);
    }

    static void handleConnection(Connection connection, Boolean isRunning){
        BufferedReader reader = null;
        JSONParser parser = new JSONParser();
        JSONObject dataPacket;
        try {
            reader = new BufferedReader(new InputStreamReader(connection.socket().getInputStream()));
        } catch (IOException e) {
            isRunning = false;
        }

        while (isRunning){
            String requestType;
            try {
                if((requestType = reader.readLine()) != null){
                    RequestHandler handler = handlersMap.get(requestType);
                    if(handler != null)
                        handler.handle(connection);
                    else
                        System.out.println("Unable to proceed "+ requestType);
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
