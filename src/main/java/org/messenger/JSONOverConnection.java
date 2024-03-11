package org.messenger;

import org.json.simple.JSONObject;

import java.io.IOException;

public class JSONOverConnection {
    private Connection connection;
    JSONOverConnection(Connection connection){
        this.connection = connection;
    }
    public JSONObject getRequest(){
        try {
            System.out.println("JSONOver getting request");
            JSONObject object = (JSONObject) connection.getRequest();
            System.out.println(object);
            System.out.println("JSONOver got request");
            return object;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    };
    public void send(JSONObject object){
        try {
            connection.send(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
