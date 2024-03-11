package org.messenger;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;


public class ConnectionManager {

    public interface RequestHandler {public void handle(Connection connection, JSONObject dataPacket);}

    static HashMap<String, RequestHandler> handlersMap = new HashMap<>();

    static{


        handlersMap.put("Authorization", (connection, dataPacket) -> {
            try {
                JSONObject reply = new JSONObject();
                reply.put("status","OK");
                reply.put("desc","Authorization!");
                connection.send(reply);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        handlersMap.put("GetThread", (connection, dataPacket) -> {

        });
        handlersMap.put("SendMessage", (connection, dataPacket) -> {

        });
    }

    private ConnectionManager(){}; //Unable to access


    static void connect(Socket socket){
        new SecuredConnection(socket).start();
    }

    static void handleConnection(Connection connection, Boolean isRunning){

        while (isRunning){
            //try {
                JSONOverConnection jsonOverConnection = new JSONOverConnection(connection);
                System.out.println("getting request [handler]");
                JSONObject dataPacket = jsonOverConnection.getRequest();
                String requestDescription = (String) dataPacket.get("requestDescription");
                handlersMap.get(requestDescription).handle(connection, dataPacket);
          /*  } catch (IOException e) {
                System.out.println("["+connection.socket().getInetAddress().getHostAddress()+":"+connection.socket().getPort()+"] Client has disconnected");
                break;
            } catch (ParseException e) {
                System.out.println("["+connection.socket().getInetAddress().getHostAddress()+":"+connection.socket().getPort()+"] Incorrect packet format");
            }*/
        }
        System.out.println("Connection released");
    }

}
