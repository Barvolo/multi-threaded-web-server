import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class PathValidator {
    public static String validatePath(String requestedPath, String rootPath) {
        Path resolvedPath = FileSystems.getDefault().getPath(rootPath, requestedPath).normalize();
        if (!resolvedPath.startsWith(rootPath)) {
            return null;
        }
        return resolvedPath.toString();
    }
    public static Boolean fileExists(String filePath, String rootPath) {
        String cleanFilePath = filePath.split("\\?")[0];

        Path resolvedPath = FileSystems.getDefault().getPath(rootPath, cleanFilePath).normalize();
        Path fullPath = resolvedPath.resolve(cleanFilePath).normalize();

        return (Files.exists(resolvedPath) && Files.isRegularFile(resolvedPath)) ||
               (Files.exists(fullPath) && Files.isRegularFile(fullPath));
    }
}