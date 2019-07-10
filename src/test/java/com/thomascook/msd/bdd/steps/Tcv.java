package com.thomascook.msd.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cucumber.api.java.en.Given;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Assert;

import java.util.Random;
import java.util.UUID;

@Slf4j
public class Tcv {
    static final String CORRELATION_ID_HEADER = "tc-correlation-id";
    static final String INITIATING_SYSTEM_ID_HEADER = "tc-initiating-system-id";

    static String correlationId;

    private ObjectMapper objectMapper = new ObjectMapper();

    static final Config config = ConfigFactory.load();

    @Given("^TCV sent booking create (\\S+) with (\\S+)$")
    public void sendBookingCreate(String requestBodyFile, String initiatingSystemId) throws Exception {
        String requestBody = IOUtils.toString(getClass().getResourceAsStream(requestBodyFile), "UTF-8");
        JsonNode requestBodyNode = objectMapper.readTree(requestBody);
        ((ObjectNode) requestBodyNode.path("summary")).put("customerID", Msd.customerId);

        String requestBookingCreation = String.valueOf(requestBodyNode);

        correlationId = UUID.randomUUID().toString();
        log.info("correlationId: {}", correlationId);

        String bookingId = String.valueOf(getRandomNumberInts(1, 9));
        log.info("bookingId: {}", bookingId);

        String path = config.getString("tcvUpdateBooking.updateUrl").replaceAll("\\{bookingId}", bookingId);
        log.info("Sending booking create request: {}", requestBookingCreation);

        HttpResponse<String> response = Unirest.put(path)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(CORRELATION_ID_HEADER, correlationId)
                .header(INITIATING_SYSTEM_ID_HEADER, initiatingSystemId)
                .body(requestBookingCreation).asString();

        Assert.assertEquals("Unexpected response: " + response.getStatus(), HttpStatus.SC_OK, response.getStatus());
    }

    public static int getRandomNumberInts(int min, int max) {
        Random random = new Random();
        return random.ints(min, (max + 100000000)).findFirst().getAsInt();
    }



}