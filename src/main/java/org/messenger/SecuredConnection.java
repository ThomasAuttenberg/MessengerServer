package org.messenger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.Arrays;

public class SecuredConnection extends Connection{

    private static final KeyPairGenerator keyPairGenerator;
    private static Key RSAprivateKey;
    private static Key RSApublicKey;
    private static final Cipher RSAencryptCipher;
    private static final Cipher RSAdecryptCipher;
    private Key userSecretKey;
    private Cipher userEncryptCipher;
    private Cipher userDecryptCipher;

    static{

        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        String publicKeyPath = "src/main/resources/publicKey";
        String privateKeyPath = "src/main/resources/privateKey";

        Path publicKeyPathObj = Paths.get(publicKeyPath).toAbsolutePath();
        Path privateKeyPathObj = Paths.get(privateKeyPath).toAbsolutePath();

        URI publicKeyFileURL = publicKeyPathObj.toUri();
        URI privateKeyFileURL = privateKeyPathObj.toUri();

        File publicKeyFile = new File(publicKeyFileURL);
        File privateKeyFile = new File(privateKeyFileURL);

        try {
            FileInputStream fileInputStream = new FileInputStream(publicKeyFile);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            RSApublicKey = (Key) objectInputStream.readObject();
            fileInputStream = new FileInputStream(privateKeyFile);
            objectInputStream = new ObjectInputStream(fileInputStream);
            RSAprivateKey = (Key) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(publicKeyFile);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                KeyPair keys = keyPairGenerator.generateKeyPair();
                RSAprivateKey = keys.getPrivate();
                RSApublicKey = keys.getPublic();
                objectOutputStream.writeObject(keys.getPublic());
                fileOutputStream = new FileOutputStream(privateKeyFile);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(keys.getPrivate());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        try {
            RSAencryptCipher = Cipher.getInstance("RSA");
            RSAencryptCipher.init(Cipher.ENCRYPT_MODE, RSApublicKey);
            RSAdecryptCipher = Cipher.getInstance("RSA");
            RSAdecryptCipher.init(Cipher.DECRYPT_MODE, RSAprivateKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }


    }

    SecuredConnection(Socket socket) {
        super(socket);
        try {
            super.send(RSApublicKey);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] bytes;
        try {
            bytes = (byte[]) super.getRequest();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        byte[] decryptedBytes;
        try {
            decryptedBytes = RSAdecryptCipher.doFinal(bytes);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
        userSecretKey = new SecretKeySpec(decryptedBytes,"AES");
        System.out.println(Arrays.toString(userSecretKey.getEncoded()));
        try {
            userEncryptCipher = Cipher.getInstance("AES");
            userDecryptCipher = Cipher.getInstance("AES");
            userEncryptCipher.init(Cipher.ENCRYPT_MODE,userSecretKey);
            userDecryptCipher.init(Cipher.DECRYPT_MODE,userSecretKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getRequest() throws IOException, ClassNotFoundException {
        //ByteArrayOutputStream objectToBytesStream = new ByteArrayOutputStream();
       // ObjectOutputStream objectOutputStream = new ObjectOutputStream(objectToBytesStream);
       // Object requestObject = super.getRequest();
        //objectOutputStream.writeObject(requestObject);
        byte[] bytes = (byte[]) super.getRequest();
        byte[] decryptedBytes = null;
        try {
            decryptedBytes = userDecryptCipher.doFinal(bytes);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
        return convertToObject(decryptedBytes);
    }

    @Override
    public void send(Object object) throws IOException {
        try {
            byte[] encryptedBytes = userEncryptCipher.doFinal(convertToByteArray(object));
            super.send(encryptedBytes);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object convertToObject(byte[] byteArray) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return objectInputStream.readObject();
        }
    }
    public static byte[] convertToByteArray(Object object) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(object);
            return byteArrayOutputStream.toByteArray();
        }
    }
}
