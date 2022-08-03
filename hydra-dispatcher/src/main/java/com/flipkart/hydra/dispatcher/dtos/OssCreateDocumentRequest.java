package com.flipkart.hydra.dispatcher.dtos;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.Map;

@JsonSerialize
public class OssCreateDocumentRequest {

    private String name;
    private String type;
    private String id;
    private Map<String, Object> fieldMapPair;
    private long createdAtEpoch;
    private long updatedAtEpoch;
    private long version;

    public OssCreateDocumentRequest(String name, String type, String id, Map<String, Object> fieldMapPair, long createdAtEpoch, long updatedAtEpoch, long version) {
        this.name = name;
        this.type = type;
        this.id = id;
        this.fieldMapPair = fieldMapPair;
        this.createdAtEpoch = createdAtEpoch;
        this.updatedAtEpoch = updatedAtEpoch;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getFieldMapPair() {
        return fieldMapPair;
    }

    public void setFieldMapPair(Map<String, Object> fieldMapPair) {
        this.fieldMapPair = fieldMapPair;
    }

    public long getCreatedAtEpoch() {
        return createdAtEpoch;
    }

    public void setCreatedAtEpoch(long createdAtEpoch) {
        this.createdAtEpoch = createdAtEpoch;
    }

    public long getUpdatedAtEpoch() {
        return updatedAtEpoch;
    }

    public void setUpdatedAtEpoch(long updatedAtEpoch) {
        this.updatedAtEpoch = updatedAtEpoch;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "DocumentRequest{" + "name='" + name + '\'' + ", type='" + type + '\'' + ", id='" + id + '\''
                + ", fieldMapPair=" + fieldMapPair + ", createdAtEpoch=" + createdAtEpoch
                + ", updatedAtEpoch=" + updatedAtEpoch + ", version=" + version + '}';
    }
}