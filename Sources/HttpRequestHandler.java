import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Collections;
import java.net.Socket;
import java.nio.file.FileSystems;

public class HttpRequestHandler implements Runnable {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");


    
    private Socket clientSocket;
    private ServerConfiguration serverConfig;

    public HttpRequestHandler(Socket clientSocket, ServerConfiguration serverConfig) {
        this.clientSocket = clientSocket;
        this.serverConfig = serverConfig;
    }

    @Override
    public void run() {
        // System.out.println("Handling connection in thread: " + Thread.currentThread().getName());
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            String request = in.readLine(); 
            if (request == null || request.isEmpty()) {
                return; 
            }

            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while (!(headerLine = in.readLine()).isEmpty()) {
                int separator = headerLine.indexOf(":");
                if (separator != -1) {
                    String name = headerLine.substring(0, separator).trim();
                    String value = headerLine.substring(separator + 1).trim();
                    headers.put(name.toLowerCase(), value); 
                }
            }
            boolean isChunkedRequested = "yes".equalsIgnoreCase(headers.get("chunked"));

            String[] requestParts = request.split(" ");
            String method = requestParts[0];
            String path = requestParts[1];

            if (method.equals("GET")) {
                handleGetRequest(request, path, out, false, isChunkedRequested);
            } else if (method.equals("POST")) {
                handlePostRequest(request, path, in, headers, out, isChunkedRequested);
            } else if (method.equals("HEAD")) {
                handleHeadRequest(request, path, out, isChunkedRequested);
            } else if (method.equals("TRACE")) {
                handleTraceRequest(request, out, isChunkedRequested);
            } else {
                sendErrorResponse(request, HttpStatusCode.NOT_IMPLEMENTED_501, out);
            }

        } catch (IOException e) {
            ExceptionHandler.handleException(e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                ExceptionHandler.handleException(e);
            }
        }
    }

    private void handleHeadRequest(String request, String path, PrintWriter out, boolean isChunkedRequested) {
        handleGetRequest(request, path, out, true, isChunkedRequested);
    }

    private void handleTraceRequest(String request, PrintWriter out, boolean isChunkedRequested) {
        out.println("HTTP/1.1 " + HttpStatusCode.OK_200.getStatusDescription());
        out.println("Content-Type: message/http");
        out.println("Content-Length: " + request.length());
        out.println(); 
        out.println(request);
        out.flush();
        String currentTime = LocalDateTime.now().format(dateTimeFormatter);
        RequestResponseLogger.logRequest(request); 
        System.out.println(ANSI_YELLOW + "[" + currentTime + "] " + ANSI_RESET + ANSI_GREEN + "Response:\nHTTP/1.1 " + HttpStatusCode.OK_200.getStatusDescription() + "\r\n" +
        "Content-Type: message/http\r\n" +
        "Content-Length: " + request.length() + "\r\n\r\n" +
        request + "\r\n" + ANSI_RESET);
        }

    private void handleGetRequest(String request, String path, PrintWriter out, boolean isHeadRequest, boolean isChunkedRequested) {
        if ("/".equals(path)) {
            path = "/" + serverConfig.getDefaultPage();
        }

        int paramsIndex = path.indexOf("?");
        String filePath = (paramsIndex != -1) ? path.substring(0, paramsIndex) : path;
        String queryParams = (paramsIndex != -1) ? path.substring(paramsIndex + 1) : null;

        String validatedPath = PathValidator.validatePath(filePath, serverConfig.getRoot());
        Boolean validateExistence = PathValidator.fileExists(filePath, serverConfig.getRoot());

        if (validatedPath != null && validateExistence) {
            if (queryParams != null && !queryParams.isEmpty()) {
                if (filePath.endsWith("params_info.html")) {
                    Map<String, String> parameters = parseParametersFromQuery(queryParams);
                    String htmlContent = buildHtmlContent(parameters);

                    out.println("HTTP/1.1 " + HttpStatusCode.OK_200.getStatusDescription());
                    out.println("Content-Type: text/html");
                    out.println("Content-Length: " + htmlContent.length());
                    out.println();
                    if (!isHeadRequest) {
                        out.println(htmlContent);
                    }
                    String currentTime = LocalDateTime.now().format(dateTimeFormatter);
                    RequestResponseLogger.logRequest(request);
                    System.out.println(ANSI_YELLOW + "[" + currentTime + "] " + ANSI_RESET + ANSI_GREEN + "Response:\nHTTP/1.1 " + HttpStatusCode.OK_200.getStatusDescription() + "\r\n" +
                            "Content-Type: text/html\r\n" +
                            (isChunkedRequested ? "Transfer-Encoding: chunked\r\n" : "") +
                            "Content-Length: " + htmlContent.length() + "\r\n" + ANSI_RESET);
                } else {
                    sendErrorResponse(request, HttpStatusCode.BAD_REQUEST_400, out);
                }
            } else {
                serveFile(request, validatedPath, out, isHeadRequest, isChunkedRequested);
            }
        } else {
            sendErrorResponse(request, HttpStatusCode.NOT_FOUND_404, out);
        }
    }

    private Map<String, String> parseParametersFromQuery(String queryParams) {
        Map<String, String> parameters = new HashMap<>();
        String[] parameterPairs = queryParams.split("&");

        for (String parameterPair : parameterPairs) {
            String[] keyValue = parameterPair.split("=");
            String key = capitalizeFirstLetter(keyValue[0]);

            if (keyValue.length == 2) {
                try {
                    parameters.put(key, java.net.URLDecoder.decode(keyValue[1], "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (keyValue.length == 1) {
                parameters.put(key, "");
            }
        }
        return parameters;
    }

    private void handlePostRequest(String request, String path, BufferedReader in, Map<String, String> headers, PrintWriter out, boolean isChunkedRequested) {
        try {
            if ("/".equals(path)) {
                path = "/" + serverConfig.getDefaultPage();
            }
            String validatedPath = PathValidator.validatePath(path, serverConfig.getRoot());
            boolean validateExistence = PathValidator.fileExists(path, serverConfig.getRoot());
            if (validatedPath != null && validateExistence) {
                if (path.contains("params_info.html")) {
                    Map<String, String> parameters = parseParameters(request, in, headers, out);

                    if (!parameters.isEmpty()) {
                        String htmlContent = buildHtmlContent(parameters);

                        out.println("HTTP/1.1 " + HttpStatusCode.OK_200.getStatusDescription());
                        out.println("Content-Type: text/html");
                        out.println("Content-Length: " + htmlContent.length());
                        out.println();
                        out.println(htmlContent);
                        String currentTime = LocalDateTime.now().format(dateTimeFormatter);
                        RequestResponseLogger.logRequest(request);
                        System.out.println(ANSI_YELLOW + "[" + currentTime + "] " + ANSI_RESET + ANSI_GREEN + "Response:\nHTTP/1.1 " + HttpStatusCode.OK_200.getStatusDescription() + "\r\n" +
                        "Content-Type: text/html\r\n" +
                        (isChunkedRequested ? "Transfer-Encoding: chunked\r\n" : "") +
                        "Content-Length: " + htmlContent.length() + "\r\n" + ANSI_RESET);
                    } else {
                        sendErrorResponse(request, HttpStatusCode.BAD_REQUEST_400, out);
                    }
                } else {
                    sendErrorResponse(request, HttpStatusCode.BAD_REQUEST_400, out);
                }
            } else {
                sendErrorResponse(request, HttpStatusCode.NOT_FOUND_404, out);
            }
        } catch (IOException e) {
            ExceptionHandler.handleException(e);
        }
    }

    private String buildHtmlContent(Map<String, String> parameters) {
        StringBuilder htmlContent = new StringBuilder();

        String params_info = (FileSystems.getDefault().getPath(serverConfig.getRoot(), "params_info.html").normalize()).toString();
        try (BufferedReader reader = new BufferedReader(new FileReader(params_info))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("<!-- Dynamic parameters will be inserted here -->")) {
                    TreeMap<String, String> sortedParameters = new TreeMap<>(parameters);
                    for (Map.Entry<String, String> entry : sortedParameters.entrySet()) {
                        htmlContent.append("<div class=\"parameter-row\">\n");
                        htmlContent.append("<div class=\"parameter-name\">").append(entry.getKey()).append("</div>\n");
                        htmlContent.append("<div class=\"parameter-value-container\">\n");
                        htmlContent.append("<div class=\"parameter-value\">").append(entry.getValue()).append("</div>\n");
                        htmlContent.append("</div>\n");
                        htmlContent.append("</div>\n");
                    }
                } else {
                    htmlContent.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            ExceptionHandler.handleException(e);
        }
        return htmlContent.toString();
    }

    private void serveFile(String request, String filePath, PrintWriter out, boolean isHeadRequest, boolean isChunkedRequested) {
        try {
            String baseFilePath = filePath.split("\\?")[0];
            byte[] fileBytes = Files.readAllBytes(Paths.get(baseFilePath));

            String contentType = getContentType(baseFilePath);

            if (isChunkedRequested) {
                out.println("HTTP/1.1 " + HttpStatusCode.OK_200.getStatusDescription());
                out.println("Content-Type: " + contentType);
                out.println("Transfer-Encoding: chunked");
                out.println();

                sendChunkedContent(fileBytes, clientSocket.getOutputStream());
            } else {
                out.println("HTTP/1.1 " + HttpStatusCode.OK_200.getStatusDescription());
                out.println("Content-Type: " + contentType);
                out.println("Content-Length: " + fileBytes.length);
                out.println();
                if (!isHeadRequest) {
                    out.flush();
                    clientSocket.getOutputStream().write(fileBytes);
                    clientSocket.getOutputStream().flush();
                }
            }

            RequestResponseLogger.logRequest(request);
            String currentTime = LocalDateTime.now().format(dateTimeFormatter);
            System.out.println(ANSI_YELLOW + "[" + currentTime + "] " + ANSI_RESET + ANSI_GREEN + "Response:\nHTTP/1.1 " + HttpStatusCode.OK_200.getStatusDescription() + "\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    (isChunkedRequested ? "Transfer-Encoding: chunked\r\n" : "") +
                    (!isHeadRequest ? "Content-Length: " + fileBytes.length + "\r\n" : "") + ANSI_RESET);

        } catch (IOException e) {
            sendErrorResponse(request, HttpStatusCode.INTERNAL_SERVER_ERROR_500, out);
        }
    }

    private void sendChunkedContent(byte[] content, OutputStream outputStream) throws IOException {
        int chunkSize = 1024; 
        int offset = 0;
        while (offset < content.length) {
            int remainingBytes = content.length - offset;
            int currentChunkSize = Math.min(chunkSize, remainingBytes);

            String chunkSizeHex = Integer.toHexString(currentChunkSize);
            outputStream.write((chunkSizeHex + "\r\n").getBytes());

            outputStream.write(content, offset, currentChunkSize);
            outputStream.write("\r\n".getBytes());

            offset += currentChunkSize;
        }
        outputStream.write("0\r\n\r\n".getBytes());
    }

    private void sendErrorResponse(String request, HttpStatusCode statusCode, PrintWriter out) {
        try {
            RequestResponseLogger.logRequest(request);
            String errors_page = (FileSystems.getDefault().getPath(serverConfig.getRoot(), "errors.html").normalize()).toString();
            String htmlContent = new String(Files.readAllBytes(Paths.get(errors_page)));
            
            htmlContent = htmlContent.replace("{{errorCode}}", String.valueOf(statusCode.getStatusDescription()));
            htmlContent = htmlContent.replace("{{errorMessage}}", statusCode.getStatusDescription());
            
            out.println("HTTP/1.1 " + statusCode.getStatusDescription());
            out.println("Content-Type: text/html");
            out.println("Content-Length: " + htmlContent.length());
            out.println(); 
            out.print(htmlContent);
            out.flush();
            String currentTime = LocalDateTime.now().format(dateTimeFormatter);
            System.out.println(ANSI_YELLOW + "[" + currentTime + "] " + ANSI_RESET + ANSI_RED + "Response:\nHTTP/1.1 " + statusCode.getStatusDescription() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + htmlContent.length() + "\r\n" + ANSI_RESET);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getContentType(String filePath) {
        String contentType = "application/octet-stream"; // default

        if (filePath.endsWith(".html")) {
            contentType = "text/html";
        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (filePath.endsWith(".png")) {
            contentType = "image/png";
        } else if (filePath.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (filePath.endsWith(".bmp")) {
            contentType = "image/bmp";
        } else if (filePath.endsWith(".ico") || filePath.endsWith(".icon")) {
            contentType = "image/icon";
        }
        return contentType;
    }

    private Map<String, String> parseParameters(String request, BufferedReader in, Map<String, String> headers, PrintWriter out) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        int contentLength = -1; 
        String line;
        if (headers.containsKey("content-length")) {
            try {
                contentLength = Integer.parseInt(headers.get("content-length"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else {
            return Collections.emptyMap();
        }

        StringBuilder requestBody = new StringBuilder();
        int readChar;
        while (contentLength > 0) {
            readChar = in.read();
            requestBody.append((char) readChar);
            contentLength--;
        }

        String requestBodyString = requestBody.toString();
        String[] parameterPairs = requestBodyString.split("&");

        for (String parameterPair : parameterPairs) {
            String[] keyValue = parameterPair.split("=");
            String key = capitalizeFirstLetter(keyValue[0]);

            // Exclude the key "loveit" from being added to the parameters, as we need it as part of the submission. By the way, thanks if you love it! 
            if (!key.equals("Loveit")) {
                if (keyValue.length == 2) {
                    try {
                        parameters.put(key, java.net.URLDecoder.decode(keyValue[1], "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else if (keyValue.length == 1) {
                    parameters.put(key, "");
                }
            }
        }
        return parameters;
    }

    private String capitalizeFirstLetter(String input) {
        if (input.length() > 0) {
            return Character.toUpperCase(input.charAt(0)) + input.substring(1);
        } else {
            return input;
        }
    }
}