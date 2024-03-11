import org.json.simple.JSONObject;

import java.io.*;
import java.net.Socket;

public class SocketTester {
   /* public static void main(String[] args) {
      /*  Socket socket;
        try {
            socket = new Socket("127.0.0.1",9000);
            socket.getOutputStream().write("Authorization\n".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}*/
   public static byte[] convertToByteArray(Object object) throws IOException {
       try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
           objectOutputStream.writeObject(object);
           return byteArrayOutputStream.toByteArray();
       }
   }
    public static Object convertToObject(byte[] byteArray) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return objectInputStream.readObject();
        }
    }

   public static void main(String[] args) {
       try {

           Socket socket = new Socket("127.0.0.1",9000);
           JSONObject object = new JSONObject();
           object.put("requestDescription","Authorization");
           SocketTesterSecureWrapper secSocket = new SocketTesterSecureWrapper(socket);
           System.out.println("connection established");
           secSocket.sendData(object);
           System.out.println("sended");
           object = (JSONObject) secSocket.getData();
           System.out.println("data get");
           System.out.println(object);
       } catch (IOException e) {
           System.out.println(e.getMessage());
           throw new RuntimeException(e);
       } catch (ClassNotFoundException e) {
           throw new RuntimeException(e);
       }

   }
}
