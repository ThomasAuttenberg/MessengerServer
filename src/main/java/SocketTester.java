import org.json.simple.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

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
           String host = "localhost";
           int port = 9000;

           KeyStore keyStore = KeyStore.getInstance("JKS");
           char[] password = "qwerty".toCharArray();
           String keyStorePathString = "src/main/resources/keystore.jks";

           Path keyStorePath = Paths.get(keyStorePathString).toAbsolutePath();

           URI publicKeyFileURL = keyStorePath.toUri();
           keyStore.load(new FileInputStream(keyStorePath.toFile()), password);

           TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
           trustManagerFactory.init(keyStore);

           SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
           sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

           SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
           SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(host, port);

           SocketTesterConnection connection = new SocketTesterConnection(socket);

           JSONObject jsonObject = new JSONObject();
           //connection.send("meow");
           //jsonObject.put("authType","authToken");
           jsonObject.put("authType","password");
           jsonObject.put("requestDescription","Authorization");
           //jsonObject.put("token","219d77fb0c8e8ded76dd658d330710d097531ada45de3e0291cdd0f93f9b111e");
           jsonObject.put("username","admin");
           jsonObject.put("password","qwerty");

           //jsonObject.put("requestDescription","SendMessage");
           //Long a = 2;
           //jsonObject.put("threadId",2L);

           connection.send(jsonObject);
           jsonObject = (JSONObject) connection.getRequest();
           System.out.println(jsonObject);
           jsonObject.clear();
           jsonObject.put("requestDescription","SendMessage");
           jsonObject.put("threadId",4L);
           jsonObject.put("content","Content by socket tester!");
           //jsonObject.put("threadId",1L);
           connection.send(jsonObject);
           jsonObject = (JSONObject) connection.getRequest();
           System.out.println(jsonObject);
           socket.close();

       } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException |
                KeyManagementException | ClassNotFoundException e) {
           throw new RuntimeException(e);
       }
   }
}
