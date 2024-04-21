public enum HttpStatusCode {
    OK_200("200 OK"),
    NOT_FOUND_404("404 Not Found"),
    NOT_IMPLEMENTED_501("501 Not Implemented"),
    BAD_REQUEST_400("400 Bad Request"),
    INTERNAL_SERVER_ERROR_500("500 Internal Server Error");

    private final String statusDescription;

    HttpStatusCode(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public String getStatusDescription() {
        return statusDescription;
    }
}