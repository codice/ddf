package ddf.common.test.config;

import org.osgi.service.cm.Configuration;

/**
 * Created by elessard on 9/21/15.
 */
public interface ConfigurationPredicate {
    public boolean test(Configuration configuration);
}
