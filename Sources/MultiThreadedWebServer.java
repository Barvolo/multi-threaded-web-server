import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadedWebServer {
    private ServerConfiguration serverConfig;

    public MultiThreadedWebServer(ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void startServer() {
        try (ServerSocket welcomeSocket = new ServerSocket(serverConfig.getPort())) {
            System.out.println("Server started on port " + serverConfig.getPort());
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            while (true) {
                Socket clientSocket = welcomeSocket.accept();
                executorService.submit(new HttpRequestHandler(clientSocket, serverConfig));
            }
        } catch (IOException e) {
            ExceptionHandler.handleException(e);
        }
    }
}