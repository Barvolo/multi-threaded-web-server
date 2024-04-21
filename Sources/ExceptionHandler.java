import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExceptionHandler {
    public static void handleException(Exception e) {
        logExceptionToConsole(e);
    }

    private static void logExceptionToConsole(Exception e) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        System.out.println("[" + formattedDateTime + "] Exception Details:");
        e.printStackTrace(System.err);
        System.out.println("--------------------------------------------------------");
    }
}