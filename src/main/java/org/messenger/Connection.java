package org.messenger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;

public class Connection extends Thread{


    private Boolean isRunning = true;
    private final Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    Connection(Socket socket){
        this.socket = socket;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            System.out.println("Unable to get input stream from socket. Client ip:" + socket.getInetAddress().getHostAddress());
        }
        this.start();
    }

    @Override
    public void run() {
        ConnectionManager.handleConnection(this, isRunning);
    }

    public void disconnect() {
        isRunning = false;
    }

    public Socket socket(){
        return socket;
    }

    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }
    public OutputStream getOutputStream() throws IOException{
        return socket.getOutputStream();
    }
    public BufferedReader getBufferedReader() throws IOException{
       return bufferedReader;
    }
    public BufferedWriter getBufferedWriter() throws IOException {
        return bufferedWriter;
    }
    public JSONObject getRequest() throws IOException, ParseException {
        String jsonDataString;
        JSONParser parser = new JSONParser();
        JSONObject dataPacket = null;
        if((jsonDataString = bufferedReader.readLine()) != null) {
            dataPacket = (JSONObject) parser.parse(jsonDataString);
        }
        return dataPacket;
    }
    public void send(JSONObject dataPacket) throws IOException {
        bufferedWriter.write(dataPacket.toJSONString() + "\n");
        bufferedWriter.flush();
    }

    @Override
    public int hashCode() {
        return socket.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Connection)
            return ((Connection) obj).socket.equals(this.socket);
        return false;
    }
}
