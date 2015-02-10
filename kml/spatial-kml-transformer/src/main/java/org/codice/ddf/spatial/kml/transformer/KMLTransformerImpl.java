/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.spatial.kml.transformer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.security.auth.Subject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import com.github.jknack.handlebars.Context;
import ddf.action.ActionProvider;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;
import de.micromata.opengis.kml.v_2_2_0.TimeSpan;

/**
 * The base Transformer for handling KML requests to take a {@link Metacard} or
 * {@link SourceResponse} and produce a KML representation. This service attempts to first locate a
 * {@link KMLEntryTransformer} for a given {@link Metacard} based on the metadata-content-type. If
 * no {@link KMLEntryTransformer} can be found, the default transformation is performed.
 * 
 * @author Ashraf Barakat, Ian Barnett, Keith C Wire
 * 
 */
public class KMLTransformerImpl implements KMLTransformer {

    private static final String UTF_8 = "UTF-8";

    private static final String KML_RESPONSE_QUEUE_PREFIX = "Results (";

    private static final String SERVICES_REST = "/services/catalog/";
    
    protected static MimeType KML_MIMETYPE;

    private static final String CLOSE_PARENTHESIS = ")";

    private static final String TEMPLATE_DIRECTORY = "/templates";

    private static final String TEMPLATE_SUFFIX = ".hbt";

    private static final String DESCRIPTION_TEMPLATE = "description";

    protected BundleContext context;

    private static List<StyleSelector> defaultStyle = new ArrayList<StyleSelector>();

    private JAXBContext jaxbContext;

    private static final Logger LOGGER = LoggerFactory.getLogger(KMLTransformerImpl.class);

    private ClassPathTemplateLoader templateLoader;

    private Map<String, String> platformConfiguration;

    private KmlStyleMap styleMapper;

    private DescriptionTemplateHelper templateHelper;

    static {
        try {
            KML_MIMETYPE = new MimeType("application/vnd.google-earth.kml+xml");
        } catch (MimeTypeParseException e) {
            LOGGER.warn("Unable to parse KML MimeType.", e);
        }
    }

