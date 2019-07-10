package com.thomascook.msd.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cucumber.api.java.en.Then;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Assert;

import java.io.IOException;


public class Kibana {
    static final Config config = ConfigFactory.load();

    static JsonNode messageBody;

    private ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode getKibanaMessageBody(String file, int retries, long pollIntervalMs) throws IOException, InterruptedException, UnirestException {
        String requestBody = IOUtils.toString(getClass().getResourceAsStream(file), "UTF-8");
        JsonNode requestBodyNode = objectMapper.readTree(requestBody);

        ((ObjectNode) requestBodyNode.path("query").path("bool").path("must").path(3).path("match")).put("correlationId", Tcv.correlationId);
        String publishMessage = String.valueOf(requestBodyNode);

        messageBody = null;
        for (int i = 0; i < retries; i++) {
            HttpResponse<String> response = Unirest.post(config.getString("kibana.url") + ":" +
                    config.getString("kibana.port") +
                    config.getString("kibana.action"))
                    .header(HttpHeaders.AUTHORIZATION, config.getString("kibana.authorization"))
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(publishMessage).asString();

            Assert.assertEquals("Unexpected response: " + response.getStatus(), HttpStatus.SC_OK, response.getStatus());

            JsonNode responseBodyKibana = objectMapper.readTree(response.getBody());
            messageBody = responseBodyKibana.path("hits").path("hits").findPath("message");
            if (!messageBody.isMissingNode() && !messageBody.isNull())
                return messageBody;
            Thread.sleep(pollIntervalMs);
        }
        return messageBody;
    }

    @Then("wait (\\w+) milliseconds before booking processed")
    public void sleep(int milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }

    @Then("execute (\\S+)$")
    public JsonNode messageKibana(String requestBodyKibana) throws Exception {
        return getKibanaMessageBody(requestBodyKibana, 6, 15000);
    }
}

