package pt.ua.tqs.barkito.bdd;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Runs every scenario under src/test/resources/features with the default Maven build.
 * Scenarios tagged @wip are merged before their step definitions (specification-driven
 * stories, QA Manual 6.1) and are skipped until the implementation removes the tag.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "pt.ua.tqs.barkito.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip")
class CucumberTest {
}
