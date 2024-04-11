package org.messenger;

import org.messenger.data.entities.User;

import java.io.*;
import java.net.Socket;

public class Connection extends Thread{


    private Boolean isRunning = true;
    private final Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private ObjectInputStream objectInputStream = null;
    private ObjectOutputStream objectOutputStream = null;
    protected BufferedInputStream inputStream;
    protected BufferedOutputStream outputStream;
    private User user;
    //private DataBaseConnection dataBaseConnection = new DataBaseConnection();

    Connection(Socket socket){
        this.socket = socket;
        try {
            inputStream = new BufferedInputStream(socket.getInputStream());
            outputStream = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    public Object getRequest() throws IOException, ClassNotFoundException {
        if(objectInputStream == null) objectInputStream = new ObjectInputStream(inputStream);
        Object request = null;
        request = objectInputStream.readObject();
        return request;
    }

    public void send(Object object) throws IOException {
        if(objectOutputStream == null) objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
    }

    public void auth(User user){
        this.user = user;
    }
    public boolean isAuthorized(){
        return user != null;
    }
    public String getIp(){
        return socket.getInetAddress().getHostAddress();
    }
    public int getPort(){return socket.getPort();}

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
    public User getUser(){return user;}


}
