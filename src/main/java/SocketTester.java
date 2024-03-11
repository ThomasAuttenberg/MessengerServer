import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

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
   public static void main(String[] args) {
      /* try {

           Socket socket = new Socket("127.0.0.1",9000);
           JSONObject object = new JSONObject();
           object.put("requestDescription","Authorization");
           BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
           writer.write(object.toJSONString()+"\n");
           writer.flush();
           System.out.println(object);
           BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
           JSONParser parser = new JSONParser();
           String jsonString = reader.readLine();
           System.out.println(jsonString);
       } catch (IOException e) {
           System.out.println(e.getMessage());
           throw new RuntimeException(e);
       }*/
       KeyPairGenerator keyGen;
       try {
           keyGen = KeyPairGenerator.getInstance("RSA");
       } catch (NoSuchAlgorithmException e) {
           throw new RuntimeException(e);
       }

       KeyPair pair = keyGen.generateKeyPair();
       try{
           File file = new File("src/main/resources/publicKey");
           ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file+"1"));
           objectOutputStream.write(pair.getPublic().getEncoded());
           System.out.println(pair.getPublic());
       } catch (IOException e) {
           throw new RuntimeException(e);
       }

   }
}
