package org.codice.ui.admin.sources.config;

import static java.net.HttpURLConnection.HTTP_OK;

import static org.codice.ui.admin.sources.config.SourceConfigurationHandler.CERT_STATUS.CANT_CONNECT;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandler.CERT_STATUS.UNVERIFIED_CERT;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandler.CERT_STATUS.VERIFIED_CERT;
import static org.codice.ui.admin.wizard.api.ConfigurationTestMessage.MessageType.FAILURE;
import static org.codice.ui.admin.wizard.api.ConfigurationTestMessage.MessageType.INFO;
import static org.codice.ui.admin.wizard.api.ConfigurationTestMessage.MessageType.NO_TEST_FOUND;
import static org.codice.ui.admin.wizard.api.ConfigurationTestMessage.MessageType.SUCCESS;
import static org.codice.ui.admin.wizard.api.ConfigurationTestMessage.buildMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.ConfigurationTestMessage;
import org.codice.ui.admin.wizard.config.Configurator;
import org.w3c.dom.Document;

public class SourceConfigurationHandler implements ConfigurationHandler<SourceConfiguration> {

    /*********************************************************
     * IDs for the handler itself, probes, and tests
     *********************************************************/

    public static final String SOURCE_CONFIGURATION_HANDLER_ID = "sourceConfigurationHandler";
    public static final String VALID_URL_TEST_ID = "testValidUrl";
    public static final String MANUAL_URL_TEST_ID = "testManualUrl";
    public static final String MANUAL_URL_PROBE_ID = "probeManualUrl";
    public static final String DISCOVER_SOURCES_ID = "discoverSources";


    /*********************************************************
     * Keys for configuration map
     *********************************************************/

    public static final String SOURCE_ID_KEY = "id";
    public static final String SHORTNAME_KEY = "shortname";

    public static final String CSW_URL_KEY = "cswUrl";
    public static final String ENDPOINT_URL_KEY= "endpointUrl";
    public static final String EVENT_SERVICE_KEY = "eventServiceAddress";

    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";


    /*********************************************************
     * Static name property for each supported source type
     *********************************************************/

    public static final String CSW_SPECIFICATION_DISPLAY_NAME = "CSW Specification Profile Federated Source";
    public static final String CSW_SPECIFICATION_FACTORY_PID = "Csw_Federated_Source";

    public static final String METACARD_CSW_DISPLAY_NAME = "CSW Federation Profile Source";
    public static final String METACARD_CSW_FACTORY_PID = "Csw_Federation_Profile_Source";

    public static final String GMD_CSW_DISPLAY_NAME = "GMD CSW ISO Federation Source";
    public static final String GMD_CSW_FACTORY_PID = "Gmd_Csw_Federated_Source";

    public static final String OPENSEARCH_DISPLAY_NAME = "Catalog OpenSearch Federated Source";
    public static final String OPENSEARCH_FACTORY_PID = "OpenSearchSource";

    public static final String WFS1_DISPLAY_NAME = "WFS v1.0.0 Federated Source";
    public static final String WFS1_FACTORY_PID = "Wfs_v1_0_0_Federated_Source";

    public static final String WFS2_DISPLAY_NAME = "WFS v2.0.0 Federated Source";
    public static final String WFS2_FACTORY_PID = "Wfs_v2_0_0_Federated_Source";

    /*********************************************************
     * ID properties for generic source types
     *********************************************************/

    public static final String CSW_TYPE = "CSW Source";
    public static final String WFS_TYPE = "WFS Source";
    public static final String OPENSEARCH_TYPE = "OpenSearch Source";

    public static final List<String> GENERIC_TYPES = Arrays.asList(CSW_TYPE, OPENSEARCH_TYPE, WFS_TYPE);

    /*********************************************************
     * Priority list of format strings for endpoint URLs
     *********************************************************/

    public static final List<String> CSW_SOURCE_URL_FORMATS = Arrays.asList(
            "https://%s:%d/services/csw",
            "https://%s:%d/csw",
            "http://%s:%d/services/csw",
            "http://%s:%d/csw");
    public static final List<String> OPENSEARCH_SOURCE_URL_FORMATS = Arrays.asList(
            "https://%s:%d/services/catalog/query",
            "https://%s:%d/catalog/query",
            "http://%s:%d/services/catalog/query",
            "http://%s:%d/catalog/query");
    public static final List<String> WFS_SOURCE_URL_FORMATS = Arrays.asList(
            "https://%s:%d/services/wfs",
            "https://%s:%d/wfs",
            "http://%s:%d/services/wfs",
            "http://%s:%d/wfs");

