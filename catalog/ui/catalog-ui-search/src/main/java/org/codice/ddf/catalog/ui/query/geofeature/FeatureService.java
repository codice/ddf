package org.codice.ddf.catalog.ui.query.geofeature;

import java.util.List;

/**
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */

public interface FeatureService {
    List<String> getSuggestedFeatureNames(String query, int maxResults);
    Feature getFeatureByName(String name);
}
