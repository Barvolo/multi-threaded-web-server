
Project Overview:
-----------------
This Java-based project implements a multi-threaded web server designed for efficient handling of simultaneous client connections and serving static web content. It is built with a focus on modularity, scalability and security. The server is configurable through external settings.

Source Code (`Sources`) Directory:
------------------------------
- `PathValidator.java`: Validates requested paths to ensure they are secure and within the server's designated root directory, preventing directory traversal attacks.

- `ExceptionHandler.java`: Manages exception handling across the server, providing a unified approach to error logging and response.

- `ServerConfiguration.java`: Loads server configurations from an external file, facilitating easy adjustments to settings like the server port and document root.

- `RequestResponseLogger.java`: Implements logging for HTTP requests and responses, aiding in monitoring server activity and debugging.

- `HttpStatusCode.java`: Enumerates HTTP status codes, offering a readable and maintainable way to manage response codes throughout the server.

- `MultiThreadedWebServer.java`: The backbone of the server, handling incoming connections on a dedicated port and processing them concurrently using a thread pool.

- `Main.java`: The entry point for the server application, responsible for initializing configurations and bootstrapping the server.

- `HttpRequestHandler.java`: Processes individual HTTP requests in a separate thread, parsing the request, determining the response and serving content or executing server logic.

`HttpRequestHandler.java` Detailed Explanation:
------------------------------------------------
`HttpRequestHandler` is pivotal to the server's functionality, tasked with processing each client's HTTP request within its thread. It uses a `Socket` object to communicate, reading requests and sending back responses. Key responsibilities include:

- **Request Parsing**: Analyzes the HTTP request to extract method headers, and path. It's crucial for routing and responding to GET, POST, HEAD and TRACE requests.

- **Header Management**: Parses headers to understand client preferences, such as requesting chunked responses with "Chunked: yes".

- **Content Serving**: For GET requests, serves static files from the server's root directory, applying security checks to prevent unauthorized access.

- **Data Handling**: Processes data from POST requests, potentially handling form submissions or API data.

- **Security and Validation**: Uses `PathValidator` to ensure requested paths are within allowed boundaries, safeguarding against directory traversal.

- **Error and Response Handling**: Generates appropriate HTTP responses, including error handling for common issues like 404 Not Found or 500 Server Error.

This class embodies the server's response logic, directly impacting performance, security, and reliability.

Web Content (`www`) Directory:
------------------------------
The `www` directory, especially under `www/lab/html`, hosts the static files served by the web server:

- `index.html`: The homepage of the web application, serving as the primary user interface.
- `params_info.html`: Displays submitted request parameters, useful for debugging and informational purposes.
- `errors.html`: Provides user-friendly error messages for various HTTP errors encountered by users.
- Image Assets: Include visual elements like `nir_ben_itach_sad.png`, `nir_ben_itach_happy.png`, `bar_volovski_sad.png`, and `bar_volovski_happy.png` for enriched user experience.
- `nir_bar_favicon.ico`: The website's favicon, enhancing brand visibility and user recognition in browser tabs.

Design and Implementation Philosophy:
-------------------------------------
The server's architecture emphasizes clean design principles, with each component handling a distinct aspect of server operation. By leveraging a multi-threaded model, the server efficiently manages multiple connections. Configuration externalization and comprehensive logging support operational flexibility and maintainability. Security features like path validation are integrated to ensure robustness against common web vulnerabilities.

Build and Run Instructions:
---------------------------
Compile the project by executing the `compile.sh` script and then utilize the `run.sh` script to start the server.
