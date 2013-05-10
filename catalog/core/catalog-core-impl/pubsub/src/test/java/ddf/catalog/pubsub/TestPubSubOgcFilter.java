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
package ddf.catalog.pubsub;


import static org.junit.Assert.*;

import java.util.Date;
 
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.FilterTransformer;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.spatial.BBOX;
import org.w3c.dom.Document;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;


public class TestPubSubOgcFilter
{

    static
    {
        org.apache.log4j.BasicConfigurator.configure();
    }

    private static Logger logger = Logger.getLogger(TestPubSubOgcFilter.class);

    @Test
    @Ignore
    public void testContextualEvaluate() throws TransformerException
    {
        FilterFactory filterFactory = new FilterFactoryImpl();

        Filter filter = filterFactory.like(filterFactory.literal("abcdef"), "abcdef");
        printFilter(filter);

        assertTrue(filter.evaluate(null));

        Filter filter2 = filterFactory.like(filterFactory.literal("123456"), "123456abc");
        assertFalse(filter2.evaluate(null));
    }
    
    
    @Test
    @Ignore
    public void testContextualFeatureEvaluate() throws TransformerException
    {
        SimpleFeature feature = generateSampleFeature();
        
        FilterFactory filterFactory = new FilterFactoryImpl();
        PropertyIsEqualTo filter = filterFactory.equal(filterFactory.property("name"), filterFactory.literal("FirstFeature"), true);
        printFilter(filter);
        assertTrue(filter.evaluate(feature));
        
    }


    @Test
    @Ignore
    public void testGeospatialFeatureEvaluate() throws TransformerException
    {
        SimpleFeature feature = generateSampleFeature();
        FilterFactoryImpl filterFactory = new FilterFactoryImpl();
        BBOX bboxFilter = filterFactory.bbox("geo", -114, 10, -110, 30, DefaultGeographicCRS.WGS84.toString());
        assertTrue(bboxFilter.evaluate(feature));
        
        BBOX bboxFilter1 = filterFactory.bbox("geo", -110, 10, 0, 30, DefaultGeographicCRS.WGS84.toString());
        assertFalse(bboxFilter1.evaluate(feature));
    }
    
    public void testMetacardFeatureEvaluate()
    {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setCreatedDate(new Date());
        metacard.setEffectiveDate(new Date(1323655067L));
        metacard.setExpirationDate(new Date(32533495067L));
        metacard.setLocation("POINT(-112 25)");
        metacard.setSourceId("mts_site_1");
        metacard.setTitle("Metacard Title");
        metacard.setModifiedDate(new Date(1319075867L));
        metacard.setId("ABC123");
        metacard.setContentTypeName("MetacardType");
        Feature feature = convertMetacardToFeature(metacard);
        
        assertTrue(true);  //TODO: test this feature metacard against an OGC Filter 
    }


    private Feature convertMetacardToFeature( MetacardImpl metacard )
    {
        // other available FeatureType's ComplexFeatureTypeImpl (link features),
        // FeatureTypeImpl, NonFeatureTypeProxy, SimpleFeatureTypeImpl,
        // UniqueNameFeatureTypeImpl
        final FeatureType PUB_SUB_FEATURE = generateMetacardFeatureType();
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder((SimpleFeatureType) PUB_SUB_FEATURE);
        featureBuilder.set(Metacard.TITLE, "Muppet Metacard");
        featureBuilder.set(Metacard.CONTENT_TYPE, "Talking Green Frog");
        featureBuilder.set(Metacard.CREATED, new Date());
        featureBuilder.set(Metacard.MODIFIED, new Date());
        featureBuilder.set(Metacard.EXPIRATION, new Date());
        featureBuilder.set(Metacard.EFFECTIVE, new Date());
        featureBuilder.set(Metacard.METADATA, null);
        
        com.vividsolutions.jts.geom.GeometryFactory geoFactory = JTSFactoryFinder.getGeometryFactory(null);
        com.vividsolutions.jts.geom.Point point = geoFactory.createPoint(new Coordinate(-112,28));
        featureBuilder.set(Metacard.GEOGRAPHY, point);
        return featureBuilder.buildFeature("KTF1");
        
    }


    private final FeatureType generateMetacardFeatureType()
    {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("PubSubFeature");
        
        // add properties
        b.add(Metacard.TITLE, String.class);
        b.add(Metacard.CONTENT_TYPE, String.class);
        b.add(Metacard.CREATED, Date.class);
        b.add(Metacard.MODIFIED, Date.class);
        b.add(Metacard.EXPIRATION, Date.class);
        b.add(Metacard.EFFECTIVE, Date.class);
        b.add(Metacard.METADATA, Document.class);
        
        // add geo
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add(Metacard.GEOGRAPHY, Geometry.class);
        
        return b.buildFeatureType();
    }


    private void printFilter( Filter filter ) throws TransformerException
    {
        FilterTransformer transform = new FilterTransformer();
        transform.setIndentation(2);
        logger.debug(transform.transform(filter));
    }


    private SimpleFeature generateSampleFeature()
    {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("PubSubFeature");
        
        // add properties
        b.add("name", String.class);
        b.add("classification", Integer.class);
        b.add("height", Double.class);
        
        com.vividsolutions.jts.geom.GeometryFactory geoFactory = JTSFactoryFinder.getGeometryFactory(null);
        
        // add geo
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("geo", Point.class);
        
        final SimpleFeatureType PUB_SUB_FEATURE = b.buildFeatureType();
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(PUB_SUB_FEATURE);
        featureBuilder.set("name", "FirstFeature");
        featureBuilder.set("classification", 10);
        featureBuilder.set("height", 5.8);
        com.vividsolutions.jts.geom.Point point = geoFactory.createPoint(new Coordinate(-112,28));
        featureBuilder.set("geo", point);
        SimpleFeature feature = featureBuilder.buildFeature("f1");
        
        // it looks like if I add an attribute into the feature that is of geometry type, it automatically 
        // becomes the default geo property.  If no geo is specified, getDefaultGeometryProperty returns null
        GeometryAttribute defaultGeo = feature.getDefaultGeometryProperty();
        logger.debug("geo name: " + defaultGeo.getName());
        logger.debug("geo: " + defaultGeo);
        
        return feature;
    }
}
