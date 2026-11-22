package pt.ua.tqs.barkito.bdd;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import io.cucumber.spring.CucumberContextConfiguration;

/** Runs the Gherkin scenarios against the whole application through MockMvc. */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
public class CucumberSpringConfiguration {
}
