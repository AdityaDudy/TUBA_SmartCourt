package in.tubalaw.courtos.modules.tracker.service.provider;

public class ProviderException extends Exception {

    public enum ErrorCode {
        /** CNR not found on eCourts */
        NOT_FOUND,
        /** Network failure, parse error, or provider down */
        SERVICE_UNAVAILABLE
    }

    private final ErrorCode errorCode;

    public ProviderException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProviderException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
