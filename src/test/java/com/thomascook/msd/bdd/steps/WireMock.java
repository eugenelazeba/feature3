package com.thomascook.msd.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.mashape.unirest.http.HttpMethod;
import com.thomascook.msd.bdd.util.CanonicalBookingJsonValidator;
import com.thomascook.msd.bdd.util.CanonicalCustomerJsonValidator;
import com.thomascook.msd.bdd.util.JSONAssertCompareIgnoreValues;
import com.thomascook.msd.bdd.util.WireMockProxyClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.core.io.FileSystemResourceLoader;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

@Slf4j
public class WireMock {
    static final Config config = ConfigFactory.load();

    private String msdUrlPattern;
    private WireMockProxyClient wireMockProxyClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    private CanonicalBookingJsonValidator canonicalBookingJsonValidator;
    private CanonicalCustomerJsonValidator canonicalCustomerJsonValidator;

    @Before
    public void setUp() throws Exception {
        URI msdApiUrl = new URIBuilder(config.getString("msd.apiUrl")).build();
        String msdBaseUrl = msdApiUrl.getScheme() + "://" + msdApiUrl.getHost();
        String proxyBaseUrl = config.getString("wireMock.baseUrl");

        wireMockProxyClient = new WireMockProxyClient(msdBaseUrl, proxyBaseUrl);
        msdUrlPattern = msdApiUrl.getPath();
        wireMockProxyClient.enableProxy(msdBaseUrl + msdUrlPattern, HttpMethod.PUT);

        canonicalBookingJsonValidator = new CanonicalBookingJsonValidator(new FileSystemResourceLoader(),
                ConfigFactory.load().getString("validation.jsonSchemaPath"));

        canonicalCustomerJsonValidator = new CanonicalCustomerJsonValidator(new FileSystemResourceLoader(),
                ConfigFactory.load().getString("validation.customerCreateSchemaPath"));
    }

    @Then("validate and verify booking (\\S+) according to schema")
    public void bookingValidation(String responseBodyFile) throws Exception {
        Thread.sleep(45000);
        JsonNode bookingToMsdActual = null;
        String bookingToMsdExpectedNoCustomerId = IOUtils.toString(getClass().getResourceAsStream(responseBodyFile));

        JsonNode requestBodyNode = objectMapper.readTree(bookingToMsdExpectedNoCustomerId);
        ((ObjectNode) requestBodyNode.path("customer").path("customerIdentifier")).put("customerID", Msd.customerId);

        String bookingToMsdExpected = String.valueOf(requestBodyNode);

        List<JsonNode> requests = wireMockProxyClient.findRequests(config.getString("msd.urlPathBooking"), HttpMethod.PUT);
        for (JsonNode r : requests) {
            Optional<JsonNode> found = Optional.empty();
            String correlationId = r.path("headers").path("tc-correlation-id").asText();
            if (correlationId.equals(Tcv.correlationId)) {
                JsonNode b = toJson(r.path("body").asText());
                found = Optional.of(b);
                bookingToMsdActual = found.get();

                ProcessingReport check = canonicalBookingJsonValidator.check(bookingToMsdActual);
                log.info("Validating booking with correlationId '{}'", Tcv.correlationId);
                assertTrue(check.isSuccess());

                compareExpectedActualJson(bookingToMsdExpected, bookingToMsdActual);
            }
        }

        Assert.assertNotNull("Booking was not created on MSD", bookingToMsdActual);
    }

    @Then("validate and verify customer (\\S+) according to schema")
    public void customerValidation(String responseBodyFile) throws Exception {
        Thread.sleep(45000);
        JsonNode customerToMsdActual = null;
        String customerToMsdExpectedNoCustomerId = IOUtils.toString(getClass().getResourceAsStream(responseBodyFile));

        JsonNode requestBodyNode = objectMapper.readTree(customerToMsdExpectedNoCustomerId);
        ((ObjectNode) requestBodyNode.path("customer").path("customerIdentifier")).put("customerID", Msd.customerId);

        String customerToMsdExpected = String.valueOf(requestBodyNode);

        List<JsonNode> requests = wireMockProxyClient.findRequests(config.getString("msd.urlPathCustomer"), HttpMethod.PUT);
        for (JsonNode r : requests) {
            Optional<JsonNode> found = Optional.empty();
            String customerId = r.path("headers").path("tc-source-system-customer-id").asText();
            if (customerId.equals(Msd.customerId)) {
                JsonNode b = toJson(r.path("body").asText());
                found = Optional.of(b);
                customerToMsdActual = found.get();

                ProcessingReport check = canonicalCustomerJsonValidator.check(customerToMsdActual);
                log.info("Validating customer with customerId '{}'", Msd.customerId);
                assertTrue(check.isSuccess());

                compareExpectedActualJson(customerToMsdExpected, customerToMsdActual);
            }
        }

        Assert.assertNotNull("Customer was not created on MSD", customerToMsdActual);
    }

    private void compareExpectedActualJson(String jsonToMsdExpected, JsonNode jsonToMsdActual) throws Exception {
        String expectedJson = jsonToMsdExpected;
        JsonNode actualJson = jsonToMsdActual;

        JSONAssert.assertEquals(
                expectedJson,
                String.valueOf(actualJson),
                new JSONAssertCompareIgnoreValues(JSONCompareMode.LENIENT,
                        "booking.bookingIdentifier.integrationProcessingInitiated"));
        log.info("Data was created on MSD");
    }

    private JsonNode toJson(String jsonAsString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(jsonAsString);
        } catch (IOException ioe) {
            return null;
        }
    }

    @After
    public void tearDown() throws Exception {
    }
}
