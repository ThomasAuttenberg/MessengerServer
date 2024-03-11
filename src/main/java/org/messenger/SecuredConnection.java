package org.messenger;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.security.*;

public class SecuredConnection extends Connection{

    private static final KeyPairGenerator keyPairGenerator;
    private static Key privateKey;
    private static Key publicKey;
    private static final Cipher encryptCipher;
    private static final Cipher decryptCipher;
    private Key usersPublicKey;

    static{

        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        URI publicKeyFileURL = URI.create("src/main/resources/publicKey");
        URI privateKeyFileURL = URI.create("src/main/resources/privateKey");
        File publicKeyFile = new File(publicKeyFileURL);
        File privateKeyFile = new File(privateKeyFileURL);
        try {
            FileInputStream fileInputStream = new FileInputStream(publicKeyFile);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            publicKey = (Key) objectInputStream.readObject();
            fileInputStream = new FileInputStream(privateKeyFile);
            objectInputStream = new ObjectInputStream(fileInputStream);
            privateKey = (Key) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(publicKeyFile);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                KeyPair keys = keyPairGenerator.generateKeyPair();
                privateKey = keys.getPrivate();
                publicKey = keys.getPublic();
                objectOutputStream.writeObject(keys.getPublic());
                fileOutputStream = new FileOutputStream(privateKeyFile);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(keys.getPrivate());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        try {
            encryptCipher = Cipher.getInstance("RSA");
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            decryptCipher = Cipher.getInstance("RSA");
            decryptCipher.init(Cipher.DECRYPT_MODE,privateKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }


    }

    SecuredConnection(Socket socket) {
        super(socket);
        JSONObject publicKeyTransmitter = new JSONObject();
        publicKeyTransmitter.put("publicKey",publicKey);
        try {
            send(publicKeyTransmitter);
            usersPublicKey = (Key) getRequest().get("publicKey");
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JSONObject getRequest() throws IOException, ParseException {
        return super.getRequest();
    }

    @Override
    public void send(JSONObject dataPacket) throws IOException {
        super.send();
    }
}
