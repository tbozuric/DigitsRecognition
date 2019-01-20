package hr.fer.zemris.projekt.exceptions;


public class FileFormatException extends Exception {
    public FileFormatException() {
        super();
    }

    public FileFormatException(String message) {
        super(message);
    }

    protected FileFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
