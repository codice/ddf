package ddf.common.test.config;

import org.osgi.service.cm.Configuration;

/**
 * Created by elessard on 9/21/15.
 */
public class ConfigurationPropertyMatches implements ConfigurationPredicate {
    private String propertyName;

    private String valueRegex;

    public ConfigurationPropertyMatches(String propertyName, String valueRegex) {
        this.propertyName = propertyName;
        this.valueRegex = valueRegex;
    }

    @Override
    public boolean test(Configuration configuration) {
        if ((configuration == null) || (configuration.getProperties() == null) || (
                configuration.getProperties().get(propertyName) == null)) {
            return false;
        }

        return ((String) configuration.getProperties().get(propertyName)).matches(valueRegex);
    }
}