    // Other connection parameters
    public static final String CSW_GET_CAPABILITIES_PARAMS = "?service=CSW&request=GetCapabilities";
    public static final String WFS_GET_CAPABILITIES_PARAMS = "?service=WFS&request=GetCapabilities";
    public static final int PING_TIMEOUT = 5000;

    /*********************************************************
     * Map of generic types to list of possible URL formats
     *********************************************************/

    public static final Map<String, List<String>> TYPE_URLS = Collections.unmodifiableMap(
            new HashMap<String, List<String>>() {{
                put(CSW_TYPE, CSW_SOURCE_URL_FORMATS);
                put(OPENSEARCH_TYPE, OPENSEARCH_SOURCE_URL_FORMATS);
                put(WFS_TYPE, WFS_SOURCE_URL_FORMATS);
            }}
    );

    /*********************************************************
     * Enum for status of certificates from sources
     *********************************************************/

    enum CERT_STATUS {VERIFIED_CERT, UNVERIFIED_CERT, CANT_CONNECT}

    /*********************************************************
     * NamespaceContext for Xpath queries
     *********************************************************/
    public static NamespaceContext owsNamespaceContext = new NamespaceContext() {
        @Override
        public String getNamespaceURI(String prefix) {
            return prefix.equals("ows") ? "http://www.opengis.net/ows" : null;
        }
        @Override
        public String getPrefix(String namespaceURI) {return null;}
        @Override
        public Iterator getPrefixes(String namespaceURI) {return null;}
    };

    /*********************************************************
     * ConfigurationHandler methods (This is the good stuff)
     *********************************************************/

    @Override
    public List<ConfigurationTestMessage> test(String testId, SourceConfiguration config) {
        switch(testId){
        case VALID_URL_TEST_ID:
            return validUrlTest(config);
        case MANUAL_URL_TEST_ID:
            return manualUrlTest(config);
        default:
            return Arrays.asList(buildMessage(NO_TEST_FOUND));
        }
    }

    public SourceConfiguration probe(String probeId, SourceConfiguration config){
        switch(probeId){
        case DISCOVER_SOURCES_ID:
            return availableSources(config);
        case MANUAL_URL_PROBE_ID:
            return config.selectedSource(getPreferredConfig(
                    config.sourceManualUrl(),
                    config.sourceManualUrlType(),
                    null)
            );
        default:
            return null;
        }
    }

    public List<ConfigurationTestMessage> persist(SourceConfiguration config) {
        Configurator configurator = new Configurator();
        SourceInfo sourceToAdd = config.selectedSource();
        String sourceConfigType = sourceToAdd.getSourceType();
        String factoryPid;
        Map<String, String> configMap = new HashMap<>();

        // Each source type has different configuration values to set
        switch(sourceConfigType) {
        case METACARD_CSW_DISPLAY_NAME:
            factoryPid = METACARD_CSW_FACTORY_PID;
            configMap.put(SOURCE_ID_KEY, config.sourceName());
            configMap.put(CSW_URL_KEY, sourceToAdd.getUrl());
            configMap.put(EVENT_SERVICE_KEY, sourceToAdd.getUrl() + "/subscription");
            if(config.sourcesUsername() != null) configMap.put(USERNAME_KEY, config.sourcesUsername());
            if(config.sourcesPassword() != null) configMap.put(PASSWORD_KEY, config.sourcesPassword());
            break;

        case GMD_CSW_DISPLAY_NAME:
            factoryPid = GMD_CSW_FACTORY_PID;
            configMap.put(SOURCE_ID_KEY, config.sourceName());
            configMap.put(CSW_URL_KEY, sourceToAdd.getUrl());
            configMap.put(EVENT_SERVICE_KEY, sourceToAdd.getUrl() + "/subscription");
            if(config.sourcesUsername() != null) configMap.put(USERNAME_KEY, config.sourcesUsername());
            if(config.sourcesPassword() != null) configMap.put(PASSWORD_KEY, config.sourcesPassword());
            break;

        case CSW_SPECIFICATION_DISPLAY_NAME:
            factoryPid = CSW_SPECIFICATION_FACTORY_PID;
            configMap.put(SOURCE_ID_KEY, config.sourceName());
            configMap.put(CSW_URL_KEY, sourceToAdd.getUrl());
            configMap.put(EVENT_SERVICE_KEY, sourceToAdd.getUrl() + "/subscription");
            if(config.sourcesUsername() != null) configMap.put(USERNAME_KEY, config.sourcesUsername());
            if(config.sourcesPassword() != null) configMap.put(PASSWORD_KEY, config.sourcesPassword());
            break;

        case OPENSEARCH_DISPLAY_NAME:
            factoryPid = OPENSEARCH_FACTORY_PID;
            configMap.put(SHORTNAME_KEY, config.sourceName());
            configMap.put(ENDPOINT_URL_KEY, sourceToAdd.getUrl());
            if(config.sourcesUsername() != null) configMap.put(USERNAME_KEY, config.sourcesUsername());
            if(config.sourcesPassword() != null) configMap.put(PASSWORD_KEY, config.sourcesPassword());
            break;

        case WFS1_DISPLAY_NAME:
            factoryPid = WFS1_FACTORY_PID;
            break;
        case WFS2_DISPLAY_NAME:
            factoryPid = WFS2_FACTORY_PID;
            break;
        default:
            return Arrays.asList(new ConfigurationTestMessage("Failed to save source.", FAILURE));
        }

        configurator.createManagedService(factoryPid, configMap);
        configurator.commit();
        return new ArrayList<>();
    }

