/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.test;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.karaf.tooling.exam.options.KarafDistributionKitConfigurationOption.Platform;
import org.apache.karaf.tooling.exam.options.KarafDistributionOption;
import org.apache.karaf.tooling.exam.options.LogLevelOption.LogLevel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.service.cm.Configuration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.source.FederatedSource;

/**
 * Tests Federation aspects.
 * 
 * @author Ashraf Barakat
 * @author Phillip Klinefelter
 * @author ddf.isgs@lmco.com
 * 
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class TestFederation extends AbstractIntegrationTest {

    private static XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(TestFederation.class));

    private static final String SAMPLE_DATA = "sample data";

    private static final int XML_RECORD_INDEX = 1;

    private static final int GEOJSON_RECORD_INDEX = 0;

    /*
     * Temporary fix - Added separate ports to not clash with other distros
     * running.
     */
    protected static final String HTTP_PORT = "9082";

    protected static final String HTTPS_PORT = "9994";

    protected static final String SSH_PORT = "9102";

    protected static final String RMI_SERVER_PORT = "44446";

    protected static final String RMI_REG_PORT = "1101";

    /* ************************ */

    private static final String DEFAULT_KEYWORD = "text";

    private static final String RECORD_TITLE_1 = "myTitle";

    private static final String RECORD_TITLE_2 = "myXmlTitle";

    private static final String SOLR_CONFIG_PID = "ddf.catalog.source.solr.SolrCatalogProvider";

    private static final String CATALOG_SYMBOLIC_NAME_PREFIX = "catalog-";

    private static final String SERVICE_ROOT = "http://localhost:" + HTTP_PORT
            + "/services";

    private static final String REST_PATH = SERVICE_ROOT + "/catalog/";

    private static final String OPENSEARCH_PATH = REST_PATH + "query/";

    private static final String REMOTE_SITE_NAME = "remoteSite";

    /*
     * The fields must be static if they are purposely used across all test
     * methods.
     */
    private static boolean ranBefore = false;

    private static String[] metacardIds = new String[2];

    private static FederatedSource source;

    /**
     * Configures the pax exam test container
     * 
     * @return list of pax exam options
     */
    @Override
    @org.ops4j.pax.exam.junit.Configuration
    public Option[] config() {
        // @formatter:off
        return options(
                getPlatformOption(Platform.WINDOWS),
                getPlatformOption(Platform.NIX),
                logLevel(LogLevel.INFO),
//                KarafDistributionOption.keepRuntimeFolder(),
                mavenBundle("junit", "junit", "4.10"),
                mavenBundle("org.apache.httpcomponents", "httpcore-osgi",
                        "4.2.1"),
                mavenBundle("org.apache.httpcomponents", "httpclient-osgi",
                        "4.2.1"),
                editConfigurationFilePut("etc/org.apache.karaf.shell.cfg",
                        "sshPort", SSH_PORT),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                        "org.osgi.service.http.port", HTTP_PORT),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                        "org.osgi.service.http.port.secure", HTTPS_PORT),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg",
                        "rmiRegistryPort", RMI_REG_PORT),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg",
                        "rmiServerPort", RMI_SERVER_PORT));
        // @formatter:on
    }

    /**
     * Runs each time before each test, items that don't need to be run each
     * time have a conditional flag.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Before
    public void beforeTest() throws InterruptedException, IOException {

        if (!ranBefore) {
            LOGGER.info("Running one-time only setup.");
            setLogLevels();
            waitForRequiredBundles(CATALOG_SYMBOLIC_NAME_PREFIX);
            this.catalogProvider = waitForCatalogProviderToBeAvailable();
            setSolrSoftCommit();

            FederatedSourceProperties sourceProperties = new FederatedSourceProperties();

            createManagedService(
                    FederatedSourceProperties.FACTORY_PID,
                    sourceProperties.createDefaultProperties(
                            String.valueOf(HTTP_PORT), REMOTE_SITE_NAME), 1000);

            this.source = waitForFederatedSource(5000);
            File file = new File("sample.txt");
            file.createNewFile();
            FileUtils.write(file, SAMPLE_DATA);
            String fileLocation = file.toURI().toURL().toString();
            metacardIds[GEOJSON_RECORD_INDEX] = ingest(
                    Library.getSimpleGeoJson(), "application/json");

            LOGGER.debug("File Location: {}", fileLocation);
            metacardIds[XML_RECORD_INDEX] = ingest(
                    Library.getSimpleXml(fileLocation), "text/xml");
            ranBefore = true;
        }

    }

    /**
     * Given what was ingested in beforeTest(), tests that a Federated wildcard
     * search will return all appropriate record(s).
     * 
     * @throws Exception
     */
    @Test
    public void testFederatedQueryByWildCardSearchPhrase() throws Exception {

        // given

        // when
        String queryUrl = OPENSEARCH_PATH + "?q=*&format=xml&src="
                + REMOTE_SITE_NAME;

        String result = read(queryUrl);

        // then
        assertNotNull(result);
        assertTrue("Record should include the first record title.",
                result.contains(RECORD_TITLE_1));
        assertTrue("Record should include the second record title.",
                result.contains(RECORD_TITLE_2));

    }

    /**
     * Given what was ingested in beforeTest(), tests that a Federated search
     * phrase will return the appropriate record(s).
     * 
     * @throws Exception
     */
    @Test
    public void testFederatedQueryBySearchPhrase() throws Exception {

        // given

        // when
        String queryUrl = OPENSEARCH_PATH + "?q=" + DEFAULT_KEYWORD
                + "&format=xml&src=" + REMOTE_SITE_NAME;

        String result = read(queryUrl);

        // then
        assertNotNull(result);
        assertTrue("Record should include the first record title.",
                result.contains(RECORD_TITLE_1));
        assertTrue("Record should include the second record title.",
                result.contains(RECORD_TITLE_2));

    }

    /**
     * Tests that given a bad test phrase, no records should have been returned.
     * 
     * @throws Exception
     */
    @Test
    public void testFederatedQueryByNegativeSearchPhrase() throws Exception {

        // given

        // when
        String negativeSearchPhrase = "negative";
        String queryUrl = OPENSEARCH_PATH + "?q=" + negativeSearchPhrase
                + "&format=xml&src=" + REMOTE_SITE_NAME;
        String result = read(queryUrl);

        // then
        assertNotNull(result);
        assertTrue("No records should have been returned.",
                !result.contains(RECORD_TITLE_1));

    }

    /**
     * Tests that a federated search by ID will return the right record.
     * 
     * @throws Exception
     */
    @Test
    public void testFederatedQueryById() throws Exception {

        // given

        // when
        String restUrl = REST_PATH + "sources/" + REMOTE_SITE_NAME + "/"
                + metacardIds[GEOJSON_RECORD_INDEX];

        String result = read(restUrl);

        // then
        assertNotNull(result);
        LOGGER.debug("testFederatedQueryById result\n" + result);
        assertTrue("Record should include the right title.",
                result.contains(RECORD_TITLE_1));

    }

    /**
     * Tests Source can retrieve product existing product.
     * 
     * @throws Exception
     */
    @Test
    public void testFederatedRetrieveExistingProduct() throws Exception {

        // given
        Map<String, Serializable> requestProperties = new Hashtable<String, Serializable>();
        requestProperties.put(Metacard.ID, metacardIds[XML_RECORD_INDEX]);

        // when
        ResourceResponse response = source.retrieveResource(null,
                requestProperties);

        // then
        String mimeTypeValue = response.getResource().getMimeTypeValue();
        LOGGER.info("MimeType returned [{}]", mimeTypeValue);
        assertEquals("text/plain", mimeTypeValue);
        assertEquals(SAMPLE_DATA,
                IOUtils.toString(response.getResource().getInputStream()));
    }

    /**
     * Tests Source can retrieve nonexistent product.
     * 
     * @throws Exception
     */
    @Test(expected = ResourceNotFoundException.class)
    public void testFederatedRetrieveNoProduct() throws Exception {

        // given

        Map<String, Serializable> requestProperties = new Hashtable<String, Serializable>();
        requestProperties.put(Metacard.ID, metacardIds[GEOJSON_RECORD_INDEX]);

        // when
        source.retrieveResource(null, requestProperties);

        // then
        // exception thrown, see expected
    }

    private FederatedSource waitForFederatedSource(long timeout)
            throws InterruptedException {
        ServiceTracker st = new ServiceTracker(bundleCtx,
                FederatedSource.class.getName(), null);

        st.open();

        FederatedSource source = (FederatedSource) st.waitForService(timeout);

        long millisWaited = 0;

        boolean available = source.isAvailable();
        while (!available && millisWaited < timeout) {
            Thread.sleep(100);
            available = source.isAvailable();
        }

        if (!available) {
            fail("Federated Source was not created in a timely manner.");
        }

        return source;
    }

    private String read(String getUrl) throws IOException,
            ClientProtocolException {
        DefaultHttpClient httpclient = new DefaultHttpClient();

        HttpGet getCall = new HttpGet(getUrl);

        HttpResponse response = httpclient.execute(getCall);

        String result = IOUtils.toString(response.getEntity().getContent());
        return result;
    }

    private String ingest(String data, String mimeType)
            throws UnsupportedEncodingException, IOException,
            ClientProtocolException {

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost postCall = new HttpPost(REST_PATH);
        postCall.setEntity(new StringEntity(data));
        postCall.setHeader(HTTP.CONTENT_TYPE, mimeType);
        HttpResponse postResponse = httpclient.execute(postCall);
        EntityUtils.consume(postResponse.getEntity());
        String id = postResponse.getFirstHeader("id").getValue();
        assertTrue("Ingest failed, Header not returned.", id != null);
        return id;

    }

    private void setSolrSoftCommit() throws IOException {
        Configuration solrConfig = configAdmin.getConfiguration(
                SOLR_CONFIG_PID, null);
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("forceAutoCommit", "true");
        solrConfig.update(properties);
    }

    public class FederatedSourceProperties extends Hashtable<String, Object> {

        public static final String FACTORY_PID = "OpenSearchSource";

        public Dictionary<String, Object> createDefaultProperties(String port,
                String remoteSiteName) {

            this.put(
                    "endpointUrl",
                    "http://localhost:"
                            + port
                            + "/services/catalog/query?q={searchTerms}&src={fs:routeTo?}&mr={fs:maxResults?}&count={count?}&mt={fs:maxTimeout?}&dn={idn:userDN?}&lat={geo:lat?}&lon={geo:lon?}&radius={geo:radius?}&bbox={geo:box?}&polygon={geo:polygon?}&dtstart={time:start?}&dtend={time:end?}&dateName={cat:dateName?}&filter={fsa:filter?}&sort={fsa:sort?}");
            this.put("localQueryOnly", "true");
            this.put("shouldConvertToBBox", "true");
            this.put("trustStoreLocation", "trustStore.jks");
            this.put("trustStorePassword", "password");
            this.put("keyStoreLocation", "keyStore.jks");
            this.put("keyStorePassword", "password");
            this.put("shortname", remoteSiteName);

            return this;
        }

    }

}
