package com.flipkart.hydra.dispatcher.exception;

public class OssSearchClientException extends BaseException {
    private static final String RESOURCE_CLIENT_EXCEPTION = "RESOURCE_CLIENT_EXCEPTION";
    public OssSearchClientException(String message) {
        super(message, RESOURCE_CLIENT_EXCEPTION);
    }
}