    @Override
    public String getConfigurationHandlerId() {
        return SOURCE_CONFIGURATION_HANDLER_ID;
    }

    /*********************************************************
     * Helper methods
     *********************************************************/

    private List<ConfigurationTestMessage> validUrlTest(SourceConfiguration config) {
        try(Socket connection = new Socket()) {
            connection.connect(new InetSocketAddress(config.sourceHostname(), config.sourcePort()), PING_TIMEOUT);
            connection.close();
            return new ArrayList<>();
        } catch(IOException e){
            return Arrays.asList(buildMessage(FAILURE, "Unable to reach specified hostname and port."));
        }
    }

    private List<ConfigurationTestMessage> manualUrlTest(SourceConfiguration config) {
        List<ConfigurationTestMessage> results =  new ArrayList<>();
        // Attempt to connect to the URL and describe possible failures
        if(config.sourceManualUrl() == null){
            results.add(buildMessage(FAILURE, "No URL value provided for test."));
            return results;
        }
        try {
            URLConnection urlConnection = (new URL(config.sourceManualUrl())).openConnection();
            urlConnection.setConnectTimeout(PING_TIMEOUT);
            urlConnection.connect();
        } catch (MalformedURLException e) {
            results.add(buildMessage(FAILURE, "Specified URL is improperly formatted."));
            return results;
        } catch (SSLHandshakeException e) {
            results.add(buildMessage(FAILURE, "Specified URL's SSL certificates are invalid."));
            return results;
        } catch (IOException e) {
            results.add(buildMessage(FAILURE, "Unable to reach specified URL."));
            return results;
        }
        // Determine available source and cert status of entered URL
        CERT_STATUS status = connectionStatus(config.sourceManualUrl(), config.sourceManualUrlType());
        if(status == VERIFIED_CERT)
            results.add(buildMessage(SUCCESS, "Specified URL has been verified."));
        else if (status == UNVERIFIED_CERT){
            results.add(buildMessage(INFO, "Specified URL's certificate is untrusted and possibly insecure."));
        }
        else results.add(buildMessage(INFO, "Specified URL could not be verified as a functional source."));
        return results;
    }

    private SourceConfiguration availableSources(SourceConfiguration config) {
        String hostname = config.sourceHostname();
        int port = config.sourcePort();
        Map<String, String> urls = new HashMap<>();
        Map<String, CERT_STATUS> certValidations = new HashMap<>();

        for(Map.Entry type : TYPE_URLS.entrySet()) {
            for(String formatUrl : (List<String>) type.getValue()){
                String url = String.format(formatUrl, hostname, port);
                CERT_STATUS status = connectionStatus(url, (String) type.getKey());
                if(status != CANT_CONNECT){
                    urls.put((String)type.getKey(), url);
                    certValidations.put((String)type.getKey(), status);
                    break;
                }
            }
        }

        List<SourceInfo> availableSources = urls.keySet().stream()
                .map(type -> getPreferredConfig(urls.get(type), type, certValidations.get(type)))
                .collect(Collectors.toList());

        config.sourcesDiscoveredSources(availableSources);
        return config;
    }

    private CERT_STATUS connectionStatus(String pingUrl, String type) {
        int status;
        String contentType;
        HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(PING_TIMEOUT).build()).build();

