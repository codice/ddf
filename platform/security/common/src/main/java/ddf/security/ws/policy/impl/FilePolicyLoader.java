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
package ddf.security.ws.policy.impl;

import ddf.security.ws.policy.PolicyLoader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.platform.util.XMLUtils;
import org.osgi.framework.BundleContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/** Uses the filesystem to load a policy file. */
public class FilePolicyLoader implements PolicyLoader {

  private static volatile Document policyDoc;

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  /**
   * Creates a new instance of the file policy loader.
   *
   * @param context Used to obtain the file from the file system.
   * @param policyLocation Location of the file within the bundle classpath.
   * @throws IOException If an error occurs while trying to load the policy file.
   */
  public FilePolicyLoader(BundleContext context, String policyLocation) throws IOException {
    URL policyURL = context.getBundle().getResource(policyLocation);
    FilePolicyLoader.policyDoc = loadFromFile(policyURL);
  }

  /**
   * Loads the policy and converts it into a Document.
   *
   * @param policyFileURL URL that is based in from the bundlecontext.
   * @return The policy in a Document format.
   * @throws IOException If an error occurs while trying to parse the file into a Document.
   */
  protected Document loadFromFile(URL policyFileURL) throws IOException {
    InputStream policyStream = null;
    Document doc = null;
    if (policyFileURL != null) {
      try {
        policyStream = policyFileURL.openStream();
        DocumentBuilder dBuilder = XML_UTILS.getSecureDocumentBuilder(true);
        doc = dBuilder.parse(policyStream);
      } catch (IOException | ParserConfigurationException | SAXException e) {
        throw new IOException("Could not read policy file located at " + policyFileURL, e);
      } finally {
        IOUtils.closeQuietly(policyStream);
      }
    }
    if (doc == null) {
      throw new IOException(
          "Could not find policy file. No valid location given: " + policyFileURL);
    }
    return doc;
  }

  @Override
  public Document getPolicy() {
    return FilePolicyLoader.policyDoc;
  }
}
