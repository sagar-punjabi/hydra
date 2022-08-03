package com.flipkart.hydra.dispatcher.exception;

public abstract class BaseException extends RuntimeException {

    private static final long serialVersionUID = 2427986262234945416L;
    private final String errorCode;
    private final Object response;

    protected BaseException(String message, String errorCode) {

        super(message);
        this.errorCode = errorCode;
        this.response = null;
    }

    protected BaseException(String message, String errorCode, Object response) {

        super(message);
        this.errorCode = errorCode;
        this.response = response;
    }

    protected BaseException(String message, String errorCode, Throwable cause) {

        super(message, cause);
        this.errorCode = errorCode;
        this.response = null;
    }

    protected BaseException(String message, String errorCode, Object response, Throwable cause) {

        super(message, cause);
        this.errorCode = errorCode;
        this.response = response;
    }

    public String getErrorCode() {

        return errorCode;
    }

    public Object getResponse() {

        return response;
    }

}
