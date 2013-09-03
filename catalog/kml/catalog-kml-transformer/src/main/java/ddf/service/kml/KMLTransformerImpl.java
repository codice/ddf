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
package ddf.service.kml;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBResult;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Controller;
import net.sf.saxon.event.Emitter;
import net.sf.saxon.event.MessageEmitter;

import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import ddf.catalog.Constants;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.service.kml.internal.TransformedContentImpl;
import ddf.service.kml.subscription.KmlSubscription;
import ddf.service.kml.subscription.KmlUpdateDeliveryMethod;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.Link;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.RefreshMode;
import de.micromata.opengis.kml.v_2_2_0.Style;

/**
 * The base service for handling KML requests to take a metadata record and
 * output the metadata record's KML representation. This service attempts to
 * first locate a custom transformation for a given metadata records based on
 * the metadata record's content type. If no custom transformation can be found,
 * the default transformation of this class is provided to the requesting
 * process.
 * 
 * @author Ashraf Barakat, Ian Barnett
 * 
 */
public class KMLTransformerImpl implements KMLTransformer {

   private static final String UTF_8 = "UTF-8";
   private static final String DEFAULT_INTERVAL_STRING = "5.0";
   private static final String KML_FOLDER_NAME_METACARD = "DDF Metacard";
   private static final String KML_FOLDER_NAME_RESPONSE_QUEUE = "DDF Query Results";
   private static final String NETWORK_LINK_UPDATE_NAME = "Update";
   private static final String DOCUMENT_ID_POSTFIX = "-doc";
   private static final String XSL_REST_URL_PROPERTY = "resturl";
   private static final String SERVICES_REST = "/services/catalog/";
   private static final String QUALIFIER = "qualifier";
   private static final String AMPERSAND = "&";
   private static final String CONTENT_TYPE = "content-type";
   protected static final String KML_MIMETYPE = "application/vnd.google-earth.kml+xml";
   private static final String EQUALS_SIGN = "=";
   private static final String CLOSE_PARENTHESIS = ")";
   private static final String OPEN_PARENTHESIS = "(";
   private static final String KML_ENTRY_TRANSFORMER = ddf.service.kml.KMLEntryTransformer.class
         .getName();
   private static final String BBOX_QUERY_PARAM_KEY = "bbox";

   protected BundleContext context;
   private Templates templates;
   private String defaultStyling;
   private Map<String, ServiceRegistration> subscriptionMap;
   private JAXBContext jaxbContext;
   private Marshaller marshaller;
   private Unmarshaller unmarshaller;

   private static final Logger LOGGER = Logger
         .getLogger(KMLTransformerImpl.class);

   private final TransformerFactory tf;
   private static Emitter messageEmitter;

   public KMLTransformerImpl() {
      tf = TransformerFactory.newInstance(
            net.sf.saxon.TransformerFactoryImpl.class.getName(), this
                  .getClass().getClassLoader());
   }

