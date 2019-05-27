package hr.fer.zemris.projekt.neural.exceptions;

public class RetrainNetworkException extends Exception{
    public RetrainNetworkException() {
        super();
    }

    public RetrainNetworkException(String message) {
        super(message);
    }

    protected RetrainNetworkException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
