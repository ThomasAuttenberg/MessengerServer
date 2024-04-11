import java.io.*;
import java.net.Socket;

public class SocketTesterConnection{


    private Boolean isRunning = true;
    private final Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private ObjectInputStream objectInputStream = null;
    private ObjectOutputStream objectOutputStream = null;
    protected BufferedInputStream inputStream;
    protected BufferedOutputStream outputStream;

    SocketTesterConnection(Socket socket){
        this.socket = socket;
        try {
            inputStream = new BufferedInputStream(socket.getInputStream());
            outputStream = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

}
