package com.flipkart.hydra.dispatcher.sal;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.flipkart.hydra.dispatcher.dtos.OSSSearchResponse;
import com.flipkart.hydra.dispatcher.dtos.OssCreateDocumentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.hydra.dispatcher.exception.OssSearchClientException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class OssSearchClient {

    private static final String TENANT = "tenant";
    private static final String X_EKART_CLIENT = "X_EKART_CLIENT";
    private static final String CONTENT_TYPE_KEY = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json";
    private static final String GET_OSS_URL = "/api/v1/opsearch/documents";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String DOCUMENT_REQUESTS = "document_requests";
    private static final String CREATED_AT_EPOCH = "created_at_epoch";
    private static final String FIELD_MAP_PAIR = "field_map_pair";
    private static final String ID = "id";
    private static final String UPDATED_AT_EPOCH = "updated_at_epoch";
    private static final String VERSION = "version";

    private final RestTemplate restTemplate;
    private final String clientUrl;
    //private final AuthUtil authUtil;

    public OssSearchClient(RestTemplate restTemplate, String clientUrl) {

        this.restTemplate = checkNotNull(restTemplate);
        this.clientUrl = checkNotNull(clientUrl);
    }

    @Timed
    @ExceptionMetered
    public OSSSearchResponse createOSSDocument(OssCreateDocumentRequest request, String tenantId, String clientId) {

        Map<String, Object> requestParams = convertSearchRequesttoMap(request);

        String url = clientUrl + GET_OSS_URL;
        //        url = apiUtil.url(url, ImmutableMap.copyOf(requestParams));

        HttpHeaders headers = new HttpHeaders();
        headers.add(X_EKART_CLIENT, clientId);
        headers.add(TENANT, tenantId);
        headers.add(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE);
        try {
            HttpEntity<OSSSearchResponse> response = restTemplate
                    .exchange(url, HttpMethod.PUT, new HttpEntity<>(requestParams, headers), OSSSearchResponse.class);

            return response.getBody();
        } catch (Exception e) {
            throw new OssSearchClientException(e.getMessage());
        }
    }

    public Map<String, Object> convertSearchRequesttoMap(OssCreateDocumentRequest request) {

        Map<String, Object> requestParams = new HashMap<>();
        List<Map<String, Object>> documentRequests = new ArrayList<>();
        Map<String, Object> documentRequestsMap = new HashMap<>();
        documentRequestsMap.put(CREATED_AT_EPOCH, request.getCreatedAtEpoch());
        documentRequestsMap.put(FIELD_MAP_PAIR, request.getFieldMapPair());
        documentRequestsMap.put(ID, request.getId());
        documentRequestsMap.put(NAME, request.getName());
        documentRequestsMap.put(TYPE, request.getType());
        documentRequestsMap.put(UPDATED_AT_EPOCH, request.getUpdatedAtEpoch());
        documentRequestsMap.put(VERSION, request.getVersion());
        documentRequests.add(documentRequestsMap);
        requestParams.put(DOCUMENT_REQUESTS, documentRequests);
        return requestParams;
    }
}