    public KMLTransformerImpl(BundleContext bundleContext, String defaultStylingName,
            KmlStyleMap mapper, ActionProvider actionProvider) {
        this.context = bundleContext;
        this.styleMapper = mapper;
        this.templateHelper = new DescriptionTemplateHelper(actionProvider);

        URL stylingUrl = context.getBundle().getResource(defaultStylingName);

        Unmarshaller unmarshaller = null;
        try {
            this.jaxbContext = JAXBContext.newInstance(Kml.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            LOGGER.error("Unable to create JAXB Context.  Setting to null.");
            this.jaxbContext = null;
        }

        try {
            if (unmarshaller != null) {
                LOGGER.debug("Reading in KML Style");
                JAXBElement<Kml> jaxbKmlStyle = unmarshaller.unmarshal(
                        new StreamSource(
                        stylingUrl.openStream()), Kml.class);
                Kml kml = jaxbKmlStyle.getValue();
                if (kml.getFeature() != null) {
                    defaultStyle = kml.getFeature().getStyleSelector();
                }
            }
        } catch (JAXBException e) {
            LOGGER.warn("Exception while unmarshalling default style resource.", e);
        } catch (IOException e) {
            LOGGER.warn("Exception while opening default style resource.", e);
        }

        templateLoader = new ClassPathTemplateLoader();
        templateLoader.setPrefix(TEMPLATE_DIRECTORY);
        templateLoader.setSuffix(TEMPLATE_SUFFIX);
    }

    /**
     * This will return a KML Placemark (i.e. there are no kml tags)
     * {@code 
     * <KML>        ---> not included
     * <Placemark>   ---> What is returned from this method
     * ...          ---> What is returned from this method 
     * </Placemark>  ---> What is returned from this method
     * </KML>       ---> not included
     * }
     * 
     * @param user
     * @param entry
     *            - the {@link Metacard} to be transformed
     * @param arguments
     *            - additional arguments to assist in the transformation
     * @return Placemark - kml object containing transformed content
     * 
     * @throws CatalogTransformerException
     */
    @Override
    public Placemark transformEntry(Subject user, Metacard entry,
            Map<String, Serializable> arguments) throws CatalogTransformerException {
        String urlToMetacard = null;

        if (arguments == null) {
            arguments = new HashMap<String, Serializable>();
        }

        String incomingRestUriAbsolutePathString = (String) arguments.get("url");
        if (incomingRestUriAbsolutePathString != null) {
            try {
                URI incomingRestUri = new URI(incomingRestUriAbsolutePathString);
                URI officialRestUri = new URI(incomingRestUri.getScheme(), null,
                        incomingRestUri.getHost(), incomingRestUri.getPort(), SERVICES_REST + "/"
                                + entry.getId(), null, null);
                urlToMetacard = officialRestUri.toString();
            } catch (URISyntaxException e) {
                LOGGER.info("bad url passed in, using request url for kml href.", e);
                urlToMetacard = incomingRestUriAbsolutePathString;
            }
            LOGGER.debug("REST URL: " + urlToMetacard);
        }

        return performDefaultTransformation(entry, incomingRestUriAbsolutePathString);
    }

    /**
     * The default Transformation from a {@link Metacard} to a KML {@link Placemark}. Protected to
     * easily allow other default transformations.
     * 
     * @param entry
     *            - the {@link Metacard} to transform.
     * @param urlToMetacard
     * @return
     * @throws TransformerException
     */
    protected Placemark performDefaultTransformation(Metacard entry, String url)
        throws CatalogTransformerException {
        
        // wrap metacard to work around classLoader/reflection issues
        entry = new MetacardImpl(entry);
        Placemark kmlPlacemark = KmlFactory.createPlacemark();
        kmlPlacemark.setId("Placemark-" + entry.getId());
        kmlPlacemark.setName(entry.getTitle());

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String effectiveTime = null;
        if (entry.getEffectiveDate() == null) {
            effectiveTime = dateFormat.format(new Date());
        } else {
            effectiveTime = dateFormat.format(entry.getEffectiveDate());
        }
        TimeSpan timeSpan = KmlFactory.createTimeSpan();
        timeSpan.setBegin(effectiveTime);
        kmlPlacemark.setTimePrimitive(timeSpan);

        kmlPlacemark.setGeometry(getKmlGeoFromWkt(entry.getLocation()));

        String description = entry.getTitle();
        Handlebars handlebars = new Handlebars(templateLoader);
        handlebars.registerHelpers(templateHelper);
        try {
            Template template = handlebars.compile(DESCRIPTION_TEMPLATE);
            description = template.apply(new HandlebarsMetacard(entry));
            LOGGER.debug(description);
            
        } catch (IOException e) {
            LOGGER.error("Failed to apply description Template", e);
        }
        kmlPlacemark.setDescription(description);

        String styleUrl = styleMapper.getStyleForMetacard(entry);
        if (StringUtils.isNotBlank(styleUrl)) {
            kmlPlacemark.setStyleUrl(styleUrl);
        }

        return kmlPlacemark;
    }

    private Geometry getKmlGeoFromWkt(final String wkt) throws CatalogTransformerException {
        if (StringUtils.isBlank(wkt)) {
            throw new CatalogTransformerException(
                    "WKT was null or empty. Unable to preform KML Transform on Metacard.");
        }

        com.vividsolutions.jts.geom.Geometry geo = readGeoFromWkt(wkt);
        Geometry kmlGeo = createKmlGeo(geo);
        if (!Point.class.getSimpleName().equals(geo.getGeometryType())) {
            kmlGeo = addPointToKmlGeo(kmlGeo, geo.getCoordinate());
        }
        return kmlGeo;
    }
    
    private Geometry createKmlGeo(com.vividsolutions.jts.geom.Geometry geo)
        throws CatalogTransformerException {
        Geometry kmlGeo = null;
        if (Point.class.getSimpleName().equals(geo.getGeometryType())) {
            Point jtsPoint = (Point) geo;
            kmlGeo = KmlFactory.createPoint().addToCoordinates(jtsPoint.getX(), jtsPoint.getY());

        } else if (LineString.class.getSimpleName().equals(geo.getGeometryType())) {
            LineString jtsLS = (LineString) geo;
            de.micromata.opengis.kml.v_2_2_0.LineString kmlLS = KmlFactory.createLineString();
            List<Coordinate> kmlCoords = kmlLS.createAndSetCoordinates();
            for (com.vividsolutions.jts.geom.Coordinate coord : jtsLS.getCoordinates()) {
                kmlCoords.add(new Coordinate(coord.x, coord.y));
            }
            kmlGeo = kmlLS;
        } else if (Polygon.class.getSimpleName().equals(geo.getGeometryType())) {
            Polygon jtsPoly = (Polygon) geo;
            de.micromata.opengis.kml.v_2_2_0.Polygon kmlPoly = KmlFactory.createPolygon();
            List<Coordinate> kmlCoords = kmlPoly.createAndSetOuterBoundaryIs()
                    .createAndSetLinearRing().createAndSetCoordinates();
            for (com.vividsolutions.jts.geom.Coordinate coord : jtsPoly.getCoordinates()) {
                kmlCoords.add(new Coordinate(coord.x, coord.y));
            }
            kmlGeo = kmlPoly;
        } else if (geo instanceof GeometryCollection) {
            List<Geometry> geos = new ArrayList<Geometry>();
            for (int xx = 0; xx < geo.getNumGeometries(); xx++) {
                geos.add(createKmlGeo(geo.getGeometryN(xx)));
            }
            kmlGeo = KmlFactory.createMultiGeometry().withGeometry(geos);
        } else {
            throw new CatalogTransformerException("Unknown / Unsupported Geometry Type '"
                    + geo.getGeometryType() + "'. Unale to preform KML Transform.");
        }
        return kmlGeo;
    }

    private com.vividsolutions.jts.geom.Geometry readGeoFromWkt(final String wkt)
        throws CatalogTransformerException {
        WKTReader reader = new WKTReader();
        try {
            return reader.read(wkt);
        } catch (ParseException e) {
            throw new CatalogTransformerException("Unable to parse WKT to Geometry.", e);

        }
    }

    private Geometry addPointToKmlGeo(Geometry kmlGeo, com.vividsolutions.jts.geom.Coordinate vertex) {
        if(null != vertex) {
            de.micromata.opengis.kml.v_2_2_0.Point kmlPoint = KmlFactory.createPoint()
                    .addToCoordinates(vertex.x, vertex.y);
            return KmlFactory.createMultiGeometry().addToGeometry(kmlPoint).addToGeometry(kmlGeo);
        } else {
            return null;
        }
    }

    @Override
    public void configurationUpdateCallback(Map<String, String> configuration) {
        platformConfiguration = configuration;
    }

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
        throws CatalogTransformerException {
        try {
            Placemark placemark = transformEntry(null, metacard, arguments);
            if (placemark.getStyleSelector().isEmpty()
                    && StringUtils.isBlank(placemark.getStyleUrl())) {
                placemark.getStyleSelector().addAll(defaultStyle);
            }
            Kml kml = KmlFactory.createKml().withFeature(placemark);

            String transformedKmlString = marshalKml(kml);

            // logger.debug("transformed kml metacard: " + transformedKmlString);
            InputStream kmlInputStream = new ByteArrayInputStream(transformedKmlString.getBytes());

            return new BinaryContentImpl(kmlInputStream, KML_MIMETYPE);
        } catch (Exception e) {
            LOGGER.error("Error transforming metacard ({}) to KML: {}", metacard.getId(), e.getMessage());
            throw new CatalogTransformerException("Error transforming metacard to KML.", e);
        }
    }

    @Override
    public BinaryContent transform(SourceResponse upstreamResponse,
            Map<String, Serializable> arguments) throws CatalogTransformerException {
        LOGGER.trace("ENTERING: ResponseQueue transform");
        if (arguments == null) {
            LOGGER.debug("Null arguments, unable to complete transform");
            throw new CatalogTransformerException("Unable to complete transform without arguments");
        }
        String docId = UUID.randomUUID().toString();

        String restUriAbsolutePath = (String) arguments.get("url");
        LOGGER.debug("rest string url arg: " + restUriAbsolutePath);

        // Transform Metacards to KML
        Document kmlDoc = KmlFactory.createDocument();
        boolean needDefaultStyle = false;
        for (Result result : upstreamResponse.getResults()) {
        	try {
        		Placemark placemark = transformEntry(null, result.getMetacard(), arguments);
                if (placemark.getStyleSelector().isEmpty()
                        && StringUtils.isEmpty(placemark.getStyleUrl())) {
                    placemark.setStyleUrl("#default");
                    needDefaultStyle = true;
                }
                kmlDoc.getFeature().add(placemark);
        	} catch (CatalogTransformerException e) {
        		LOGGER.warn("Error transforming current metacard (" + result.getMetacard().getId()  + ") to KML and will continue with remaining query responses.", e);
        		continue;
        	}
        }
        
        if (needDefaultStyle) {
            kmlDoc.getStyleSelector().addAll(defaultStyle);
        }

        Kml kmlResult = encloseKml(kmlDoc, docId,
                KML_RESPONSE_QUEUE_PREFIX + kmlDoc.getFeature().size() + CLOSE_PARENTHESIS);

        String transformedKml = marshalKml(kmlResult);

        // logger.debug("transformed kml: " + transformedKml);

        InputStream kmlInputStream = new ByteArrayInputStream(transformedKml.getBytes());
        LOGGER.trace("EXITING: ResponseQueue transform");
        return new BinaryContentImpl(kmlInputStream, KML_MIMETYPE);
    }


    /**
     * Encapsulate the kml content (placemarks, etc.) with a style in a KML Document element If
     * either content or style are null, they will be in the resulting Document
     * 
     * @param kml
     * @param style
     * @param documentId
     *            which should be the metacard id
     * @return KML DocumentType element with style and content
     */
    public static Document encloseDoc(Placemark placemark, Style style, String documentId,
            String docName) throws IllegalArgumentException {
        Document document = KmlFactory.createDocument();
        document.setId(documentId);
        document.setOpen(true);
        document.setName(docName);

        if (style != null) {
            document.getStyleSelector().add(style);
        }
        if (placemark != null) {
            document.getFeature().add(placemark);
        }

        return document;
    }

    /**
     * Wrap KML document with the opening and closing kml tags
     * 
     * @param document
     * @param folderId
     *            which should be the subscription id if it exists
     * @return completed KML
     */
    public static Kml encloseKml(Document doc, String docId, String docName) {
        Kml kml = KmlFactory.createKml();
        if (doc != null) {
            kml.setFeature(doc);
            doc.setId(docId); // Id should be subscription id
            doc.setName(docName);
            doc.setOpen(false);
        }
        return kml;
    }

    private String marshalKml(Kml kmlResult) {

        String kmlResultString = null;
        StringWriter writer = new StringWriter();

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, UTF_8);
            marshaller.marshal(kmlResult, writer);
        } catch (JAXBException e) {
            LOGGER.warn("Failed to marshal KML: ", e);
        }

        kmlResultString = writer.toString();

        return kmlResultString;
    }
}
