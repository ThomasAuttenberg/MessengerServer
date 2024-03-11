import javax.crypto.*;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SocketTesterSecureWrapper {
    ObjectOutputStream objectOutputStream;
    ObjectInputStream objectInputStream;
    private final Key secretKey;
    private final Cipher syncKeyEncryptor;
    private final Cipher syncKeyDecryptor;
    SocketTesterSecureWrapper(Socket socket ){
        try {
            objectInputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            Key serverPublicKey = (Key) objectInputStream.readObject();
            Cipher viaPublicKeyEncryptor = Cipher.getInstance("RSA");
            viaPublicKeyEncryptor.init(Cipher.ENCRYPT_MODE,serverPublicKey);
            secretKey = KeyGenerator.getInstance("AES").generateKey();
            syncKeyEncryptor = Cipher.getInstance("AES");
            syncKeyEncryptor.init(Cipher.ENCRYPT_MODE,secretKey);
            syncKeyDecryptor = Cipher.getInstance("AES");
            syncKeyDecryptor.init(Cipher.DECRYPT_MODE,secretKey);
            byte[] bytes = viaPublicKeyEncryptor.doFinal(secretKey.getEncoded());
            System.out.println(Arrays.toString(secretKey.getEncoded()));
            //System.out.println(Arrays.toString(bytes));
            objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            objectOutputStream.writeObject(bytes);
            objectOutputStream.flush();
            //objectOutputStream.writeObject("Hello world");
            objectOutputStream.flush();
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
    public Object getData() throws IOException, ClassNotFoundException {
       byte[] bytes = (byte[]) objectInputStream.readObject();
        try {
            byte[] decryptedBytes = syncKeyDecryptor.doFinal(bytes);
            return convertToObject(decryptedBytes);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
    public void sendData(Object object){
        try {
            byte[] bytes = convertToByteArray(object);
            byte[] encryptedBytes = syncKeyEncryptor.doFinal(bytes);
            objectOutputStream.writeObject(encryptedBytes);
            objectOutputStream.flush();
        } catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
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
}
