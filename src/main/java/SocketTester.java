import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketTester {
    public static void main(String[] args) {
        Socket socket;
        try {
            socket = new Socket("127.0.0.1",9000);
            socket.getOutputStream().write("Hello there\n".getBytes(StandardCharsets.UTF_8));
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
}
