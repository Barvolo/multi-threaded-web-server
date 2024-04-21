import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfiguration {
    private int port;
    private String root;
    private String defaultPage;
    private int maxThreads;

    public ServerConfiguration() {
        readConfiguration();
    }

    private void readConfiguration() {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream("config.ini")) {
            properties.load(input);

            port = Integer.parseInt(properties.getProperty("port", "8080"));
            root = properties.getProperty("root", "www/lab/html/");
            defaultPage = properties.getProperty("defaultPage", "index.html");
            maxThreads = Integer.parseInt(properties.getProperty("maxThreads", "10"));
        } catch (IOException e) {
            ExceptionHandler.handleException(e);
        }
    }

    public int getPort() {
        return port;
    }

    public String getRoot() {
        return root;
    }

    public String getDefaultPage() {
        return defaultPage;
    }

    public int getMaxThreads() {
        return maxThreads;
    }
}