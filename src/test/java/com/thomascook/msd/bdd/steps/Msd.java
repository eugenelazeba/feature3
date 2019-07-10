package com.thomascook.msd.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cucumber.api.java.en.Given;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Assert;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class Msd {

    static final Config config = ConfigFactory.load();

    static final String CORRELATION_ID_HEADER = "tc-correlation-id";
    static final String INITIATING_SYSTEM_ID_HEADER = "tc-initiating-system-id";
    static final String SOURCE_MARKET = "tc-source-market";
    static final String SOURCE_SYSTEM_CUSTOMER_ID = "source-system-customer-id";
    static final String SOURCE_SYSTEM_ID = "tc-source-system-id";

    static String customerId;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Given("msd-manage-customer ms sent customer create (\\S+)")
    public void sendCustomerCreate(String requestBodyCustomerCreate) throws IOException, UnirestException {
        customerId = UUID.randomUUID().toString();
        log.info("customerId: " + customerId);

        String requestBody = IOUtils.toString(getClass().getResourceAsStream(requestBodyCustomerCreate), "UTF-8");
        JsonNode requestBodyNode = objectMapper.readTree(requestBody);

        ((ObjectNode) requestBodyNode.path("customer")).put("customerID", customerId);

        String requestCustomerCreation = String.valueOf(requestBodyNode);

        String path = config.getString("tcvPublishCustomerEvent.apiUrl").replaceAll("\\{customerId}", customerId);
        log.info("Sending customer create request: {}", requestCustomerCreation);

        HttpResponse<String> response = Unirest.put(path)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(CORRELATION_ID_HEADER, customerId)
                .header(INITIATING_SYSTEM_ID_HEADER, "WebRio")
                .header(SOURCE_MARKET, "GB")
                .header(SOURCE_SYSTEM_CUSTOMER_ID, customerId)
                .header(SOURCE_SYSTEM_ID, "TCV")
                .body(requestCustomerCreation).asString();

        Assert.assertEquals("Unexpected response: " + response.getStatus(), HttpStatus.SC_OK, response.getStatus());
    }
}

