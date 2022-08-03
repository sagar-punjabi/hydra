package com.flipkart.hydra.dispatcher.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OSSSearchResponse {

    @JsonProperty(value = "success")
    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "OSSSearchResponse{" +
                "success=" + success +
                '}';
    }
}
