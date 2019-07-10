package com.thomascook.msd.bdd.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpStatus;
import org.json.JSONArray;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class WireMockProxyClient {
    private final static Set<Integer> SUCCESSFUL_STATUSES = new HashSet<>();

    static {
        SUCCESSFUL_STATUSES.add(HttpStatus.SC_OK);
        SUCCESSFUL_STATUSES.add(HttpStatus.SC_CREATED);
    }

    private String MOCK_ADMIN_BASE_URL = "/__admin";
    private String ADD_NEW_URL = MOCK_ADMIN_BASE_URL + "/mappings/new";
    private String FIND_REQUESTS_URL = MOCK_ADMIN_BASE_URL + "/requests/find";
    private String RESET_REQUESTS_URL = MOCK_ADMIN_BASE_URL + "/requests/reset";
    private String GET_REQUESTS_URL = MOCK_ADMIN_BASE_URL + "/requests/";

    private String targetBaseUrl;
    private String proxyBaseUrl;

    private ObjectMapper objectMapper;

    public WireMockProxyClient(String targetBaseUrl, String proxyBaseUrl) {
        this.targetBaseUrl = targetBaseUrl;
        this.proxyBaseUrl = proxyBaseUrl;
        this.objectMapper = new ObjectMapper();
    }


    public void enableProxy(String urlPath, HttpMethod method) throws UnirestException {

        Unirest.setObjectMapper(new com.mashape.unirest.http.ObjectMapper() {

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return objectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return objectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        ObjectNode newMappingRequest = objectMapper.createObjectNode();

        newMappingRequest.set("request",
                objectMapper.createObjectNode()
                        .put("method", method.name())
                        .put("urlPath", urlPath));
        newMappingRequest.set("response",
                objectMapper.createObjectNode()
                        .put("proxyBaseUrl", targetBaseUrl+urlPath));

        String addNewUrl = proxyBaseUrl + ADD_NEW_URL;
        log.info("Enabling proxy on '{}' for '{}'", proxyBaseUrl, urlPath);

        HttpResponse response = Unirest.post(addNewUrl).body(newMappingRequest).asJson();

        log.info("Response status: {} {}", response.getStatus(), response.getStatusText());
        validateHttpStatus(response.getStatus());
    }

    public List<JsonNode> findRequests(String urlPath, HttpMethod method) throws UnirestException {
        ObjectNode findRequest = objectMapper.createObjectNode()
                .put("method", method.name())
                .put("urlPattern", urlPath);

        String findRequestsUrl = proxyBaseUrl + FIND_REQUESTS_URL;

        log.info("Find requests for '{}'", urlPath);
        HttpResponse<com.mashape.unirest.http.JsonNode> response = Unirest.post(findRequestsUrl).body(findRequest.toString()).asJson();

        log.info("Response status: {} {}", response.getStatus(), response.getStatusText());
        validateHttpStatus(response.getStatus());

        JSONArray requests = response.getBody().getObject().getJSONArray("requests");
        return StreamSupport.stream(requests.spliterator(), false)
                .map(r -> toJson(r.toString()))
                .collect(Collectors.toList());
    }

    public List<JsonNode> getRequests(String urlPattern, HttpMethod method) throws UnirestException {
        log.info("Get requests for '{}'", urlPattern);

        String getRequestsUrl = proxyBaseUrl + GET_REQUESTS_URL;
        HttpResponse<com.mashape.unirest.http.JsonNode> getRequests = Unirest.get(getRequestsUrl).asJson();

        log.info("Response status: {} {}", getRequests.getStatus(), getRequests.getStatusText());

        validateHttpStatus(getRequests.getStatus());

        JSONArray requests = getRequests.getBody().getObject().getJSONArray("requests");
        return StreamSupport.stream(requests.spliterator(), false)
                .map(r -> toJson(r.toString()))
                .collect(Collectors.toList());
    }

    public void resetRequests(String urlPattern, HttpMethod method) throws UnirestException {
        String resetRequestsUrl = proxyBaseUrl + RESET_REQUESTS_URL;

        log.info("Reset requests for '{}'", urlPattern);
        HttpResponse response = Unirest.post(resetRequestsUrl).asString();

        log.info("Response status: {} {}", response.getStatus(), response.getStatusText());
        validateHttpStatus(response.getStatus());
    }

    private void validateHttpStatus(int status) {
        Validate.isTrue(SUCCESSFUL_STATUSES.contains(status));
    }

    private JsonNode toJson(String jsonAsString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(jsonAsString);
        } catch (IOException ioe) {
            return null;
        }
    }
}
