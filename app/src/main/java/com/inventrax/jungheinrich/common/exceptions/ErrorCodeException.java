package com.inventrax.jungheinrich.common.exceptions;

/**
 * Created by nareshp on 07/01/2016.
 */
public class ErrorCodeException extends AbstractException {

    private static final long serialVersionUID = -7477869088363031784L;

    private ErrorCode errorCode;
    private Object[] errorCodeDescriptionArgs;

    public ErrorCodeException(ErrorCode errorCode, String message, Throwable throwable,
                              Object... errorCodeDescriptionArgs) {
        super(message != null ? message : errorCode.toString(), throwable);
        setTrackable(false);
        this.errorCode = errorCode;
        this.errorCodeDescriptionArgs = errorCodeDescriptionArgs;
    }

    public ErrorCodeException(ErrorCode errorCode, Throwable throwable) {
        this(errorCode, null, throwable);
    }

    public ErrorCodeException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public ErrorCodeException(ErrorCode errorCode, Object... errorCodeDescriptionArgs) {
        this(errorCode, null, null, errorCodeDescriptionArgs);
    }

    public ErrorCodeException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    /**
     * @return the errorCode
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getErrorCodeDescriptionArgs() {
        return errorCodeDescriptionArgs;
    }

    public com.inventrax.jungheinrich.common.exceptions.ErrorCodeException setTrackable(Boolean trackable) {
        return (com.inventrax.jungheinrich.common.exceptions.ErrorCodeException)super.setTrackable(trackable);
    }
}