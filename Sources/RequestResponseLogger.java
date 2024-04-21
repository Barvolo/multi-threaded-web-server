import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RequestResponseLogger {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");


    public static void logRequest(String request) {
        if (request != null) {
            String currentTime = LocalDateTime.now().format(dateTimeFormatter);
            System.out.println(ANSI_YELLOW + "[" + currentTime + "] " + ANSI_RESET + ANSI_BLUE + "Request: " + request + ANSI_RESET);
        }
    }
}