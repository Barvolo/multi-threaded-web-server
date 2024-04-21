public class Main {
    public static void main(String[] args) {
        ServerConfiguration serverConfig = new ServerConfiguration();
        MultiThreadedWebServer server = new MultiThreadedWebServer(serverConfig);
        server.startServer();
    }
}