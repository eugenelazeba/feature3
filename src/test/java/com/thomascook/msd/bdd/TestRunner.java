package com.thomascook.msd.bdd;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        glue = {"com.thomascook.msd.bdd.steps"},
        features = {"classpath:scenarios/"},
        format = {"pretty", "html:target/html/"},
        strict = true
)
@Slf4j
public class TestRunner {

    public TestRunner() {
        log.info("Starting test");
    }
}

