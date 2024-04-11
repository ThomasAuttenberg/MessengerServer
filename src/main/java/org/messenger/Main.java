package org.messenger;

import org.messenger.data.DataBaseConnection;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.ZoneOffset;

public class Main {
    public static ZoneOffset zoneOffset = ZoneOffset.of("+00:00");
    private static boolean isRunning = true;
    private static final int serverPort = 9000;
    private static final int notificationPort = 9001;
    private static Runnable mainServer;
    private static Runnable notificationServer;

    public static void main(String[] args) {

        try {

            KeyStore keyStore = KeyStore.getInstance("JKS");
            char[] password = "qwerty".toCharArray();

            String keyStorePathString = "src/main/resources/keystore.jks";

            Path keyStorePath = Paths.get(keyStorePathString).toAbsolutePath();

            keyStore.load(new FileInputStream(keyStorePath.toFile()), password);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);

            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
            ServerSocket serverSocket = sslServerSocketFactory.createServerSocket(serverPort);
            ServerSocket notificationSocket = sslServerSocketFactory.createServerSocket(notificationPort);

            DataBaseConnection connection_ = new DataBaseConnection();
            connection_.close();

            mainServer = () -> {

                while (isRunning) {
                    try {
                        //System.out.println("Waiting for connect via socket");
                        Socket connection = serverSocket.accept();
                        ConnectionManager.connect(connection);
                        //System.out.println("Connect via socket");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            notificationServer = () -> {
                while (isRunning) {
                    try {
                        //ystem.out.println("Waiting for notification connect via socket");
                        Socket connection = notificationSocket.accept();
                        NotificationManager.connect(connection);
                        //System.out.println("Connect via notification socket");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            new Thread(mainServer).start();
            new Thread(notificationServer).start();

        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException |
                 UnrecoverableKeyException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}