        switch(type) {
        case CSW_TYPE:
            pingUrl += CSW_GET_CAPABILITIES_PARAMS;
            break;
        case OPENSEARCH_TYPE:
            break;
        case WFS_TYPE:
            pingUrl += WFS_GET_CAPABILITIES_PARAMS;
            break;
        default:
            return CANT_CONNECT;
        }

        HttpGet request = new HttpGet(pingUrl);
        try {
            HttpResponse response = client.execute(request);
            status = response.getStatusLine().getStatusCode();
            contentType = ContentType.getOrDefault(response.getEntity()).getMimeType();
            if(status == HTTP_OK && !(!type.equals(OPENSEARCH_TYPE) && contentType.equals("text/html")))
                return VERIFIED_CERT;
        } catch (IOException e) {
            try {
                // We want to trust any root CA, but maintain all other standard SSL checks
                SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null,
                        (TrustStrategy) (chain, authType) -> true).build();
                SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(sslContext);
                client = HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(PING_TIMEOUT).build()).setSSLSocketFactory(sf).build();
                HttpResponse response = client.execute(request);
                status = response.getStatusLine().getStatusCode();
                contentType = ContentType.getOrDefault(response.getEntity()).getMimeType();
                if(status == HTTP_OK && !(!type.equals(OPENSEARCH_TYPE) && contentType.equals("text/html"))) return UNVERIFIED_CERT;
            } catch (Exception e1) {
                return CANT_CONNECT;
            }
        }
        return CANT_CONNECT;
    }

     private SourceInfo getPreferredConfig(String hostUrl, String type, CERT_STATUS certStatus){
        switch (type) {
        case CSW_TYPE:
            return new SourceInfo(getPreferredCswConfig(hostUrl), CSW_TYPE, hostUrl, certStatus);
        case OPENSEARCH_TYPE:
            return new SourceInfo(OPENSEARCH_DISPLAY_NAME, OPENSEARCH_TYPE, hostUrl, certStatus);
        case WFS_TYPE:
            return new SourceInfo(getPreferredWfsConfig(hostUrl), WFS_TYPE, hostUrl, certStatus);
        default:
            return null;
        }
    }

    private String getPreferredCswConfig(String hostUrl) {
        String hasCatalogMetacardExp = "//ows:OperationsMetadata//ows:Operation[@name='GetRecords']/ows:Parameter[@name='OutputSchema' or @name='outputSchema']/ows:Value/text()='urn:catalog:metacard'";
        String hasGmdIsoExp = "//ows:OperationsMetadata/ows:Operation[@name='GetRecords']/ows:Parameter[@name='OutputSchema' or @name='outputSchema']/ows:Value/text()='http://www.isotc211.org/2005/gmd'";
        try {
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null,
                    (TrustStrategy) (chain, authType) -> true).build();
            SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(sslContext);
            HttpClient client = HttpClientBuilder.create().setSSLSocketFactory(sf).build();
            HttpGet getCapabilitiesRequest = new HttpGet(hostUrl + CSW_GET_CAPABILITIES_PARAMS);
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(owsNamespaceContext);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document capabilitiesXml = builder.parse(client.execute(getCapabilitiesRequest).getEntity().getContent());
            if((Boolean) xpath.compile(hasCatalogMetacardExp).evaluate(capabilitiesXml, XPathConstants.BOOLEAN)) return METACARD_CSW_DISPLAY_NAME;
            else if ((Boolean) xpath.compile(hasGmdIsoExp).evaluate(capabilitiesXml, XPathConstants.BOOLEAN)) return GMD_CSW_DISPLAY_NAME;
        }
        catch (Exception e) {
            return "none";
        }
        return CSW_SPECIFICATION_DISPLAY_NAME;
    }

    private String getPreferredWfsConfig(String hostUrl) {
        String wfsVersionExp = "//ows:ServiceIdentification//ows:ServiceTypeVersion/text()";
        try {
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null,
                    (TrustStrategy) (chain, authType) -> true).build();
            SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(sslContext);
            HttpClient client = HttpClientBuilder.create().setSSLSocketFactory(sf).build();
            HttpGet getCapabilitiesRequest = new HttpGet(hostUrl + WFS_GET_CAPABILITIES_PARAMS);
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(owsNamespaceContext);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document capabilitiesXml = builder.parse(client.execute(getCapabilitiesRequest).getEntity().getContent());
            String wfsVersion = xpath.compile(wfsVersionExp).evaluate(capabilitiesXml);
            if(wfsVersion.equals("2.0.0")) return WFS2_DISPLAY_NAME;
        } catch (Exception e) {
            return "none";
        }
        return WFS1_DISPLAY_NAME;
    }

}
