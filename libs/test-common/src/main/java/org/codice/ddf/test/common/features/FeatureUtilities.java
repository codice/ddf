/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.test.common.features;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.codice.ddf.platform.util.XMLUtils;
import org.ops4j.pax.exam.options.UrlProvisionOption;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class FeatureUtilities {

  public static final String FEATURE_NAME_XPATH = "//*[local-name() = 'feature']/@name";

  private FeatureUtilities() {}

  /**
   * Returns a list of feature names defined in a feature file.
   *
   * @param featureFilePath
   * @return feature names in feature file
   */
  @SuppressWarnings("squid:S00112" /* Runtime exception is fine in this context */)
  public static List<String> getFeaturesFromFeatureRepo(String featureFilePath) {
    XPath xPath = XPathFactory.newInstance().newXPath();
    List<String> featureNames = new ArrayList<>();

    try (FileInputStream fi = new FileInputStream(new File(featureFilePath))) {
      Document featuresFile = XMLUtils.getInstance().getSecureDocumentBuilder(false).parse(fi);

      NodeList features =
          (NodeList)
              xPath.compile(FEATURE_NAME_XPATH).evaluate(featuresFile, XPathConstants.NODESET);

      for (int i = 0; i < features.getLength(); i++) {
        featureNames.add(features.item(i).getNodeValue());
      }
    } catch (ParserConfigurationException
        | XPathExpressionException
        | IOException
        | SAXException e) {
      throw new RuntimeException(
          "Unable to read features names in feature file at: " + featureFilePath, e);
    }
    return featureNames;
  }

  /**
   * Converts the given feature file into a list of feature name parameters for parameterized
   * testing.
   *
   * @param featureFilePath
   * @return feature name parameters
   */
  public static List<Object[]> featureRepoToFeatureParameters(String featureFilePath) {
    return featureRepoToFeatureParameters(featureFilePath, Collections.emptyList());
  }

  /**
   * Converts the given feature file into a list of feature name parameters for parameterized
   * testing.
   *
   * @param featureFilePath
   * @param ignoredFeatures excludes the specified features from the parameters
   * @return feature name parameters
   */
  public static List<Object[]> featureRepoToFeatureParameters(
      String featureFilePath, List<String> ignoredFeatures) {
    return getFeaturesFromFeatureRepo(featureFilePath)
        .stream()
        .filter(f -> !ignoredFeatures.contains(f))
        .map(feat -> new Object[] {feat})
        .collect(Collectors.toList());
  }

  /**
   * Creates a feature repo object from the specified feature file
   *
   * @param filePath
   * @return
   */
  public static FeatureRepo toFeatureRepo(String filePath) {
    return new FeatureRepoImpl(new UrlProvisionOption("file:" + filePath));
  }

  /**
   * Creates a feature object from the specified feature file
   *
   * @param filePath
   * @return
   */
  public static Feature toFeature(String filePath, String feature) {
    return new FeatureImpl(toFeatureRepo(filePath).getFeatureFileUrl(), feature);
  }
}