   public KMLTransformerImpl(BundleContext context, Bundle bundle,
         String defaultTransformerName, String defaultStylingName)
         throws TransformerConfigurationException {
      this();

      this.subscriptionMap = new HashMap<String, ServiceRegistration>();
      this.context = context;

      URL xsltUrl = bundle.getResource(defaultTransformerName);

      URL stylingUrl = bundle.getResource(defaultStylingName);

      try {
         this.defaultStyling = extractStringFrom(stylingUrl);
      } catch (IOException e) {
         LOGGER.warn("Exception while extracting style from string", e);
      }

      LOGGER.debug("Resource located at " + xsltUrl);

      Source xsltSource = new StreamSource(xsltUrl.toString());

      try {
         templates = tf.newTemplates(xsltSource);
      } catch (TransformerConfigurationException tce) {
         throw new TransformerConfigurationException(
               "Could not create new templates for KMLDefaultService: "
                     + tce.getException(), tce);
      }

      try {
         this.jaxbContext = JAXBContext.newInstance(Kml.class);
         this.marshaller = jaxbContext.createMarshaller();
         this.unmarshaller = jaxbContext.createUnmarshaller();
      } catch (JAXBException e) {
         LOGGER.error("Unable to create JAXB Context and Marshaller.  Setting to null.");
         this.jaxbContext = null;
         this.marshaller = null;
         this.unmarshaller = null;
      }

      try {
         this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
               Boolean.FALSE);
         this.marshaller.setProperty(Marshaller.JAXB_ENCODING, UTF_8);
      } catch (PropertyException e) {
         LOGGER.error("Unable to set properties on JAXB Marshaller: ", e);
      }
   }

   public Transformer getTransformer() {
      Transformer transformer;

      try {
         transformer = templates.newTransformer();
      } catch (TransformerConfigurationException e) {
         LOGGER.warn("Unable to create new transformer: " + e.getException(), e);
         return null;
      }

      transformer.setOutputProperty(
            javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");

      if (transformer instanceof Controller) {
         if (messageEmitter == null) {
            messageEmitter = new MessageEmitter();
         }
         ((Controller) transformer).setMessageEmitter(messageEmitter);
      }

      return transformer;
   }

   /**
    * This will return a KML document (i.e. there are no kml tags)
    * {@code 
    * <KML>        ---> not included
    * <Document>   ---> What is returned from this method
    * ...          ---> What is returned from this method 
    * </Document>  ---> What is returned from this method
    * </KML>       ---> not included
     * }
    * 
    * @param user
    * @param entry
    * @param arguments
    * @return Document jaxb containing kml document with content and style
    * 
    * @throws PluginExecutionException
    */
   @Override
   public Document transformEntry(Subject user, Metacard entry,
         Map<String, Serializable> arguments)
         throws CatalogTransformerException {
      String urlToMetacard = null;

      if (arguments == null) {
         arguments = new HashMap<String, Serializable>();
      }

      String incomingRestUriAbsolutePathString = (String) arguments.get("url");
      if (incomingRestUriAbsolutePathString != null) {
         try {
            URI incomingRestUri = new URI(incomingRestUriAbsolutePathString);
            URI officialRestUri = new URI(incomingRestUri.getScheme(), null,
                  incomingRestUri.getHost(), incomingRestUri.getPort(),
                  SERVICES_REST + "/" + entry.getId(), null, null);
            urlToMetacard = officialRestUri.toString();
         } catch (URISyntaxException e) {
            LOGGER.info("bad url passed in, using request url for kml href.", e);
            urlToMetacard = incomingRestUriAbsolutePathString;
         }
         LOGGER.debug("REST URL: " + urlToMetacard);
      }

      String type = entry.getContentTypeName();
      String qual = "type";
      try {

         LOGGER.info("Entry with id \""
               + entry.getId()
               + "\" has come in to be transformed, search for KMLEntryTransformers that handle the given qualified content type: "
               + qual + " : " + type);

         KMLEntryTransformer kmlET = lookupTransformersForQualifiedContentType(
               qual, type);

         // add the rest url argument
         arguments.put(XSL_REST_URL_PROPERTY, urlToMetacard);

         String content = kmlET.getKMLContent(entry, arguments);
         String style = kmlET.getKMLStyle();

         if (this.unmarshaller == null) {
            throw new PluginExecutionException(
                  "Unmarshaller is null.  Cannot obtain kml content and kml style.");
         }

         JAXBElement<Placemark> kmlContentJaxb = this.unmarshaller.unmarshal(
               new StreamSource(new ByteArrayInputStream(content.getBytes())),
               Placemark.class);
         JAXBElement<Style> kmlStyleJaxb = this.unmarshaller.unmarshal(
               new StreamSource(new ByteArrayInputStream(style.getBytes())),
               Style.class);

         String docName = entry.getTitle();
         return encloseDoc(kmlContentJaxb.getValue(), kmlStyleJaxb.getValue(),
               entry.getId() + DOCUMENT_ID_POSTFIX, docName);

      } catch (Exception e) {
         LOGGER.warn("Could not transform for given content type: "
               + e.getMessage());
         try {
            LOGGER.info("No other transformer can properly perform the transformation, defaulting to common kml transformation.");

            return performDefaultTransformation(entry, urlToMetacard);
         } catch (TransformerException te) {
            LOGGER.info(
                  "Exception performing default transformation: "
                        + te.getMessage(), te);
            throw new CatalogTransformerException(e);
         } catch (Exception e1) {
            LOGGER.info(
                  "Uncaught exception performing default transformation: "
                        + e1.getMessage(), e1);
            throw new CatalogTransformerException(e);
         }

      }
   }

   protected void addNetworkLinkUpdate(Kml kmlResult, URL requestUrl,
         String subscriptionId, double refreshInterval)
         throws URISyntaxException, MalformedURLException {
      // TODO: make it so it generates the URL dynamically. Needs to include
      // query params from the original request.
      // TODO: use some sort of URI or URL builder to generate the URL to
      // handle encoding of special chars.
      String queryParams = requestUrl.getQuery();
      String[] queryParamsSplit = queryParams.split(BBOX_QUERY_PARAM_KEY + "=");

      String requestPath = requestUrl.getPath();
      String[] requestPathSplit = requestPath.split("query");
      LOGGER.debug("incoming request url for network link update: "
            + requestUrl);
      LOGGER.debug("bbox query param: "
            + queryParamsSplit[queryParamsSplit.length - 1]);
      String netLinkUpdatePath = null;

      // This is to handle the case where the path may already have a slash
      // Although some clients can handle multiple slashes (/) in the URL
      // some do not so we will make sure there are not two slashes
      if (requestPathSplit[0].endsWith("/")) {
         netLinkUpdatePath = requestPathSplit[0] + "kml" + "/" + "update";
      } else {
         netLinkUpdatePath = requestPathSplit[0] + "/" + "kml" + "/" + "update";

      }
      String netLinkUpdateQuery = "subscription=" + subscriptionId + "&"
            + BBOX_QUERY_PARAM_KEY + "="
            + queryParamsSplit[queryParamsSplit.length - 1];

      URI netLinkUpdateUri = new URI(requestUrl.getProtocol(), null,
            requestUrl.getHost(), requestUrl.getPort(), netLinkUpdatePath,
            netLinkUpdateQuery, null);
      LOGGER.debug("network link update url: " + netLinkUpdateUri.toURL());

      Folder folder = (Folder) kmlResult.getFeature();
      NetworkLink networkLink = KmlFactory.createNetworkLink();
      folder.addToFeature(networkLink);

      networkLink.setName(NETWORK_LINK_UPDATE_NAME);
      // networkLink.setVisibility(true);

      Link link = KmlFactory.createLink();
      networkLink.setLink(link);

      link.setRefreshMode(RefreshMode.ON_INTERVAL);
      link.setRefreshInterval(refreshInterval);
      link.setHref(netLinkUpdateUri.toURL().toString());
      link.setViewBoundScale(1);
   }

   private Document performDefaultTransformation(Metacard entry,
         String urlToMetacard) throws TransformerException {
      Transformer transformer = getTransformer();

      String entryDocument = entry.getMetadata();
      StringReader entryDocumentReader = new StringReader(entryDocument);

      LOGGER.debug("setting id to " + entry.getId());
      transformer.setParameter("id", entry.getId());
      transformer.setParameter("title", entry.getTitle());
      transformer.setParameter("location", entry.getLocation());
      transformer.setParameter("site", entry.getSourceId());
      transformer.setParameter("services", getServiceRefs(entry.getId()));
      LOGGER.debug("setting " + XSL_REST_URL_PROPERTY + " to " + urlToMetacard);
      transformer.setParameter(XSL_REST_URL_PROPERTY, urlToMetacard);

      Placemark kmlPlacemark = null;
      Style style = new Style();
      try {
         JAXBResult jaxbKmlContentResult = new JAXBResult(this.jaxbContext);
         LOGGER.debug("Transforming Metacard to KML.");
         transformer.transform(new StreamSource(entryDocumentReader),
               jaxbKmlContentResult);
         LOGGER.debug("getting placemark");

 
//         Geometry geometry = (Geometry) jaxbKmlContentResult.getResult();
//         kmlPlacemark = new Placemark();
//         kmlPlacemark.setGeometry(geometry);
//         kmlPlacemark.setName(entry.getTitle());
//         kmlPlacemark.setId(entry.getId());
         kmlPlacemark = (Placemark) jaxbKmlContentResult.getResult();

         if (unmarshaller != null) {
            LOGGER.debug("Reading in KML Style");
            JAXBElement<Style> jaxbKmlStyle = this.unmarshaller.unmarshal(
                  new StreamSource(new ByteArrayInputStream(this.defaultStyling
                        .getBytes())), Style.class);
            style = jaxbKmlStyle.getValue();
         }
      } catch (Exception e) {
         throw new TransformerException(
               "Exception performing default KML transform for document:\n"
                     + entryDocument + "\n", e);
      }

      String docName = entry.getTitle();

      return encloseDoc(kmlPlacemark, style, entry.getId()
            + DOCUMENT_ID_POSTFIX, docName);
   }

   private KMLEntryTransformer lookupTransformersForQualifiedContentType(
         String qualifier, String contentType)
         throws CatalogTransformerException {
      try {

         ServiceReference[] refs = context.getServiceReferences(
               KML_ENTRY_TRANSFORMER, OPEN_PARENTHESIS + AMPERSAND
                     + OPEN_PARENTHESIS + QUALIFIER + EQUALS_SIGN + qualifier
                     + CLOSE_PARENTHESIS + OPEN_PARENTHESIS + CONTENT_TYPE
                     + EQUALS_SIGN + contentType + CLOSE_PARENTHESIS
                     + CLOSE_PARENTHESIS);

         if (refs == null || refs.length == 0) {
            throw new CatalogTransformerException(
                  "No KML transformer found for " + qualifier + " : "
                        + contentType);
         } else {
            return (KMLEntryTransformer) context.getService(refs[0]);
         }
      } catch (InvalidSyntaxException e) {
         throw new IllegalArgumentException("Invalid transformer shortName");

      }
   }

   // private String getRestServiceUrl( BundleContext context )
   // {
   // StringBuilder url = new StringBuilder("");
   //
   // /*
   // * ServiceReference serviceRef =
   // * context.getServiceReference(ConfigurationAdmin.class.getName());
   // *
   // * if (null != serviceRef) { ConfigurationAdmin osgiConfigAdmin =
   // * (ConfigurationAdmin) context.getService(serviceRef);
   // *
   // * if (null != osgiConfigAdmin) { try { // Method does not return null
   // * Configuration config =
   // * osgiConfigAdmin.getConfiguration("org.ops4j.pax.web");
   // *
   // * @SuppressWarnings("unchecked") Dictionary<String, String> props =
   // * (Dictionary<String, String>) config.getProperties();
   // *
   // * if (null != props) { url.append(parseUrl((String)
   // * serviceRef.getProperty("org.osgi.service.http.secure.enabled"),
   // * (String)
   // * serviceRef.getProperty("org.ops4j.pax.web.listening.addresses"),
   // * (String) serviceRef.getProperty("org.osgi.service.http.port"))); } }
   // * catch (Exception e) { logger.debug("", e); } } }
   // */
   //
   // // TODO: use the config admin to get the /services url
   // // TODO: figure out how to get the /rest url
   //
   // // for now, hardcode it!
   // url.append(SERVICES_REST);
   //
   // /*
   // * ServiceReference[] serviceRef = null; try { serviceRef =
   // * context.getServiceReferences
   // * (null,"(org.springframework.context.service.name=endpoint-rest)"); }
   // * catch (InvalidSyntaxException e) { // TODO Auto-generated catch block
   // * e.printStackTrace(); } System.out.println("looking for serviceref");
   // *
   // * if (null != serviceRef) { System.out.println("found " +
   // * serviceRef.length + " serviceref(s)"); for (String key :
   // * serviceRef[0].getPropertyKeys()) { System.out.println(key);
   // * System.out.println(" : " + serviceRef[0].getProperty(key)); } }
   // */
   // return url.toString();
   // }

   // private String parseUrl( String cmHttpsEnabled, String cmHost, String
   // cmPort )
   // {
   //
   // String defaultScheme = "http";
   // String defaultHost = null;
   // String defaultPort = "8181";
   //
   // logger.debug("Received the following arguments:\n" + "Https Enabled: " +
   // cmHttpsEnabled + "\n" + "Host IP: "
   // + cmHost + "\n" + "Host Port: " + cmPort);
   //
   // if (Boolean.parseBoolean(cmHttpsEnabled))
   // {
   // defaultScheme = "https";
   // defaultPort = "8443";
   // }
   //
   // if (isValidArgument(cmHost) &&
   // !cmHost.matches(UNSUBSTITUTED_PROPERTY_REGEX))
   // {
   //
   // if (cmHost.contains(","))
   // {
   // defaultHost = cmHost.substring(0, cmHost.indexOf(",")).trim();
   // }
   // else
   // {
   // defaultHost = cmHost;
   // }
   //
   // }
   // else
   // {
   // defaultHost = getServerAddress();
   // }
   //
   // if (isValidArgument(cmPort) &&
   // !cmPort.matches(UNSUBSTITUTED_PROPERTY_REGEX))
   // {
   // defaultPort = cmPort;
   // }
   //
   // return (defaultScheme + "://" + defaultHost + ":" + defaultPort);
   //
   // }

   // private String getServerAddress()
   // {
   // InetAddress address = null;
   // Enumeration<NetworkInterface> netInterfaces;
   // String defaultHost = null;
   // try
   // {
   // netInterfaces = NetworkInterface.getNetworkInterfaces();
   //
   // while (null == defaultHost && netInterfaces.hasMoreElements())
   // {
   // NetworkInterface ni = netInterfaces.nextElement();
   // Enumeration<InetAddress> addresses = ni.getInetAddresses();
   //
   // while (null == defaultHost && addresses.hasMoreElements())
   // {
   // address = addresses.nextElement();
   // if (!address.isLoopbackAddress() &&
   // !address.getHostAddress().contains(":"))
   // {
   // defaultHost = address.getHostAddress();
   // }
   // }
   // }
   // }
   // catch (SocketException se)
   // {
   // logger.info("Socket Exception attempting to obtain IP address of localhost",
   // se);
   // }
   //
   // if (null == defaultHost)
   // {
   // logger.info("Unable to obtain localhost IP address, using \"localhost\"");
   // defaultHost = DEFAULT_HOST_NAME;
   // }
   //
   // return defaultHost;
   // }
   //
   // private boolean isValidArgument( String argument )
   // {
   // return (null != argument && !argument.isEmpty());
   // }

   private List<String> getServiceRefs(String id) {
      ServiceReference[] refs = null;
      try {
         refs = context.getServiceReferences(
               MetacardTransformer.class.getName(), null);
      } catch (InvalidSyntaxException e) {
         // can't happen because filter is null
      }
      List<String> serviceList = new ArrayList<String>();
      if (refs != null) {
         for (ServiceReference ref : refs) {
            if (ref != null) {
               String title = null;
               String shortName = (String) ref
                     .getProperty(Constants.SERVICE_SHORTNAME);

               if ((title = (String) ref.getProperty(Constants.SERVICE_TITLE)) == null) {
                  title = "View as " + shortName.toUpperCase();
               }

               String url = "/services/catalog/" + id + "?transform="
                     + shortName;

               // define the services
               serviceList.add(title);
               serviceList.add(url);
            }
         }
      }
      return serviceList;

   }

   /**
    * Given a URL, it will seek the contents of the URL and pass those contents
    * back as a String. Expects to be passed a URL to a file.
    * 
    * If URL is null, empty string is passed back.
    * 
    * 
    * @param url
    *           URL of where the file is.
    * @return A String representation of the file's contents
    * @throws IOException
    *            If file cannot be opened or corrupted.
    */
   public static String extractStringFrom(URL url) throws IOException {
      InputStream is = null;
      InputStreamReader isReader = null;
      BufferedReader reader = null;

      if (url != null) {
         StringBuilder builder = new StringBuilder();
         try {
            is = url.openStream();
            isReader = new InputStreamReader(is);
            reader = new BufferedReader(isReader);

            String nextLine = "";

            while ((nextLine = reader.readLine()) != null) {

               builder.append(nextLine);
            }
         } finally {
            if (reader != null) {
               reader.close();
            }
            if (isReader != null) {
               isReader.close();
            }
            if (is != null) {
               is.close();
            }
         }

         return builder.toString();
      } else {
         return "";
      }

   }

   public String getDefaultStyling() {
      return defaultStyling;
   }

   public void setDefaultStyling(String defaultStyling) {
      this.defaultStyling = defaultStyling;
   }

   @Override
   public BinaryContent transform(Metacard metacard,
         Map<String, Serializable> arguments)
         throws CatalogTransformerException {
      try {
         Document transformedKmlDocument = transformEntry(null, metacard,
               arguments);
         Kml transformedKml = encloseKml(transformedKmlDocument, null,
               KML_FOLDER_NAME_METACARD);
         String transformedKmlString = marshalKml(transformedKml);

         // logger.debug("transformed kml metacard: " + transformedKmlString);
         InputStream kmlInputStream = new ByteArrayInputStream(
               transformedKmlString.getBytes());

         return new TransformedContentImpl(kmlInputStream,
               KMLTransformerImpl.KML_MIMETYPE);
      } catch (Exception e) {
         LOGGER.error("Error transforming metacard to KML." + e.getMessage());
         throw new CatalogTransformerException(
               "Error transforming metacard to KML.", e);
      }
   }

   @Override
   public BinaryContent transform(SourceResponse upstreamResponse,
         Map<String, Serializable> arguments)
         throws CatalogTransformerException {
      LOGGER.trace("ENTERING: ResponseQueue transform");
      if (arguments == null) {
         LOGGER.debug("Null arguments, unable to complete transform");
         throw new CatalogTransformerException(
               "Unable to complete transform without arguments");
      }
      Object id = arguments.get(Constants.SUBSCRIPTION_KEY);
      String subscriptionId = null;
      if (id != null) {
         subscriptionId = (String) id;
      }
      LOGGER.debug("subscription id: " + subscriptionId);
      String restUriAbsolutePath = (String) arguments.get("url");
      LOGGER.debug("rest string url arg: " + restUriAbsolutePath);

      double intervalDouble = parseInterval(arguments);

      Kml kmlResult = encloseKml(null, null, KML_FOLDER_NAME_RESPONSE_QUEUE);
      Folder folder = (Folder) kmlResult.getFeature();
      List<Feature> folderFeatures = folder.getFeature();

      String folderId = subscriptionId;
      if (subscriptionId == null) {
         LOGGER.debug("subscription id was null, generating folder id.");
         folderId = UUID.randomUUID().toString();
      }
      folder.setId(folderId);

      List<Result> results = upstreamResponse.getResults();
      for (Result result : results) {
         Document transformedKmlDocument = transformEntry(null,
               result.getMetacard(), arguments);
         folderFeatures.add(transformedKmlDocument);
      }

      if (subscriptionId != null && !subscriptionId.isEmpty()) {
         try {
            // add network link update to results
            URL url = new URL(restUriAbsolutePath);
            // arguments.get(key)

            addNetworkLinkUpdate(kmlResult, url, subscriptionId, intervalDouble);

            ServiceRegistration svcReg = this.subscriptionMap
                  .get(subscriptionId);
            if (svcReg != null) {
               LOGGER.debug("Found existing subscription with ID: "
                     + subscriptionId
                     + " Delete existing subscription and create new one.");
               try {
                  svcReg.unregister();
               } catch (IllegalStateException e) {
                  LOGGER.info("Attempted to unregister subscription, but subscription was already unregistered.");
               }
            }

            // create and register new subscription
            LOGGER.debug("creating new kml subscription with id: "
                  + subscriptionId);
            QueryRequest queryRequest = upstreamResponse.getRequest();
            Query query = null;
            if (queryRequest != null) {
               query = queryRequest.getQuery();
            } else {
               LOGGER.warn("QueryRequest was null, unable to create query for the subscription");
            }
            LOGGER.debug("query used as the subscription: " + query);
            KmlSubscription sub = createKmlSubscription(subscriptionId, query);
            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put("subscription-id", subscriptionId);
            String className = KmlSubscription.class.getInterfaces()[0]
                  .getName();
            ServiceRegistration newSvcRegistration = this.context
                  .registerService(className, sub, properties);
            this.subscriptionMap.put(subscriptionId, newSvcRegistration);
         } catch (Exception e) {
            LOGGER.warn(
                  "Unable to obtain subscription URL, returning KML results without any updates: ",
                  e);

         }
      }

      String transformedKml;
      transformedKml = marshalKml(kmlResult);

      // logger.debug("transformed kml: " + transformedKml);

      InputStream kmlInputStream = new ByteArrayInputStream(
            transformedKml.getBytes());
      LOGGER.trace("EXITING: ResponseQueue transform");
      return new TransformedContentImpl(kmlInputStream,
            KMLTransformerImpl.KML_MIMETYPE);
   }

   /**
    * parses the interval from the arguments
    * 
    * @param arguments
    *           - the arguments that may contain the interval string
    * @return a double value for the interval
    */
   private double parseInterval(Map<String, Serializable> arguments) {

      String interval = DEFAULT_INTERVAL_STRING;
      Object intervalArg = arguments.get("interval");
      if (intervalArg != null && intervalArg instanceof String) {
         interval = (String) intervalArg;
      }

      return Double.parseDouble(interval);

   }

   private KmlSubscription createKmlSubscription(String subscriptionId,
         Query query) {
      LOGGER.trace("ENTERING: createKmlSubscription");
      LOGGER.trace("EXITING: createKmlSubscription");
      return new KmlSubscription(subscriptionId, new KmlUpdateDeliveryMethod(),
            query);
   }

   /**
    * Encapsulate the kml content (placemarks, etc.) with a style in a KML
    * Document element If either content or style are null, they will be in the
    * resulting Document
    * 
    * @param kml
    * @param style
    * @param documentId
    *           which should be the metacard id
    * @return KML DocumentType element with style and content
    */
   public static Document encloseDoc(Placemark content, Style style,
         String documentId, String docName) throws IllegalArgumentException {
      // TODO: pass in doc name too
      Document document = KmlFactory.createDocument();
      document.setId(documentId);
      document.setOpen(true);
      document.setName(docName);

      if (style != null) {
         document.getStyleSelector().add(style);
      }
      if (content != null) {
         document.getFeature().add(content);
      }

      return document;
   }

   /**
    * Wrap KML document with the opening and closing kml tags
    * 
    * @param document
    * @param folderId
    *           which should be the subscription id if it exists
    * @return completed KML
    */
   public static Kml encloseKml(Document doc, String folderId, String folderName) {
      Kml kml = KmlFactory.createKml();
      Folder folder = KmlFactory.createFolder();
      kml.setFeature(folder);

      folder.setId(folderId); // Id should be subscription id
      folder.setName(KML_FOLDER_NAME_RESPONSE_QUEUE);
      folder.setOpen(true);
      if (doc != null) {
         folder.getFeature().add(doc);
      }

      return kml;
   }

   private String marshalKml(Kml kmlResult) {

      String kmlResultString = null;
      StringWriter writer = new StringWriter();

      try {
         marshaller.marshal(kmlResult, writer);
      } catch (JAXBException e) {
         LOGGER.warn("Failed to marshal KML: ", e);
      }

      kmlResultString = writer.toString();

      return kmlResultString;
   }
}
