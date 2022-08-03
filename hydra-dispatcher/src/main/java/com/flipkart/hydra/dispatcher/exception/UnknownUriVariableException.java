package com.flipkart.hydra.dispatcher.exception;

import java.util.List;

public class UnknownUriVariableException extends BaseException {

    private static final long serialVersionUID = 2365245302982656858L;

    private static final String UNKNOWN_URI_VARIABLE = "UNKNOWN_URI_VARIABLE";

    public UnknownUriVariableException(List<String> unknownUriVariables) {

        super("Unknown URI Variables : " + unknownUriVariables, UNKNOWN_URI_VARIABLE);
    }
}
