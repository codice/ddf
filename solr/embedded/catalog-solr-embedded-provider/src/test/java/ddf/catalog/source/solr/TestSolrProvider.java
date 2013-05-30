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
package ddf.catalog.source.solr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionContaining.hasItem;
import static org.hamcrest.number.OrderingComparisons.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparisons.lessThanOrEqualTo;
import static org.hamcrest.text.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.border.BevelBorder;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.SortByImpl;
import org.geotools.geometry.jts.spatialschema.geometry.DirectPositionImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.UomOgcMapping;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeDescriptorImpl;
import ddf.catalog.data.BasicTypes;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.ContentTypeImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.data.MetacardTypeImpl;
import ddf.catalog.data.Result;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteRequestImpl;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryRequestImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateRequestImpl;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Tests the {@link SolrCatalogProvider}.
 * <p>
 * Uses the {@link RandomBlockJUnit4ClassRunner} to run the methods randomly so
 * that the order does not matter when testing.
 * </p>
 * 
 * @author Ashraf Barakat
 * 
 */
@RunWith(RandomBlockJUnit4ClassRunner.class)
public class TestSolrProvider extends SolrProviderTestCase {
	
    private static final String SHOW_LOW_AIRPORT_POINT_WKT = "POINT (-110.00540924072266 34.265270233154297)";
    private static final String TAMPA_AIRPORT_POINT_WKT = "POINT (-82.533248901367188 27.975471496582031)";
    private static final String GULF_OF_GUINEA_POLYGON_WKT = "POLYGON ((1 1,2 1,2 2,1 2,1 1))";
    private static final String GULF_OF_GUINEA_MULTIPOLYGON_WKT = "MULTIPOLYGON (((1 1,2 1,2 2,1 2,1 1)), ((0 0,1 1,2 0,0 0)))";
    private static final String GULF_OF_GUINEA_LINESTRING_WKT = "LINESTRING (1 1,2 1)";
    private static final String GULF_OF_GUINEA_MULTILINESTRING_WKT = "MULTILINESTRING ((1 1, 2 1), (1 2, 0 0))";
    private static final String GULF_OF_GUINEA_POINT_WKT = "POINT (1 1)";
    private static final String GULF_OF_GUINEA_MULTIPOINT_WKT = "MULTIPOINT ((1 1), (0 0), (2 2))";
    private static final String GULF_OF_GUINEA_MULTIPOINT_SINGLE_WKT = "MULTIPOINT ((1 1))";
    private static final String GULF_OF_GUINEA_GEOMETRYCOLLECTION_WKT = "GEOMETRYCOLLECTION ("
            + GULF_OF_GUINEA_POINT_WKT + ", " + GULF_OF_GUINEA_LINESTRING_WKT + ", " + GULF_OF_GUINEA_MULTIPOLYGON_WKT
            + ")";
    private static final String LAS_VEGAS_POINT_WKT = "POINT (-115.136389 36.175)";
    private static final String PHOENIX_POINT_WKT = "POINT (-112.066667 33.45)";
    private static final String COUNTERCLOCKWISE_ARIZONA_RECTANGLE_WKT = "POLYGON ((-108.08349609374837 30.90222470517274, -108.08349609374837 37.45741810263027, -115.70800781249432 37.45741810263027, -115.70800781249432 30.90222470517274, -108.08349609374837 30.90222470517274))";
    private static final String CLOCKWISE_ARIZONA_RECTANGLE_WKT = "POLYGON ((-115.72998046874625 30.921076375385542, -115.72998046874625 37.47485808497204, -108.12744140624321 37.47485808497204, -108.12744140624321 30.921076375385542, -115.72998046874625 30.921076375385542))";
    private static final String ARIZONA_INTERSECTING_LINESTING_WKT = "LINESTRING (-115.33642578125 33.28662109375,-108.17333984375 35.83544921875)";
    private static final String ARIZONA_INTERSECTING_MULTILINESTING_WKT = "MULTILINESTRING ((-115.33642578125 33.28662109375,-108.17333984375 35.83544921875), (-119.15527356533 36.906984126196, -114.40917981533 39.455812251196, -117.22167981533 39.719484126196, -117.26562512783 39.719484126196))";
    private static final String FLAGSTAFF_AIRPORT_POINT_WKT = "POINT (-111.67121887207031 35.138454437255859)";
    private static final String PHOENIX_AND_LAS_VEGAS_MULTIPOINT_WKT = "MULTIPOINT ((-112.066667 33.45), (-115.136389 36.175))";
    private static final String ARIZONA_INTERSECTING_POLYGON_WKT = "POLYGON ((-116.26171901822 34.658206701279, -113.80078151822 38.261722326279, -110.15332058072 35.625003576279, -110.06542995572 33.251956701279, -113.97656276822 32.812503576279, -116.26171901822 34.658206701279))";
    private static final String ARIZONA_INTERSECTING_MULTIPOLYGON_WKT = "MULTIPOLYGON (((-116.26171901822 34.658206701279, -113.80078151822 38.261722326279, -110.15332058072 35.625003576279, -110.06542995572 33.251956701279, -113.97656276822 32.812503576279, -116.26171901822 34.658206701279)), ((-117.88085950283 35.588624751196, -117.66113294033 40.554445063696, -120.60546887783 37.654054438696, -117.88085950283 35.588624751196)))";
    private static final String ARIZONA_INTERSECTING_GEOMETRYCOLLECTION_WKT = "GEOMETRYCOLLECTION ("
            + FLAGSTAFF_AIRPORT_POINT_WKT + ", " + ARIZONA_INTERSECTING_LINESTING_WKT + ", "
            + ARIZONA_INTERSECTING_MULTIPOLYGON_WKT + ")";
    private static final String ARIZONA_POLYGON_WKT = "POLYGON ((-114.52062730304343 33.02770735822419, -114.55908930307925 33.03678235823264, -114.6099253031266 33.027002358223534, -114.63396730314898 33.03356735822965, -114.6451593031594 33.044412358239754, -114.66395130317692 33.038922358234636, -114.71135530322107 33.09538235828722, -114.70946330321931 33.122375358312354, -114.6781203031901 33.16725035835415, -114.6800513031919 33.224595358407555, -114.68771130319904 33.23925835842121, -114.67769330318971 33.268016358447994, -114.73542730324348 33.3057083584831, -114.70360330321384 33.352418358526606, -114.7249363032337 33.41105935858121, -114.64509230315934 33.419116358588724, -114.63057330314584 33.439425358607636, -114.621089303137 33.468599358634805, -114.59808630311556 33.48612735865113, -114.5870613031053 33.50944535867285, -114.52942030305162 33.56007335872, -114.5402473030617 33.58050735873903, -114.52717030304953 33.622136358777794, -114.52526330304775 33.66550435881818, -114.53643330305815 33.682735358834236, -114.49567630302019 33.70836935885811, -114.5102873030338 33.74320035889055, -114.50455830302846 33.7717143589171, -114.5211223030439 33.82603135896769, -114.51172230303513 33.84196535898253, -114.52096230304375 33.862926359002046, -114.49818830302253 33.925036359059895, -114.5256323030481 33.95241335908539, -114.51820830304118 33.96506335909717, -114.42898030295808 34.02984435915751, -114.42402930295347 34.07833235920266, -114.41016630294055 34.10265435922531, -114.32279930285918 34.1412973592613, -114.28536830282434 34.17123135928918, -114.23577630277813 34.186222359303144, -114.14991230269818 34.266979359378354, -114.12523030267519 34.272621359383606, -114.13412730268348 34.31454835942266, -114.15341530270143 34.33644735944305, -114.18208030272814 34.36520635946984, -114.2578423027987 34.40548835950735, -114.2833943028225 34.41206935951348, -114.30286530284062 34.43575435953554, -114.33263630286835 34.454873359553346, -114.3765073029092 34.45967935955782, -114.38386230291606 34.47708535957403, -114.3768283029095 34.536563359629426, -114.40974230294016 34.58372335967334, -114.43430230296303 34.59896335968754, -114.42227030295183 34.61089535969865, -114.46563730299222 34.70987335979083, -114.49780430302218 34.74475735982332, -114.52555330304801 34.74891135982719, -114.54204030306337 34.759958359837476, -114.5702173030896 34.83186035990444, -114.62726330314274 34.875533359945116, -114.63047530314574 34.919501359986064, -114.62100730313692 34.943609360008516, -114.63227630314742 34.997651360058846, -114.62106830313698 34.99891436006002, -114.63378030314881 35.041863360100024, -114.59563230311329 35.07605836013187, -114.6359093031508 35.11865536017154, -114.62644130314197 35.13390636018575, -114.58261630310116 35.132560360184485, -114.57225530309151 35.14006736019148, -114.56104030308107 35.17434636022341, -114.5595833030797 35.22018336026609, -114.58789030310608 35.304768360344866, -114.58958430310764 35.358378360394795, -114.64539630315963 35.450760360480835, -114.6722153031846 35.515754360541365, -114.64979230316374 35.54663736057013, -114.65313430316684 35.5848333606057, -114.63986630315449 35.611348360630394, -114.65406630316771 35.64658436066321, -114.66848630318114 35.65639936067235, -114.66509130317797 35.69309936070653, -114.68882030320007 35.73259536074332, -114.68273930319441 35.76470336077322, -114.68986730320105 35.84744236085027, -114.66246230317552 35.870960360872175, -114.66160030317472 35.88047336088104, -114.69927630320981 35.91161236091004, -114.7362123032442 35.987648360980856, -114.71767330322695 36.036758361026585, -114.72896630323746 36.058753361047074, -114.7281503032367 36.08596236107242, -114.71276130322238 36.10518136109032, -114.62161030313749 36.141966361124574, -114.59893530311636 36.13833536112119, -114.5305733030527 36.15509036113679, -114.46661330299312 36.1247113611085, -114.44394530297203 36.1210533611051, -114.38080330291321 36.15099136113298, -114.34423430287916 36.137480361120396, -114.31609530285294 36.11143836109614, -114.30385730284155 36.08710836107348, -114.30758730284502 36.06223336105032, -114.233472302776 36.01833136100943, -114.20676930275113 36.017255361008424, -114.12902330267872 36.04173036103122, -114.10777530265894 36.12109036110513, -114.04510530260056 36.19397836117301, -114.03739230259339 36.21602336119354, -114.04371630259928 36.84184936177639, -114.04393930259948 36.99653836192046, -112.89998330153409 36.99622736192016, -112.54252130120118 36.99799436192181, -112.23725830091688 36.995492361919474, -111.3561643000963 37.001709361925265, -110.7400632995225 37.002488361926, -110.48408929928411 37.003926361927334, -110.45223629925445 36.991746361915986, -109.99707629883055 36.99206736191629, -109.0484802979471 36.99664136192055, -109.0478462979465 35.99666436098925, -109.04664129794538 34.95464636001879, -109.04865229794726 34.59178035968085, -109.05034929794884 33.7833023589279, -109.050526297949 33.20516435838946, -109.05134629794976 32.779550357993074, -109.04949529794804 32.44204435767875, -109.04561529794442 31.34345335665561, -110.45257829925477 31.33766035665021, -111.07196429983162 31.335634356648328, -111.36952130010873 31.431531356737636, -113.32911130193375 32.04362135730769, -114.82176130332388 32.487169357720774, -114.80939430331236 32.6160443578408, -114.72204930323102 32.720857357938414, -114.71269530322232 32.7350133579516, -114.69404030320493 32.74142535795757, -114.60394230312102 32.72628535794347, -114.60352230312063 32.73588635795241, -114.57195930309123 32.73743935795386, -114.57221030309148 32.74882935796447, -114.56075130308079 32.74893635796457, -114.56158230308156 32.760753357975574, -114.54300430306427 32.76074935797557, -114.54318730306444 32.77123235798533, -114.53009530305225 32.7714113579855, -114.53507730305688 32.788047358000995, -114.52621930304863 32.80991235802135, -114.4614363029883 32.84542235805443, -114.47644430300228 32.9359083581387, -114.46838730299478 32.9777893581777, -114.52062730304343 33.02770735822419))";
    private static final String WEST_USA_CONTAINING_POLYGON_WKT = "POLYGON ((-125 49, -125 30, -100 30, -100 49, -125 49))";
	
	private static final String AIRPORT_QUERY_PHRASE = "Airport";
	private static final String TAMPA_QUERY_PHRASE = "Tampa";
	private static final String FLAGSTAFF_QUERY_PHRASE = "Flagstaff";
	private static final String PURCHASE_ORDER_QUERY_PHRASE = "Lawnmower";

	private static final String SAMPLE_CONTENT_TYPE_1 = "contentType1";
	private static final String SAMPLE_CONTENT_TYPE_2 = "contentType2";
	private static final String SAMPLE_CONTENT_TYPE_3 = "content-Type";

	private static final String SAMPLE_CONTENT_TYPE_4 = "ct1=3";
	private static final String SAMPLE_CONTENT_VERSION_1 = "version1";
	private static final String SAMPLE_CONTENT_VERSION_2 = "vers:ion2";
	private static final String SAMPLE_CONTENT_VERSION_3 = "DDFv20";
	private static final String SAMPLE_CONTENT_VERSION_4 = "vers+4";

	private static final String DEFAULT_TEST_ESCAPE = "\\";
	private static final String DEFAULT_TEST_SINGLE_WILDCARD = "?";
	private static final String DEFAULT_TEST_WILDCARD = "*";

	private static final long MINUTES_IN_MILLISECONDS = 60000;
	private static final double METERS_PER_KM = 1000.0;

	private static final Logger LOGGER = Logger.getLogger(TestSolrProvider.class);

	private static final int ONE_HIT = 1;

	/**
	 * Testing that you cannot instantiate with a null Server.
	 * 
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testSolrServerNull() {
		new SolrCatalogProvider(null, null);
	}

	/**
	 * Testing that if we create a record, it is truly ingested and we can
	 * retrieve all the fields we intend to be retrievable.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test
	public void testCreateOperation() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

		create(metacard);

		FilterFactory filterFactory = new FilterFactoryImpl();

		// SIMPLE TITLE SEARCH
		Filter filter = filterFactory.like(filterFactory.property(Metacard.TITLE), MockMetacard.DEFAULT_TITLE,
				DEFAULT_TEST_WILDCARD, DEFAULT_TEST_SINGLE_WILDCARD, DEFAULT_TEST_ESCAPE, false);

		QueryImpl query = new QueryImpl(filter);

		query.setStartIndex(1);

		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

		List<Result> results = sourceResponse.getResults();
		Metacard mResult = results.get(0).getMetacard();
		assertEquals(1, results.size());
		assertNotNull(mResult.getId());
		assertEquals(MockMetacard.DEFAULT_TITLE, mResult.getTitle());
		assertEquals(MockMetacard.DEFAULT_LOCATION, mResult.getLocation());
		assertEquals(MockMetacard.DEFAULT_TYPE, mResult.getContentTypeName());
		assertEquals(MockMetacard.DEFAULT_VERSION, mResult.getContentTypeVersion());
		assertNotNull(mResult.getMetadata());
		assertThat(mResult.getMetadata(), containsString("<title>Flagstaff Chamber of Commerce</title>"));
		assertTrue(!mResult.getMetadata().isEmpty());
		assertFalse(mResult.getCreatedDate().after(new Date()));
		assertFalse(mResult.getModifiedDate().after(new Date()));
		assertEquals(metacard.getEffectiveDate(), mResult.getEffectiveDate());
		assertEquals(metacard.getExpirationDate(), mResult.getExpirationDate());
		assertTrue(Arrays.equals(metacard.getThumbnail(), mResult.getThumbnail()));
		assertEquals(metacard.getLocation(), mResult.getLocation());
		assertEquals(MASKED_ID, mResult.getSourceId());

		// --- Simple KEYWORD SEARCH
		filter = filterFactory.like(filterFactory.property(Metacard.METADATA), MockMetacard.DEFAULT_TITLE,
				DEFAULT_TEST_WILDCARD, DEFAULT_TEST_SINGLE_WILDCARD, DEFAULT_TEST_ESCAPE, false);

		query = new QueryImpl(filter);

		query.setStartIndex(1);

		sourceResponse = provider.query(new QueryRequestImpl(query));

		results = sourceResponse.getResults();
		mResult = results.get(0).getMetacard();
		assertEquals(1, results.size());
		assertNotNull(mResult.getId());
		assertEquals(MockMetacard.DEFAULT_TITLE, mResult.getTitle());
		assertEquals(MockMetacard.DEFAULT_LOCATION, mResult.getLocation());
		assertEquals(MockMetacard.DEFAULT_TYPE, mResult.getContentTypeName());
		assertEquals(MockMetacard.DEFAULT_VERSION, mResult.getContentTypeVersion());
		assertNotNull(mResult.getMetadata());
		assertTrue(!mResult.getMetadata().isEmpty());
		assertFalse(mResult.getCreatedDate().after(new Date()));
		assertFalse(mResult.getModifiedDate().after(new Date()));
		assertEquals(metacard.getEffectiveDate(), mResult.getEffectiveDate());
		assertEquals(metacard.getExpirationDate(), mResult.getExpirationDate());
		assertTrue(Arrays.equals(metacard.getThumbnail(), mResult.getThumbnail()));
		assertEquals(metacard.getLocation(), mResult.getLocation());
		assertEquals(MASKED_ID, mResult.getSourceId());

	}
	

	@Test(expected = IngestException.class)
	public void testCreateOperationWithSourceIdNoId() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

		metacard.setSourceId("ddfChild");

		Date oneDayAgo = new DateTime().minusDays(1).toDate();
		metacard.setCreatedDate(oneDayAgo);
		metacard.setExpirationDate(oneDayAgo);
		metacard.setEffectiveDate(oneDayAgo);
		metacard.setModifiedDate(oneDayAgo);

		create(metacard);

	}

	@Test
	public void testCreateOperationWithSourceId() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

		metacard.setId("12345678900987654321abcdefgabcdefg");

		metacard.setSourceId("ddfChild");

		Date oneDayAgo = new DateTime().minusDays(1).toDate();
		metacard.setCreatedDate(oneDayAgo);
		metacard.setExpirationDate(oneDayAgo);
		metacard.setEffectiveDate(oneDayAgo);
		metacard.setModifiedDate(oneDayAgo);

		CreateResponse createResponse = create(metacard);

		Metacard createdMetacard = createResponse.getCreatedMetacards().get(0);

		assertNotNull(createdMetacard.getId());
		assertEquals(MockMetacard.DEFAULT_TITLE, createdMetacard.getTitle());
		assertEquals(MockMetacard.DEFAULT_LOCATION, createdMetacard.getLocation());
		assertEquals(MockMetacard.DEFAULT_TYPE, createdMetacard.getContentTypeName());
		assertEquals(MockMetacard.DEFAULT_VERSION, createdMetacard.getContentTypeVersion());
		assertNotNull(createdMetacard.getMetadata());
		assertThat(createdMetacard.getMetadata(), containsString("<title>Flagstaff Chamber of Commerce</title>"));
		assertThat(createdMetacard.getMetadata().isEmpty(), is(not(true)));
		assertThat(createdMetacard.getCreatedDate(), is(oneDayAgo));
		assertThat(createdMetacard.getModifiedDate(), is(oneDayAgo));
		assertThat(createdMetacard.getEffectiveDate(), is(oneDayAgo));
		assertThat(createdMetacard.getExpirationDate(), is(oneDayAgo));
		assertTrue(Arrays.equals(metacard.getThumbnail(), createdMetacard.getThumbnail()));
		assertEquals(metacard.getLocation(), createdMetacard.getLocation());
		assertThat(createdMetacard.getSourceId(), is(metacard.getSourceId()));

		// --------------------

		FilterFactory filterFactory = new FilterFactoryImpl();

		// SIMPLE TITLE SEARCH
		Filter filter = filterFactory.like(filterFactory.property(Metacard.TITLE), MockMetacard.DEFAULT_TITLE,
				DEFAULT_TEST_WILDCARD, DEFAULT_TEST_SINGLE_WILDCARD, DEFAULT_TEST_ESCAPE, false);

		QueryImpl query = new QueryImpl(filter);

		query.setStartIndex(1);

		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

		List<Result> results = sourceResponse.getResults();
		Metacard mResult = results.get(0).getMetacard();
		assertEquals(1, results.size());
		assertNotNull(mResult.getId());
		assertEquals(MockMetacard.DEFAULT_TITLE, mResult.getTitle());
		assertEquals(MockMetacard.DEFAULT_LOCATION, mResult.getLocation());
		assertEquals(MockMetacard.DEFAULT_TYPE, mResult.getContentTypeName());
		assertEquals(MockMetacard.DEFAULT_VERSION, mResult.getContentTypeVersion());
		assertNotNull(mResult.getMetadata());
		assertThat(mResult.getMetadata(), containsString("<title>Flagstaff Chamber of Commerce</title>"));
		assertThat(mResult.getMetadata().isEmpty(), is(not(true)));
		assertThat(mResult.getCreatedDate(), is(oneDayAgo));
		assertThat(mResult.getModifiedDate(), is(oneDayAgo));
		assertThat(mResult.getEffectiveDate(), is(oneDayAgo));
		assertThat(mResult.getExpirationDate(), is(oneDayAgo));
		assertTrue(Arrays.equals(metacard.getThumbnail(), mResult.getThumbnail()));
		assertEquals(metacard.getLocation(), mResult.getLocation());
		// assertThat(mResult.getSourceId(), is("ddf"));

	}

	/**
	 * Tests what happens when the whole request is null.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test(expected = IngestException.class)
	public void testCreateNull() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		provider.create(null);

		fail();

	}

	@Test
	public void testCreateNullList() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		CreateResponse response = provider.create(new CreateRequest() {

			@Override
			public boolean hasProperties() {
				return false;
			}

			@Override
			public Serializable getPropertyValue(String name) {
				return null;
			}

			@Override
			public Set<String> getPropertyNames() {
				return null;
			}

			@Override
			public Map<String, Serializable> getProperties() {
				return null;
			}

			@Override
			public boolean containsPropertyName(String name) {
				return false;
			}

			@Override
			public List<Metacard> getMetacards() {
				return null;
			}
		});

		assertThat(response.getCreatedMetacards().size(), is(0));
	}

	@Test
	public void testCreatedDates() throws Exception {

		deleteAllIn(provider);

		/* ALL NULL */
		MockMetacard mockMetacard = new MockMetacard(Library.getFlagstaffRecord());
		mockMetacard.setEffectiveDate(null);
		mockMetacard.setExpirationDate(null);
		mockMetacard.setCreatedDate(null);
		mockMetacard.setModifiedDate(null);
		List<Metacard> list = Arrays.asList((Metacard) mockMetacard);

		CreateResponse createResponse = create(list);

		assertEquals(1, createResponse.getCreatedMetacards().size());

		Metacard createdMetacard = createResponse.getCreatedMetacards().get(0);

		assertNotNull(createdMetacard.getId());
		assertEquals(MockMetacard.DEFAULT_TITLE, createdMetacard.getTitle());
		assertEquals(MockMetacard.DEFAULT_LOCATION, createdMetacard.getLocation());
		assertEquals(MockMetacard.DEFAULT_TYPE, createdMetacard.getContentTypeName());
		assertEquals(MockMetacard.DEFAULT_VERSION, createdMetacard.getContentTypeVersion());
		assertNotNull(createdMetacard.getMetadata());
		assertTrue(!createdMetacard.getMetadata().isEmpty());

		// DATES
		assertEquals(mockMetacard.getCreatedDate(), createdMetacard.getCreatedDate());
		assertThat(createdMetacard.getCreatedDate(), nullValue());

		assertEquals(mockMetacard.getModifiedDate(), createdMetacard.getModifiedDate());
		assertThat(createdMetacard.getModifiedDate(), nullValue());

		assertEquals(mockMetacard.getEffectiveDate(), createdMetacard.getEffectiveDate());
		assertThat(createdMetacard.getEffectiveDate(), nullValue());

		assertEquals(mockMetacard.getExpirationDate(), createdMetacard.getExpirationDate());
		assertThat(createdMetacard.getExpirationDate(), nullValue());

		assertTrue(Arrays.equals(mockMetacard.getThumbnail(), createdMetacard.getThumbnail()));
		assertEquals(mockMetacard.getLocation(), createdMetacard.getLocation());
		assertEquals(MASKED_ID, createdMetacard.getSourceId());
	}

	/**
	 * Testing that if records are properly updated.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test
	public void testUpdateOperationSimple() throws IngestException, UnsupportedQueryException {

		// Single Update

		deleteAllIn(provider);

		MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

		CreateResponse createResponse = create(metacard);

		String id = createResponse.getCreatedMetacards().get(0).getId();

		metacard.setContentTypeName("newContentType");

		UpdateResponse response = update(id, metacard);

		Update update = response.getUpdatedMetacards().get(0);

		Metacard newMetacard = update.getNewMetacard();

		Metacard oldMetacard = update.getOldMetacard();

		assertEquals(1, response.getUpdatedMetacards().size());

		assertEquals("newContentType", newMetacard.getContentTypeName());
		assertEquals(MockMetacard.DEFAULT_TYPE, oldMetacard.getContentTypeName());

	}

	/**
	 * Tests if a partial update is handled appropriately.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test
	public void testUpdatePartial() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

		CreateResponse createResponse = create(metacard);

		String id = createResponse.getCreatedMetacards().get(0).getId();

		metacard.setContentTypeName("newContentType");

		String[] ids = { id, "no_such_record" };
		
		UpdateResponse response = update(ids, Arrays.asList((Metacard) metacard,
						metacard));

		assertEquals(1, response.getUpdatedMetacards().size());

	}

	/**
	 * Tests what happens when the whole request is null.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test(expected = IngestException.class)
	public void testUpdateNull() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		provider.update(null);

		fail();

	}

	/**
	 * Tests null list in UpdateRequest
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test
	public void testUpdateNullList() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		UpdateResponse response = provider.update(new UpdateRequestImpl(null, Metacard.ID, null));

		assertEquals(0, response.getUpdatedMetacards().size());

	}

	/**
	 * Tests empty list in UpdateRequest
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test
	public void testUpdateEmptyList() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		UpdateResponse response = provider.update(new UpdateRequestImpl(new ArrayList<Entry<Serializable, Metacard>>(),
				Metacard.ID, null));

		assertEquals(0, response.getUpdatedMetacards().size());

	}

	@Test
	public void testUpdateByMetacardId() throws Exception {

		deleteAllIn(provider);

		MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
		MockMetacard metacard2 = new MockMetacard(Library.getShowLowRecord());

		String uri1 = "http://youwillfindme.com/here";
		String uri2 = "http://youwillfindme.com/there";

		metacard1.setResourceURI(new URI(uri1));
		metacard1.setContentTypeName("oldNitf");
		metacard2.setResourceURI(new URI(uri2));
		metacard2.setContentTypeName("oldNitf2");
		metacard2.setResourceSize("25L");

		List<Metacard> list = Arrays.asList((Metacard) metacard1, metacard2);

		CreateResponse createResponse = create(list);

		List<String> responseStrings = MockMetacard.toStringList(createResponse.getCreatedMetacards());

		assertEquals(2, responseStrings.size());

		/** UPDATE **/

		MockMetacard updatedMetacard1 = new MockMetacard(Library.getTampaRecord());
		MockMetacard updatedMetacard2 = new MockMetacard(Library.getFlagstaffRecord());

		updatedMetacard1.setId(metacard1.getId());
		updatedMetacard1.setContentTypeName("nitf");

		updatedMetacard2.setId(metacard2.getId());
		updatedMetacard2.setResourceURI(new URI(uri2));
		updatedMetacard2.setContentTypeName("nitf2");
		updatedMetacard2.setResourceSize("50L");

		list = Arrays.asList((Metacard) updatedMetacard1, updatedMetacard2);

		String[] ids = { metacard1.getId(), metacard2.getId() };

		UpdateResponse updateResponse = update(ids, list);

		assertEquals("Testing Update operation: ", 2, updateResponse.getUpdatedMetacards().size());

		List<Update> updatedMetacards = updateResponse.getUpdatedMetacards();

		for (Update up : updatedMetacards) {

			Metacard newCard = up.getNewMetacard();
			Metacard oldCard = up.getOldMetacard();

			assertNotNull(oldCard.getResourceURI());
			assertEquals(provider.getId(), oldCard.getSourceId());
			assertEquals(provider.getId(), newCard.getSourceId());

			if (oldCard.getContentTypeName().equals("oldNitf")) {

				assertEquals("nitf", newCard.getContentTypeName());

				// TPA is unique to the document
				assertTrue(newCard.getMetadata().indexOf("TPA") != ALL_RESULTS);
				assertThat(newCard.getResourceURI(), is(nullValue()));
				assertThat(oldCard.getResourceURI().toString(), equalTo(uri1));

				assertEquals(oldCard.getId(), newCard.getId());
				// Title
				assertEquals(MockMetacard.DEFAULT_TITLE, oldCard.getTitle());
				assertEquals(MockMetacard.DEFAULT_TITLE, newCard.getTitle());
				// Location (decimal points make them not exact Strings POINT(1
				// 0) as opposed to POINT( 1.0 0.0) )
				assertEquals(MockMetacard.DEFAULT_LOCATION.substring(0, 8), oldCard.getLocation().substring(0, 8));
				assertEquals(MockMetacard.DEFAULT_LOCATION.substring(0, 8), newCard.getLocation().substring(0, 8));
				// Metadata
				assertNotNull(oldCard.getMetadata());
				assertNotNull(newCard.getMetadata());
				assertTrue(!oldCard.getMetadata().isEmpty());
				assertTrue(!newCard.getMetadata().isEmpty());
				// Created Date
				assertFalse(oldCard.getCreatedDate().after(new Date()));
				assertFalse(newCard.getCreatedDate().after(new Date()));
				assertTrue(newCard.getCreatedDate().equals(oldCard.getCreatedDate()));
				// Modified Date
				assertTrue(newCard.getModifiedDate().after(oldCard.getModifiedDate()));
				// Effective Date
				assertTrue(newCard.getEffectiveDate().after(oldCard.getEffectiveDate()));
				// Expiration Date
				assertTrue(newCard.getExpirationDate().after(oldCard.getExpirationDate()));
				// Thumbnail
				assertTrue(Arrays.equals(newCard.getThumbnail(), oldCard.getThumbnail()));

			} else if (oldCard.getContentTypeName().equals("oldNitf2")) {

				assertEquals("nitf2", newCard.getContentTypeName());

				// Cardinals is unique to the document
				assertTrue(newCard.getMetadata().indexOf("Cardinals") != ALL_RESULTS);

				assertTrue("50L".equals(newCard.getResourceSize()));

				assertEquals(uri2, newCard.getResourceURI().toString());

				assertEquals(oldCard.getId(), newCard.getId());
				// Title
				assertEquals(MockMetacard.DEFAULT_TITLE, oldCard.getTitle());
				assertEquals(MockMetacard.DEFAULT_TITLE, newCard.getTitle());
				// Location (decimal points make them not exact in Strings
				assertEquals(MockMetacard.DEFAULT_LOCATION.substring(0, 8), oldCard.getLocation().substring(0, 8));
				assertEquals(MockMetacard.DEFAULT_LOCATION.substring(0, 8), newCard.getLocation().substring(0, 8));
				// Metadata
				assertNotNull(oldCard.getMetadata());
				assertNotNull(newCard.getMetadata());
				assertTrue(!oldCard.getMetadata().isEmpty());
				assertTrue(!newCard.getMetadata().isEmpty());
				// Created Date
				assertFalse(oldCard.getCreatedDate().after(new Date()));
				assertFalse(newCard.getCreatedDate().after(new Date()));
				assertTrue(newCard.getCreatedDate().equals(oldCard.getCreatedDate()));
				// Modified Date
				assertTrue(newCard.getModifiedDate().after(oldCard.getModifiedDate()));
				// Effective Date
				assertTrue(newCard.getEffectiveDate().after(oldCard.getEffectiveDate()));
				// Expiration Date
				assertTrue(newCard.getExpirationDate().after(oldCard.getExpirationDate()));
				// Thumbnail
				assertTrue(Arrays.equals(newCard.getThumbnail(), oldCard.getThumbnail()));

			} else {
				Assert.fail("Expecting one or the other of the updated records.");
			}

		}

		/** READ **/
		CommonQueryBuilder builder = new CommonQueryBuilder();

		QueryImpl query = builder.queryByProperty(Metacard.RESOURCE_URI, uri2);

		QueryRequestImpl queryRequest = new QueryRequestImpl(query);
		SourceResponse sourceResponse = provider.query(queryRequest);

		assertEquals(1, sourceResponse.getResults().size());

		for (Result r : sourceResponse.getResults()) {

			assertTrue(r.getMetacard().getMetadata().indexOf("Cardinals") != ALL_RESULTS);

			assertEquals(uri2, r.getMetacard().getResourceURI().toString());
		}

		/** UPDATE with null thumbnail **/
		updatedMetacard1.setThumbnail(null);
		updateResponse = update(updatedMetacard1.getId(), updatedMetacard1);

		assertEquals("Testing Update operation: ", 1, updateResponse.getUpdatedMetacards().size());

		Metacard newCard = updateResponse.getUpdatedMetacards().get(0).getNewMetacard();
		Metacard oldCard = updateResponse.getUpdatedMetacards().get(0).getOldMetacard();

		assertNotNull(oldCard.getThumbnail());
		assertEquals(null, newCard.getThumbnail());

		/** UPDATE with null WKT **/
		// updatedMetacard1.setLocation(null);
		// updateResponse = provider.update(new
		// UpdateRequestImpl(updatedMetacard1.getId(), updatedMetacard1));
		//
		// 
		//
		// assertEquals("Testing Update operation: ", 1,
		// updateResponse.getUpdatedMetacards().size());
		//
		// newCard =
		// updateResponse.getUpdatedMetacards().get(0).getNewMetacard();
		// oldCard =
		// updateResponse.getUpdatedMetacards().get(0).getOldMetacard();
		//
		// assertNotNull(oldCard.getResourceURI());
		// assertNotNull(newCard.getResourceURI());
		// assertEquals(oldCard.getResourceURI().toString(),
		// newCard.getResourceURI().toString());
		// assertEquals(provider.getId(), oldCard.getSourceId());
		// assertEquals(provider.getId(), newCard.getSourceId());
		// LOGGER.info("New Metacard location: " + newCard.getLocation());
		// LOGGER.info("Old Metacard location: " + oldCard.getLocation());
		// assertTrue(oldCard.getLocation().contains("POINT"));
		// assertEquals(null, newCard.getLocation());

		/** UPDATE with null expiration date **/
		updatedMetacard1.setExpirationDate(null);
		updateResponse = update(updatedMetacard1.getId(), updatedMetacard1);

		assertEquals("Testing Update operation: ", ONE_HIT, updateResponse.getUpdatedMetacards().size());

		newCard = updateResponse.getUpdatedMetacards().get(0).getNewMetacard();
		oldCard = updateResponse.getUpdatedMetacards().get(0).getOldMetacard();

		assertNotNull(oldCard.getExpirationDate());
		assertEquals(null, newCard.getExpirationDate());

		/** UPDATE with null content type **/
		updatedMetacard1.setContentTypeName(null);
		updateResponse = update(updatedMetacard1.getId(), updatedMetacard1);

		assertEquals("Testing Update operation: ", ONE_HIT, updateResponse.getUpdatedMetacards().size());

		newCard = updateResponse.getUpdatedMetacards().get(0).getNewMetacard();
		oldCard = updateResponse.getUpdatedMetacards().get(0).getOldMetacard();

		assertNotNull(oldCard.getContentTypeName());
		assertThat(newCard.getContentTypeName(), nullValue());

		/** UPDATE with empty content type **/
		updatedMetacard1.setContentTypeName("");
		updateResponse = update(updatedMetacard1.getId(), updatedMetacard1);

		assertEquals("Testing Update operation: ", ONE_HIT, updateResponse.getUpdatedMetacards().size());

		newCard = updateResponse.getUpdatedMetacards().get(0).getNewMetacard();
		oldCard = updateResponse.getUpdatedMetacards().get(0).getOldMetacard();

		assertThat(oldCard.getContentTypeName(), nullValue());
		assertThat(newCard.getContentTypeName(), is(""));

		/** UPDATE with null content type version **/
		updatedMetacard1.setContentTypeVersion(null);
		updateResponse = update(updatedMetacard1.getId(), updatedMetacard1);

		assertEquals("Testing Update operation: ", ONE_HIT, updateResponse.getUpdatedMetacards().size());

		newCard = updateResponse.getUpdatedMetacards().get(0).getNewMetacard();
		oldCard = updateResponse.getUpdatedMetacards().get(0).getOldMetacard();

		assertNotNull(oldCard.getContentTypeVersion());
		assertThat(newCard.getContentTypeVersion(), nullValue());

		/** UPDATE with empty content type version **/
		updatedMetacard1.setContentTypeVersion("");
		updateResponse = update(updatedMetacard1.getId(), updatedMetacard1);

		assertEquals("Testing Update operation: ", ONE_HIT, updateResponse.getUpdatedMetacards().size());

		newCard = updateResponse.getUpdatedMetacards().get(0).getNewMetacard();
		oldCard = updateResponse.getUpdatedMetacards().get(0).getOldMetacard();

		assertThat(oldCard.getContentTypeVersion(), nullValue());
		assertThat(newCard.getContentTypeVersion(), is(""));

		/** UPDATE with new resource uri **/
		updatedMetacard1.setResourceURI(new URI(uri1 + "Now"));
		updateResponse = update(updatedMetacard1.getId(), updatedMetacard1);

		assertEquals("Testing Update operation: ", ONE_HIT, updateResponse.getUpdatedMetacards().size());

		newCard = updateResponse.getUpdatedMetacards().get(0).getNewMetacard();
		oldCard = updateResponse.getUpdatedMetacards().get(0).getOldMetacard();

		assertThat(oldCard.getResourceURI(), is(nullValue()));
		assertEquals(uri1 + "Now", newCard.getResourceURI().toString());

		/** TEST NULL UPDATE **/
		updateResponse = provider.update(new UpdateRequest() {
			@Override
			public boolean hasProperties() {
				return false;
			}

			@Override
			public Serializable getPropertyValue(String name) {
				return null;
			}

			@Override
			public Set<String> getPropertyNames() {
				return null;
			}

			@Override
			public Map<String, Serializable> getProperties() {
				return null;
			}

			@Override
			public boolean containsPropertyName(String name) {
				return false;
			}

			@Override
			public List<Entry<Serializable, Metacard>> getUpdates() {
				return null;
			}

			@Override
			public String getAttributeName() {
				return UpdateRequest.UPDATE_BY_ID;
			}
		});

		assertTrue(updateResponse.getUpdatedMetacards().isEmpty());
	}

	/**
	 * Testing that if no records are found to update, the provider returns an
	 * empty list.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test
	public void testUpdateOperationWithNoResults() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

		UpdateResponse response = update("BAD_ID", metacard);

		assertEquals(0, response.getUpdatedMetacards().size());

	}

	/**
	 * Testing update operation of alternative attribute. Should return positive
	 * results.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test
	public void testUpdateAlternativeAttribute() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		final MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

		create(metacard);

		UpdateResponse response = provider.update(new UpdateRequest() {

			@Override
			public boolean hasProperties() {
				return false;
			}

			@Override
			public Serializable getPropertyValue(String name) {
				return null;
			}

			@Override
			public Set<String> getPropertyNames() {
				return null;
			}

			@Override
			public Map<String, Serializable> getProperties() {
				return null;
			}

			@Override
			public boolean containsPropertyName(String name) {
				return false;
			}

			@Override
			public List<Entry<Serializable, Metacard>> getUpdates() {

				MetacardImpl newMetacard = new MetacardImpl(metacard);

				newMetacard.setContentTypeName("newContentName");

				List<Entry<Serializable, Metacard>> updateList = new ArrayList<Entry<Serializable, Metacard>>();

				updateList.add(new SimpleEntry<Serializable, Metacard>(MockMetacard.DEFAULT_TITLE, newMetacard));

				return updateList;
			}

			@Override
			public String getAttributeName() {
				return Metacard.TITLE;
			}
		});

		

		Update update = response.getUpdatedMetacards().get(0);

		assertThat(update.getNewMetacard().getId(), is(equalTo(update.getOldMetacard().getId())));

		assertEquals(1, response.getUpdatedMetacards().size());

	}

	/**
	 * Tests if we catch properly the case that the attribute value matches
	 * multiple Metacards.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test(expected = IngestException.class)
	public void testUpdateNonUniqueAttributeValue() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		MockMetacard m1 = new MockMetacard(Library.getFlagstaffRecord());
		MockMetacard m2 = new MockMetacard(Library.getFlagstaffRecord());
		MockMetacard m3 = new MockMetacard(Library.getFlagstaffRecord());

		List<Metacard> list = Arrays.asList((Metacard) m1, m2, m3);

		create(list);

		provider.update(new UpdateRequest() {

			@Override
			public boolean hasProperties() {
				return false;
			}

			@Override
			public Serializable getPropertyValue(String name) {
				return null;
			}

			@Override
			public Set<String> getPropertyNames() {
				return null;
			}

			@Override
			public Map<String, Serializable> getProperties() {
				return null;
			}

			@Override
			public boolean containsPropertyName(String name) {
				return false;
			}

			@Override
			public List<Entry<Serializable, Metacard>> getUpdates() {

				MockMetacard newMetacard = new MockMetacard(Library.getShowLowRecord());

				List<Entry<Serializable, Metacard>> updateList = new ArrayList<Entry<Serializable, Metacard>>();

				updateList.add(new SimpleEntry<Serializable, Metacard>(MockMetacard.DEFAULT_TITLE, newMetacard));

				return updateList;
			}

			@Override
			public String getAttributeName() {
				return Metacard.TITLE;
			}
		});
	}

	/**
	 * Tests if we catch a rare case where some attribute value match multiple
	 * Metacards while others do not match any records.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test(expected = IngestException.class)
	public void testUpdateNonUniqueAttributeValue2() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		MockMetacard m1 = new MockMetacard(Library.getFlagstaffRecord());
		MockMetacard m2 = new MockMetacard(Library.getFlagstaffRecord());

		List<Metacard> list = Arrays.asList((Metacard) m1, m2);

		create(list);

		provider.update(new UpdateRequest() {

			@Override
			public boolean hasProperties() {
				return false;
			}

			@Override
			public Serializable getPropertyValue(String name) {
				return null;
			}

			@Override
			public Set<String> getPropertyNames() {
				return null;
			}

			@Override
			public Map<String, Serializable> getProperties() {
				return null;
			}

			@Override
			public boolean containsPropertyName(String name) {
				return false;
			}

			@Override
			public List<Entry<Serializable, Metacard>> getUpdates() {

				MockMetacard newMetacard = new MockMetacard(Library.getShowLowRecord());

				List<Entry<Serializable, Metacard>> updateList = new ArrayList<Entry<Serializable, Metacard>>();

				updateList.add(new SimpleEntry<Serializable, Metacard>(MockMetacard.DEFAULT_TITLE, newMetacard));
				updateList.add(new SimpleEntry<Serializable, Metacard>(TAMPA_QUERY_PHRASE, newMetacard));

				return updateList;
			}

			@Override
			public String getAttributeName() {
				return Metacard.TITLE;
			}
		});
	}

	/**
	 * Testing update operation of unknown attribute. Should return no results.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	public void testUpdateUnknownAttribute() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		UpdateResponse response = provider.update(new UpdateRequest() {

			@Override
			public boolean hasProperties() {
				return false;
			}

			@Override
			public Serializable getPropertyValue(String name) {
				return null;
			}

			@Override
			public Set<String> getPropertyNames() {
				return null;
			}

			@Override
			public Map<String, Serializable> getProperties() {
				return null;
			}

			@Override
			public boolean containsPropertyName(String name) {
				return false;
			}

			@Override
			public List<Entry<Serializable, Metacard>> getUpdates() {
				MockMetacard newMetacard = new MockMetacard(Library.getShowLowRecord());

				List<Entry<Serializable, Metacard>> updateList = new ArrayList<Entry<Serializable, Metacard>>();

				updateList.add(new SimpleEntry<Serializable, Metacard>(MockMetacard.DEFAULT_TITLE, newMetacard));

				return updateList;
			}

			@Override
			public String getAttributeName() {
				return "dataAccess";
			}
		});
		
		

		assertEquals(0, response.getUpdatedMetacards().size());

	}

	/**
	 * Testing if exception is thrown with a <code>null</code> property.
	 * 
	 * @throws IngestException
	 */
	@Test(expected = IngestException.class)
	public void testUpdateNullAttribute() throws IngestException, UnsupportedQueryException {
		provider.update(new UpdateRequest() {

			@Override
			public boolean hasProperties() {
				return false;
			}

			@Override
			public Serializable getPropertyValue(String name) {
				return null;
			}

			@Override
			public Set<String> getPropertyNames() {
				return null;
			}

			@Override
			public Map<String, Serializable> getProperties() {
				return null;
			}

			@Override
			public boolean containsPropertyName(String name) {
				return false;
			}

			@Override
			public List<Entry<Serializable, Metacard>> getUpdates() {
				return null;
			}

			@Override
			public String getAttributeName() {
				return null;
			}
		});

	}

	/**
	 * Testing that if records are properly deleted.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test
	public void testDeleteOperation() throws IngestException, UnsupportedQueryException {

		// Single Deletion

		deleteAllIn(provider);

		MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

		CreateResponse createResponse = create(metacard);

		DeleteResponse deleteResponse = delete(createResponse.getCreatedMetacards()
				.get(0).getId());

		Metacard deletedMetacard = deleteResponse.getDeletedMetacards().get(0);

		verifyDeletedRecord(metacard, createResponse, deleteResponse, deletedMetacard);

	}
	
    @Test
    public void testDeleteList() throws IngestException, UnsupportedQueryException {
        int metacardCount = 20;
        deleteAllIn(provider);

        List<Metacard> metacards = new ArrayList<Metacard>();
        for (int i = 0; i < metacardCount; i++) {
            metacards.add(new MockMetacard(Library.getFlagstaffRecord()));
        }

        CreateResponse createResponse = create(metacards);
        assertThat(createResponse.getCreatedMetacards().size(), is(metacards.size()));

        List<String> ids = new ArrayList<String>();
        for (Metacard mc : createResponse.getCreatedMetacards()) {
            ids.add(mc.getId());
        }

        DeleteResponse deleteResponse = delete(ids.toArray(new String[metacardCount]));
        assertThat(deleteResponse.getDeletedMetacards().size(), is(metacards.size()));
        
        for (int i = 0; i < metacardCount; i++) {
            assertThat(deleteResponse.getDeletedMetacards().get(i).getId(), is(ids.get(i)));
        }
    }

	/**
	 * Tests what happens when the whole request is null.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test(expected = IngestException.class)
	public void testDeleteNull() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		provider.delete(null);

		fail();

	}

	/**
	 * Tests the provider will allow you to delete nothing.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test
	public void testDeleteNothing() throws IngestException, UnsupportedQueryException {

		// Single Deletion

		deleteAllIn(provider);

		DeleteResponse deleteResponse = delete("no_such_record");

		assertThat(deleteResponse.getDeletedMetacards().size(), equalTo(0));

	}


	/**
	 * Testing if another attribute can be used to delete records other than
	 * {@link Metacard#ID}
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test
	public void testDeleteAlternativeAttribute() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

		CreateResponse createResponse = create(metacard);

		DeleteResponse deleteResponse = provider.delete(new DeleteRequest() {

			@Override
			public boolean hasProperties() {
				return false;
			}

			@Override
			public Serializable getPropertyValue(String name) {
				return null;
			}

			@Override
			public Set<String> getPropertyNames() {
				return null;
			}

			@Override
			public Map<String, Serializable> getProperties() {
				return null;
			}

			@Override
			public boolean containsPropertyName(String name) {
				return false;
			}

			@Override
			public List<? extends Serializable> getAttributeValues() {
				return Arrays.asList(MockMetacard.DEFAULT_TITLE);
			}

			@Override
			public String getAttributeName() {
				return Metacard.TITLE;
			}
		}

		);

		

		Metacard deletedMetacard = deleteResponse.getDeletedMetacards().get(0);

		verifyDeletedRecord(metacard, createResponse, deleteResponse, deletedMetacard);

		// verify it is really not in SOLR

		Filter filter = filterBuilder.attribute(Metacard.TITLE).like().text(MockMetacard.DEFAULT_TITLE);

		QueryImpl query = new QueryImpl(filter);

		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

		List<Result> results = sourceResponse.getResults();
		assertEquals(0, results.size());

	}

	@Test
	public void testDeleteNoList() throws IngestException, UnsupportedQueryException {

		/* EMPTY */
		DeleteRequestImpl deleteRequest = new DeleteRequestImpl(new String[0]);

		DeleteResponse results = provider.delete(deleteRequest);

		assertNotNull(results.getDeletedMetacards());

		assertEquals(0, results.getDeletedMetacards().size());

		assertEquals(deleteRequest, results.getRequest());

		/* EMPTY */
		DeleteRequestImpl emptyDeleteRequest = new DeleteRequestImpl(new ArrayList<Serializable>(),
				DeleteRequest.DELETE_BY_ID, null);

		results = provider.delete(emptyDeleteRequest);

		assertNotNull(results.getDeletedMetacards());

		assertEquals(0, results.getDeletedMetacards().size());

		assertEquals(emptyDeleteRequest, results.getRequest());

		/* NULL */
		DeleteRequest nullDeleteRequest = new DeleteRequestImpl(null, DeleteRequest.DELETE_BY_ID, null);

		results = provider.delete(nullDeleteRequest);

		assertNotNull(results.getDeletedMetacards());

		assertEquals(0, results.getDeletedMetacards().size());

		assertEquals(nullDeleteRequest, results.getRequest());

	}

	@Test
	public void testExtensibleMetacards() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		String brandNewField1 = "description";
		String brandNewFieldValue1 = "myDescription";

		/* BASIC STRINGS */
		Set<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
		descriptors.add(new AttributeDescriptorImpl(Metacard.ID, true, true, true, false, BasicTypes.STRING_TYPE));
		descriptors.add(new AttributeDescriptorImpl(brandNewField1, true, true, true, false, BasicTypes.STRING_TYPE));
		MetacardTypeImpl mType = new MetacardTypeImpl("custom1", descriptors);
		MetacardImpl customMetacard = new MetacardImpl(mType);
		// customMetacard.setAttribute("id", "44567880");
		customMetacard.setAttribute(brandNewField1, brandNewFieldValue1);

		create(customMetacard);

		// search id
		Query query = new QueryImpl(filterBuilder.attribute("id").like().text("*"));

		QueryRequest request = new QueryRequestImpl(query);

		SourceResponse response = provider.query(request);

		assertEquals(1, response.getResults().size());

		assertEquals("custom1", response.getResults().get(0).getMetacard().getMetacardType().getName());

		assertThat(response.getResults().get(0).getMetacard().getMetacardType().getAttributeDescriptors(),
				equalTo(descriptors));

		// search title - *
		query = new QueryImpl(filterBuilder.attribute(brandNewField1).like().text("*"));

		request = new QueryRequestImpl(query);

		response = provider.query(request);

		assertEquals(1, response.getResults().size());

		assertEquals("custom1", response.getResults().get(0).getMetacard().getMetacardType().getName());

		assertThat(response.getResults().get(0).getMetacard().getMetacardType().getAttributeDescriptors(),
				equalTo(descriptors));

		// search title - exact
		query = new QueryImpl(filterBuilder.attribute(brandNewField1).equalTo().text(brandNewFieldValue1));

		request = new QueryRequestImpl(query);

		response = provider.query(request);

		assertEquals(1, response.getResults().size());

		// search negative

		query = new QueryImpl(filterBuilder.attribute(brandNewField1).like().text("no"));

		request = new QueryRequestImpl(query);

		response = provider.query(request);

		assertEquals(0, response.getResults().size());

		// NEW TYPE

		String brandNewXmlField1 = "author";
		String brandNewXmlFieldValue1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + "<author>john doe</author>";

		descriptors = new HashSet<AttributeDescriptor>();
		descriptors.add(new AttributeDescriptorImpl(Metacard.ID, true, true, true, false, BasicTypes.STRING_TYPE));
		descriptors.add(new AttributeDescriptorImpl(brandNewXmlField1, true, true, true, false, BasicTypes.XML_TYPE));
		mType = new MetacardTypeImpl("34ga$^TGHfg:/", descriptors);
		customMetacard = new MetacardImpl(mType);
		// customMetacard.setAttribute(Metacard.ID, "44567880");
		customMetacard.setAttribute(brandNewXmlField1, brandNewXmlFieldValue1);

		create(customMetacard);

		// Two Ids
		query = new QueryImpl(filterBuilder.attribute(Metacard.ID).like().text("*"));

		request = new QueryRequestImpl(query);

		response = provider.query(request);

		assertEquals(2, response.getResults().size());

		// search xml
		query = new QueryImpl(filterBuilder.attribute(brandNewXmlField1).like().caseSensitiveText("doe"));

		request = new QueryRequestImpl(query);

		response = provider.query(request);

		assertEquals(1, response.getResults().size());

		assertEquals("34ga$^TGHfg:/", response.getResults().get(0).getMetacard().getMetacardType().getName());

		assertThat(response.getResults().get(0).getMetacard().getMetacardType().getAttributeDescriptors(),
				equalTo(descriptors));

		// search xml - negative
		query = new QueryImpl(filterBuilder.attribute(brandNewXmlField1).like().text("author"));

		request = new QueryRequestImpl(query);

		response = provider.query(request);

		assertEquals(0, response.getResults().size());

		// EVERYTHING ELSE type
		String doubleField = "hertz";
		double doubleFieldValue = 16065.435;

		String floatField = "inches";
		float floatFieldValue = 4.435f;

		String intField = "count";
		int intFieldValue = 4;

		String longField = "milliseconds";
		long longFieldValue = 987654322111L;

		String byteField = "bytes";
		byte[] byteFieldValue = { 86 };

		String booleanField = "expected";
		boolean booleanFieldValue = true;

		String dateField = "lost";
		Date dateFieldValue = new Date();

		String geoField = "geo";
		String geoFieldValue = GULF_OF_GUINEA_POINT_WKT;

		String shortField = "daysOfTheWeek";
		short shortFieldValue = 1;

		String objectField = "payload";
		BevelBorder objectFieldValue = new BevelBorder(BevelBorder.RAISED);

		descriptors = new HashSet<AttributeDescriptor>();
		descriptors.add(new AttributeDescriptorImpl(Metacard.ID, true, true, true, false, BasicTypes.STRING_TYPE));
		descriptors.add(new AttributeDescriptorImpl(doubleField, true, true, false, false, BasicTypes.DOUBLE_TYPE));
		descriptors.add(new AttributeDescriptorImpl(floatField, true, true, false, false, BasicTypes.FLOAT_TYPE));
		descriptors.add(new AttributeDescriptorImpl(intField, true, true, false, false, BasicTypes.INTEGER_TYPE));
		descriptors.add(new AttributeDescriptorImpl(longField, true, true, false, false, BasicTypes.LONG_TYPE));
		descriptors.add(new AttributeDescriptorImpl(byteField, false, true, false, false, BasicTypes.BINARY_TYPE));
		descriptors.add(new AttributeDescriptorImpl(booleanField, true, true, false, false, BasicTypes.BOOLEAN_TYPE));
		descriptors.add(new AttributeDescriptorImpl(dateField, true, true, false, false, BasicTypes.DATE_TYPE));
		descriptors.add(new AttributeDescriptorImpl(geoField, true, true, false, false, BasicTypes.GEO_TYPE));
		descriptors.add(new AttributeDescriptorImpl(shortField, true, true, false, false, BasicTypes.SHORT_TYPE));
		descriptors.add(new AttributeDescriptorImpl(objectField, false, true, false, false, BasicTypes.OBJECT_TYPE));

		mType = new MetacardTypeImpl("numbersMT", descriptors);

		customMetacard = new MetacardImpl(mType);
		// customMetacard.setAttribute(Metacard.ID, "245gasg324");
		customMetacard.setAttribute(doubleField, doubleFieldValue);
		customMetacard.setAttribute(floatField, floatFieldValue);
		customMetacard.setAttribute(intField, intFieldValue);
		customMetacard.setAttribute(longField, longFieldValue);
		customMetacard.setAttribute(byteField, byteFieldValue);
		customMetacard.setAttribute(booleanField, booleanFieldValue);
		customMetacard.setAttribute(dateField, dateFieldValue);
		customMetacard.setAttribute(geoField, geoFieldValue);
		customMetacard.setAttribute(shortField, shortFieldValue);
		customMetacard.setAttribute(objectField, objectFieldValue);

		create(customMetacard);

		// Three Ids
		query = new QueryImpl(filterBuilder.attribute(Metacard.ID).like().text("*"));

		request = new QueryRequestImpl(query);

		response = provider.query(request);

		assertEquals(3, response.getResults().size());

		// search double
		query = new QueryImpl(filterBuilder.attribute(doubleField).greaterThan().number(doubleFieldValue - 1.0));

		request = new QueryRequestImpl(query);

		response = provider.query(request);

		assertEquals(1, response.getResults().size());

		// search int
		query = new QueryImpl(filterBuilder.attribute(intField).greaterThan().number(intFieldValue - 1));

		request = new QueryRequestImpl(query);

		response = provider.query(request);

		assertEquals(1, response.getResults().size());

		Metacard resultMetacard = response.getResults().get(0).getMetacard();

		assertThat(resultMetacard.getAttribute(Metacard.ID), notNullValue());
		assertThat((Double) (resultMetacard.getAttribute(doubleField).getValue()), equalTo(doubleFieldValue));
		assertThat((Integer) (resultMetacard.getAttribute(intField).getValue()), equalTo(intFieldValue));
		assertThat((Float) (resultMetacard.getAttribute(floatField).getValue()), equalTo(floatFieldValue));
		assertThat((Long) (resultMetacard.getAttribute(longField).getValue()), equalTo(longFieldValue));
		assertThat((byte[]) (resultMetacard.getAttribute(byteField).getValue()), equalTo(byteFieldValue));
		assertThat((Boolean) (resultMetacard.getAttribute(booleanField).getValue()), equalTo(booleanFieldValue));
		assertThat((Date) (resultMetacard.getAttribute(dateField).getValue()), equalTo(dateFieldValue));
		assertThat((String) (resultMetacard.getAttribute(geoField).getValue()), equalTo(geoFieldValue));
		assertThat((Short) (resultMetacard.getAttribute(shortField).getValue()), equalTo(shortFieldValue));
		assertThat((BevelBorder) (resultMetacard.getAttribute(objectField).getValue()), instanceOf(BevelBorder.class));

		/*
		 * Going to use the XMLEncoder. If it writes out the objects the same
		 * way in xml, then they are the same.
		 */
		ByteArrayOutputStream beveledBytesStreamFromSolr = new ByteArrayOutputStream();
		XMLEncoder solrXMLEncoder = new XMLEncoder(new BufferedOutputStream(beveledBytesStreamFromSolr));
		solrXMLEncoder.writeObject((resultMetacard.getAttribute(objectField).getValue()));
		solrXMLEncoder.close();

		ByteArrayOutputStream beveledBytesStreamOriginal = new ByteArrayOutputStream();
		XMLEncoder currendEncoder = new XMLEncoder(new BufferedOutputStream(beveledBytesStreamOriginal));
		currendEncoder.writeObject(objectFieldValue);
		currendEncoder.close();

		assertThat(beveledBytesStreamFromSolr.toByteArray(), equalTo(beveledBytesStreamOriginal.toByteArray()));

		// search short
		query = new QueryImpl(filterBuilder.attribute(shortField).greaterThanOrEqualTo().number(shortFieldValue));

		request = new QueryRequestImpl(query);

		response = provider.query(request);

		assertEquals(1, response.getResults().size());

		resultMetacard = response.getResults().get(0).getMetacard();

	}

	@Test
	public void testQueryIsNull() throws Exception {
        SourceResponse response = provider.query(new QueryRequestImpl(null));
        assertEquals(0, response.getHits());

        response = provider.query(null);
        assertEquals(0, response.getHits());
	}
	
    @Test
    public void testQueryHasLuceneSpecialCharacters() throws Exception {
        deleteAllIn(provider);
        
        List<Metacard> list = Arrays.asList((Metacard) new MockMetacard(Library.getFlagstaffRecord()),
                new MockMetacard(Library.getTampaRecord()));
        create(list);

        // if + is escaped, this query will be an implicit OR otherwise both both terms would be required
        Filter txtFilter = filterBuilder.attribute(Metacard.ANY_TEXT).like()
                .text("+Flag?taff +" + TAMPA_QUERY_PHRASE);

        SourceResponse response = provider.query(new QueryRequestImpl(new QueryImpl(txtFilter)));

        assertEquals(1, response.getResults().size());
    }
	
	/**
	 * Searching Solr with a field not known to the server should return 0
	 * results and should not give an error.
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test
	public void testQueryMissingField() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		// TXT FORMAT
		Filter txtFilter = filterBuilder.attribute("missingField").like().text("*");

		SourceResponse response = provider.query(quickQuery(txtFilter));

		assertEquals(0, response.getResults().size());

		// DATE FORMAT
		Filter dateFilter = filterBuilder.attribute("missingField").before().date(new Date());

		response = provider.query(quickQuery(dateFilter));

		assertEquals(0, response.getResults().size());

		// GEO FORMAT
		Filter geoFilter = filterBuilder.attribute("missingField").intersecting().wkt("POINT ( 1 0 ) ");

		response = provider.query(quickQuery(geoFilter));

		assertEquals(0, response.getResults().size());

		// NUMERICAL FORMAT
		Filter numericalFilter = filterBuilder.attribute("missingField").greaterThanOrEqualTo().number(23L);

		response = provider.query(quickQuery(numericalFilter));

		assertEquals(0, response.getResults().size());

	}

	@Test
	public void testQueryMissingSortField() throws IngestException, UnsupportedQueryException {

		deleteAllIn(provider);

		MockMetacard m = new MockMetacard(Library.getTampaRecord());
		m.setTitle("Tampa");

		List<Metacard> list = Arrays.asList((Metacard) m, new MockMetacard(Library.getFlagstaffRecord()));

		create(list);

		Filter txtFilter = filterBuilder.attribute("id").like().text("*");

		QueryImpl query = new QueryImpl(txtFilter);

		query.setSortBy(new ddf.catalog.filter.SortByImpl("unknownField", SortOrder.ASCENDING));

		SourceResponse response = provider.query(new QueryRequestImpl(query));

		assertEquals(2, response.getResults().size());

	}

	/**
	 * Testing if the temporal search does not fail when no schema field can be
	 * found and/or there is no data in the index
	 * 
	 * @throws IngestException
	 * @throws UnsupportedQueryException
	 */
	@Test
	public void testQueryMissingSortFieldTemporal() throws IngestException, UnsupportedQueryException {

		/*
		 * I have tested this with an empty schema and without an empty schema -
		 * both pass, but there is no regression test for the empty schema
		 * scenario 
		 * TODO there should probably be an automated test that creates
		 * a fresh cache, that would be a better test
		 */

		deleteAllIn(provider);

		Filter txtFilter = filterBuilder.attribute("id").like().text("*");

		QueryImpl query = new QueryImpl(txtFilter);

		query.setSortBy(new ddf.catalog.filter.SortByImpl(Result.TEMPORAL, SortOrder.ASCENDING));

		SourceResponse response = provider.query(new QueryRequestImpl(query));

		assertEquals(0, response.getResults().size());

	}

	/**
	 * If parts of a query can be understood, the query should be executed
	 * whereas the part that has a missing property should return 0 results.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTwoQueriesWithOneMissingField() throws Exception {

		deleteAllIn(provider);

		MockMetacard m = new MockMetacard(Library.getTampaRecord());
		m.setTitle("Tampa");

		List<Metacard> list = Arrays.asList((Metacard) m, new MockMetacard(Library.getFlagstaffRecord()));

		create(list);

		Filter filter = filterBuilder.anyOf(filterBuilder.attribute(Metacard.TITLE).text("Tampa"), filterBuilder
				.attribute("missingField").text("someText"));

		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Tampa should be found", 1, sourceResponse.getResults().size());

	}

	@Test(expected = IngestException.class)
	public void testDeleteNullAttribute() throws IngestException, UnsupportedQueryException {

		provider.delete(new DeleteRequest() {

			@Override
			public boolean hasProperties() {
				return false;
			}

			@Override
			public Serializable getPropertyValue(String name) {
				return null;
			}

			@Override
			public Set<String> getPropertyNames() {
				return null;
			}

			@Override
			public Map<String, Serializable> getProperties() {
				return null;
			}

			@Override
			public boolean containsPropertyName(String name) {
				return false;
			}

			@Override
			public List<? extends Serializable> getAttributeValues() {
				return null;
			}

			@Override
			public String getAttributeName() {
				return null;
			}
		});

	}

	@Test
	public void testContextualSimpleWithLogicOperators() throws Exception {

		deleteAllIn(provider);

		MockMetacard m = new MockMetacard(Library.getTampaRecord());
		m.setTitle("Tampa");

		List<Metacard> list = Arrays.asList((Metacard) new MockMetacard(Library.getFlagstaffRecord()), m);

		assertEquals(2, create(list).getCreatedMetacards().size());

		/** CONTEXTUAL QUERY - AND negative **/

		String wildcard = DEFAULT_TEST_WILDCARD;
		String singleChar = DEFAULT_TEST_SINGLE_WILDCARD;
		String escape = DEFAULT_TEST_ESCAPE;

		FilterFactory filterFactory = new FilterFactoryImpl();

		Filter filter = filterFactory.and(filterFactory.like(filterFactory.property(Metacard.METADATA),
				FLAGSTAFF_QUERY_PHRASE, wildcard, singleChar, escape, false), filterFactory.like(
				filterFactory.property(Metacard.METADATA), TAMPA_QUERY_PHRASE, wildcard, singleChar, escape, false));

		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Flagstaff and Tampa", 0, sourceResponse.getResults().size());

		/** CONTEXTUAL QUERY - AND positive **/

		filter = filterFactory.and(filterFactory.like(filterFactory.property(Metacard.METADATA),
				FLAGSTAFF_QUERY_PHRASE, wildcard, singleChar, escape, false), filterFactory.like(
				filterFactory.property(Metacard.METADATA), AIRPORT_QUERY_PHRASE, wildcard, singleChar, escape, false));

		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Flagstaff and Airport", ONE_HIT, sourceResponse.getResults().size());

		/** CONTEXTUAL QUERY - OR positive **/

		filter = filterFactory.or(filterFactory.like(filterFactory.property(Metacard.METADATA), FLAGSTAFF_QUERY_PHRASE,
				wildcard, singleChar, escape, false), filterFactory.like(filterFactory.property(Metacard.METADATA),
				TAMPA_QUERY_PHRASE, wildcard, singleChar, escape, false));

		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Flagstaff OR Tampa", 2, sourceResponse.getResults().size());

		/** CONTEXTUAL QUERY - AND / OR positive **/

		filter = filterFactory.or(filterFactory.and(filterFactory.like(filterFactory.property(Metacard.METADATA),
				AIRPORT_QUERY_PHRASE, wildcard, singleChar, escape, false), filterFactory.like(
				filterFactory.property(Metacard.METADATA), "AZ", wildcard, singleChar, escape, false)), filterFactory
				.like(filterFactory.property(Metacard.METADATA), FLAGSTAFF_QUERY_PHRASE, wildcard, singleChar, escape,
						false));

		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Failed: (Airport AND AZ) or Flagstaff", ONE_HIT, sourceResponse.getResults().size());
		
		/** COMPLEX CONTEXTUAL QUERY **/

		filter = filterFactory.and(filterFactory.like(filterFactory.property(Metacard.METADATA),
				AIRPORT_QUERY_PHRASE, wildcard, singleChar, escape, false),
				filterFactory.and(filterFactory.like(filterFactory.property(Metacard.METADATA),
				"AZ", wildcard, singleChar, escape, false),filterFactory.or (filterFactory.like(filterFactory.property(Metacard.METADATA),
				FLAGSTAFF_QUERY_PHRASE, wildcard, singleChar, escape, false), filterFactory.like(
				filterFactory.property(Metacard.METADATA),  TAMPA_QUERY_PHRASE, wildcard, singleChar, escape, false)) ));

		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Failed: (Airport AND (AZ AND (Flagstaff OR TAMPA)))", ONE_HIT, sourceResponse.getResults().size());
		

		/** CONTEXTUAL QUERY - NOT positive **/

		filter = filterFactory.and(filterFactory.like(filterFactory.property(Metacard.METADATA),
				FLAGSTAFF_QUERY_PHRASE, wildcard, singleChar, escape, false), filterFactory.not(filterFactory.like(
				filterFactory.property(Metacard.METADATA), TAMPA_QUERY_PHRASE, wildcard, singleChar, escape, false)));

		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Did not find Flagstaff NOT Tampa", ONE_HIT, sourceResponse.getResults().size());

		/** CONTEXTUAL QUERY - NOT negative **/

		filter = filterFactory.and(filterFactory.like(filterFactory.property(Metacard.METADATA),
				FLAGSTAFF_QUERY_PHRASE, wildcard, singleChar, escape, false), filterFactory.not(filterFactory.like(
				filterFactory.property(Metacard.METADATA), AIRPORT_QUERY_PHRASE, wildcard, singleChar, escape, false)));

		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Wrongly found Flagstaff NOT Airport", 0, sourceResponse.getResults().size());

		/** CONTEXTUAL QUERY - Single NOT positive **/

		filter = filterFactory.not(filterFactory.like(filterFactory.property(Metacard.METADATA), TAMPA_QUERY_PHRASE,
				wildcard, singleChar, escape, false));

		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Did not find Flagstaff", ONE_HIT, sourceResponse.getResults().size());
		assertTrue(sourceResponse.getResults().get(0).getMetacard().getMetadata().contains(FLAGSTAFF_QUERY_PHRASE));

		/** CONTEXTUAL QUERY - NOT multi **/
		LinkedList<Filter> filters = new LinkedList<Filter>();
		filters.add(filterFactory.like(filterFactory.property(Metacard.METADATA), FLAGSTAFF_QUERY_PHRASE, wildcard,
				singleChar, escape, false));
		filters.add(filterFactory.not(filterFactory.like(filterFactory.property(Metacard.METADATA), TAMPA_QUERY_PHRASE,
				wildcard, singleChar, escape, false)));
		filters.add(filterFactory.not(filterFactory.like(filterFactory.property(Metacard.METADATA), "Pennsylvania",
				wildcard, singleChar, escape, false)));

		filter = filterFactory.and(filters);

		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Did not find Flagstaff NOT Tampa", ONE_HIT, sourceResponse.getResults().size());

		/** CONTEXTUAL QUERY - AND / OR **/

		filter = filterFactory.or(filterFactory.and(filterFactory.like(filterFactory.property(Metacard.METADATA),
				AIRPORT_QUERY_PHRASE, wildcard, singleChar, escape, false), filterFactory.like(
				filterFactory.property(Metacard.METADATA), "AZ", wildcard, singleChar, escape, false)), filterFactory
				.or(filterFactory.like(filterFactory.property(Metacard.METADATA), FLAGSTAFF_QUERY_PHRASE, wildcard,
						singleChar, escape, false), filterFactory.like(filterFactory.property(Metacard.METADATA), "AZ",
						wildcard, singleChar, escape, false)));

		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Failed: ( Airport )  AND  ( AZ )  OR  ( Flagstaff )  OR  ( AZ ) ", ONE_HIT, sourceResponse
				.getResults().size());

		/** CONTEXTUAL QUERY - OR Then NOT **/

		filter = filterFactory.or(filterFactory.like(filterFactory.property(Metacard.METADATA), FLAGSTAFF_QUERY_PHRASE,
				wildcard, singleChar, escape, false), filterFactory.and(filterFactory.like(
				filterFactory.property(Metacard.METADATA), "AZ", wildcard, singleChar, escape, false), filterFactory
				.not(filterFactory.like(filterFactory.property(Metacard.METADATA), TAMPA_QUERY_PHRASE, wildcard,
						singleChar, escape, false))));

		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Failed: ( Flagstaff )  OR  ( AZ )  NOT  (  ( Tampa )  )  ", ONE_HIT, sourceResponse.getResults()
				.size());
	}
	
    /**
     * Testing attributes are properly indexed.
     * 
     * @param bundleContext
     * @throws Exception
     */
    @Test
    public void testContextualXmlAttributes() throws Exception {

        deleteAllIn(provider);

        List<Metacard> list = new ArrayList<Metacard>();

        String sought_word = "self";
        
        MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());

        list.add(metacard1);

        create(list);

        queryAndVerifyCount(1, filterBuilder.attribute(Metacard.METADATA).is().like().text(sought_word));
    }
	
	/**
	 * Testing {@link Metacard#ANY_TEXT}
	 * 
	 * @param bundleContext
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testContextualAnyText() throws Exception {

		deleteAllIn(provider);

		List<Metacard> list = new ArrayList<Metacard>();

		MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
		String sought_word = "nitf";
		metacard1.setContentTypeName(sought_word);

		list.add(metacard1);

		MockMetacard metacard2 = new MockMetacard(Library.getTampaRecord());

		list.add(metacard2);

		MockMetacard metacard3 = new MockMetacard(Library.getShowLowRecord());

		list.add(metacard3);

		create(list);

		queryAndVerifyCount(1, filterBuilder.attribute(Metacard.ANY_TEXT).is().like().text(sought_word));
	}

	/**
	 * Testing case sensitive index.
	 * 
	 * @param bundleContext
	 * @throws Exception
	 */
	@Test
	public void testContextualCaseSensitiveSimple() throws Exception {

		deleteAllIn(provider);

		List<Metacard> list = Arrays.asList((Metacard) new MockMetacard(Library.getFlagstaffRecord()),
				(Metacard) new MockMetacard(Library.getTampaRecord()));

		create(list);

		CommonQueryBuilder queryBuilder = new CommonQueryBuilder();

		boolean isCaseSensitive = true;
		boolean isFuzzy = false;

		QueryImpl query = null;
		SourceResponse sourceResponse = null;

		/** CONTEXTUAL QUERY - REGRESSION TEST OF SIMPLE TERMS **/

		// Find one
		query = queryBuilder.like(Metacard.METADATA, FLAGSTAFF_QUERY_PHRASE, isCaseSensitive, isFuzzy);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(1, sourceResponse.getResults().size());

		// Find the other
		query = queryBuilder.like(Metacard.METADATA, TAMPA_QUERY_PHRASE, isCaseSensitive, isFuzzy);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(1, sourceResponse.getResults().size());

		// Find nothing
		query = queryBuilder.like(Metacard.METADATA, "NO_SUCH_WORD", isCaseSensitive, isFuzzy);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(0, sourceResponse.getResults().size());

		// Find both
		query = queryBuilder.like(Metacard.METADATA, AIRPORT_QUERY_PHRASE, isCaseSensitive, isFuzzy);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(2, sourceResponse.getResults().size());

		// Phrase
		query = queryBuilder.like(Metacard.METADATA, "Airport TPA in FL", isCaseSensitive, isFuzzy);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(1, sourceResponse.getResults().size());

		/** NEGATIVE CASES **/

		query = queryBuilder.like(Metacard.METADATA, "Tamp", isCaseSensitive, isFuzzy);
		query.setStartIndex(1);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(0, sourceResponse.getResults().size());

		query = queryBuilder.like(Metacard.METADATA, "TAmpa", isCaseSensitive, isFuzzy);
		query.setStartIndex(1);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(0, sourceResponse.getResults().size());

		query = queryBuilder.like(Metacard.METADATA, "AIrport TPA in FL", isCaseSensitive, isFuzzy);
		query.setStartIndex(1);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(0, sourceResponse.getResults().size());

	}

	@Test
	public void testContextualFuzzy() throws Exception {
		deleteAllIn(provider);

		List<Metacard> list = Arrays.asList((Metacard) new MockMetacard(Library.getFlagstaffRecord()),
				(Metacard) new MockMetacard(Library.getTampaRecord()));

		/** CREATE **/
		create(list);

		/** CONTEXTUAL QUERY - FUZZY **/
		Filter filter = filterBuilder.attribute(Metacard.METADATA).like().fuzzyText(FLAGSTAFF_QUERY_PHRASE);
		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
		assertEquals("Expected one hit for fuzzy term 'Flagstaff'", ONE_HIT, sourceResponse.getResults().size());
		
		/** CONTEXTUAL QUERY - FUZZY PHRASE **/
		filter = filterBuilder.attribute(Metacard.METADATA).like().fuzzyText("Flagstaff Chamber");
		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
		assertEquals("Expected one hit for fuzzy term 'Flagstaff Chamber'", ONE_HIT, sourceResponse.getResults().size());

		/** CONTEXTUAL QUERY - FUZZY PHRASE, multiple spaces **/
		filter = filterBuilder.attribute(Metacard.METADATA).like().fuzzyText("Flagstaff    Chamber");
		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
		assertEquals("Expected one hit for fuzzy term 'Flagstaff    Chamber'", ONE_HIT, sourceResponse.getResults().size());

		/** CONTEXTUAL QUERY - FUZZY PHRASE, upper case with insertion **/
		filter = filterBuilder.attribute(Metacard.METADATA).like().fuzzyText("FLGD");
		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
		assertEquals("Expected two hits for fuzzy term 'FLGD'", 2, sourceResponse.getResults().size());
		
		/** CONTEXTUAL QUERY - FUZZY PHRASE, second word missing **/
		filter = filterBuilder.attribute(Metacard.METADATA).like().fuzzyText("Flagstaff Igloo");
		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
		assertEquals("Expected zero hits for fuzzy term 'Flagstaff Igloo'", 0, sourceResponse.getResults().size());

		/** CONTEXTUAL QUERY - FUZZY - Possible POSITIVE CASE **/
		// Possible matches are:
		// Tampa record has word 'company'
		// Flagstaff has word 'camp'
		filter = filterBuilder.attribute(Metacard.METADATA).like().fuzzyText("comp");
		sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
		assertThat("Expected to find any hits for fuzzy 'comp'", sourceResponse.getResults().size(),
				is(greaterThanOrEqualTo(1)));

		/** CONTEXTUAL QUERY - FUZZY - Bad fuzzy field **/
		filter = filterBuilder.attribute(Metacard.CREATED).like().fuzzyText(new Date().toString());
		try {
			sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
			fail("Should not be allowed to run a fuzzy on a date field.");
		} catch (UnsupportedQueryException e) {
			LOGGER.info("Properly received exception.");
		}
	}

	@Test
	public void testContextualWildcard() throws Exception {

		deleteAllIn(provider);

		List<Metacard> list = Arrays.asList((Metacard) new MockMetacard(Library.getFlagstaffRecord()),
				(Metacard) new MockMetacard(Library.getTampaRecord()));

		create(list);

		/** CONTEXTUAL QUERY - SIMPLE TERMS **/

		CommonQueryBuilder queryBuilder = new CommonQueryBuilder();

		boolean isCaseSensitive = false;
		boolean isFuzzy = false;

		QueryImpl query = null;
		SourceResponse sourceResponse = null;

		query = queryBuilder.like(Metacard.METADATA, "Flag*ff Chamber", isCaseSensitive, isFuzzy);
		query.setStartIndex(1);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(1, sourceResponse.getResults().size());

		// FIX FOR THIS IS IN https://issues.apache.org/jira/browse/SOLR-1604
		// Either roll this in yourself or wait for it to come in with Solr
		query = queryBuilder.like(Metacard.METADATA, "Flag*ff Pulliam", isCaseSensitive, isFuzzy);
		query.setStartIndex(1);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		// assertEquals(0, sourceResponse.getResults().size());

		query = queryBuilder.like(Metacard.METADATA, "*rport", isCaseSensitive, isFuzzy);
		query.setStartIndex(1);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(2, sourceResponse.getResults().size());

		query = queryBuilder.like(Metacard.METADATA, "*rpor*", isCaseSensitive, isFuzzy);
		query.setStartIndex(1);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(2, sourceResponse.getResults().size());

		query = queryBuilder.like(Metacard.METADATA, "*", isCaseSensitive, isFuzzy);
		query.setStartIndex(1);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(2, sourceResponse.getResults().size());

		query = queryBuilder.like(Metacard.METADATA, "airpo*t", isCaseSensitive, isFuzzy);
		query.setStartIndex(1);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(2, sourceResponse.getResults().size());

		query = queryBuilder.like(Metacard.METADATA, "Airpo*t", isCaseSensitive, isFuzzy);
		query.setStartIndex(1);
		sourceResponse = provider.query(new QueryRequestImpl(query));
		assertEquals(2, sourceResponse.getResults().size());

	}

	@Test
	public void testPropertyIsLike() throws Exception {
		deleteAllIn(provider);

		List<Metacard> list = new ArrayList<Metacard>();

		MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
		metacard1.setTitle("Mary");

		list.add(metacard1);

		MockMetacard metacard2 = new MockMetacard(Library.getTampaRecord());
		metacard2.setTitle("Mary had a little");

		list.add(metacard2);

		MockMetacard metacard3 = new MockMetacard(Library.getShowLowRecord());
		metacard3.setTitle("Mary had a little l!@#$%^&*()_mb");

		list.add(metacard3);

		create(list);

		queryAndVerifyCount(3, filterBuilder.attribute(Metacard.TITLE).is().like().text("Mary"));
		queryAndVerifyCount(2, filterBuilder.attribute(Metacard.TITLE).is().like().text("little"));
		queryAndVerifyCount(0, filterBuilder.attribute(Metacard.TITLE).is().like().text("gary"));

	}

	@Test
	public void testPropertyIsEqualTo() throws Exception {
		deleteAllIn(provider);

		List<Metacard> list = new ArrayList<Metacard>();

		/* STRINGS */

		MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
		metacard1.setTitle("Mary");
		Date exactEffectiveDate = new DateTime().minusMinutes(1).toDate();
		metacard1.setEffectiveDate(exactEffectiveDate);

		list.add(metacard1);

		MockMetacard metacard2 = new MockMetacard(Library.getTampaRecord());
		metacard2.setTitle("Mary had a little");

		list.add(metacard2);

		MockMetacard metacard3 = new MockMetacard(Library.getShowLowRecord());
		metacard3.setTitle("Mary had a little l!@#$%^&*()_mb");

		list.add(metacard3);

		create(list);

		queryAndVerifyCount(1, filterBuilder.attribute(Metacard.TITLE).is().equalTo().text("Mary"));

		queryAndVerifyCount(0, filterBuilder.attribute(Metacard.TITLE).is().equalTo().text("Mar"));

		queryAndVerifyCount(0, filterBuilder.attribute(Metacard.TITLE).is().equalTo().text("Mary had"));

		queryAndVerifyCount(1, filterBuilder.attribute(Metacard.TITLE).is().equalTo().text("Mary had a little"));

		queryAndVerifyCount(1,
				filterBuilder.attribute(Metacard.TITLE).is().equalTo().text("Mary had a little l!@#$%^&*()_mb"));

		/* DATES */

		queryAndVerifyCount(1, filterBuilder.attribute(Metacard.EFFECTIVE).is().equalTo().date(exactEffectiveDate));

	}

	@Test(expected = UnsupportedQueryException.class)
	public void testPropertyIsEqualToCaseSensitive() throws Exception {
		deleteAllIn(provider);

		CommonQueryBuilder commonBuilder = new CommonQueryBuilder();

		// Expect an exception
		queryAndVerifyCount(0, commonBuilder.equalTo(Metacard.TITLE, "Mary", false));

	}

	@Test
	public void testTemporalDuring() throws Exception {

		deleteAllIn(provider);

		Metacard metacard = new MockMetacard(Library.getFlagstaffRecord());
		List<Metacard> list = Arrays.asList(metacard);

		/** CREATE **/
		create(list);

		/** TEMPORAL QUERY - DURING FILTER (Period) - AKA ABSOLUTE **/
		FilterFactory filterFactory = new FilterFactoryImpl();

		int minutes = 3;

		DateTime startDT = new DateTime().plusMinutes(ALL_RESULTS * minutes);

		DateTime endDT = new DateTime();

		CommonQueryBuilder queryBuilder = new CommonQueryBuilder();

		QueryImpl query = queryBuilder.during(Metacard.MODIFIED, startDT.toDate(), endDT.toDate());

		query.setStartIndex(1);

		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(1, sourceResponse.getResults().size());

		for (Result content : sourceResponse.getResults()) {
			String term = FLAGSTAFF_QUERY_PHRASE;

			LOGGER.debug("RESULT returned: " + content);
			String metadata = content.getMetacard().getMetadata();
			assertTrue("Testing if contents has term [" + term + "]", ALL_RESULTS != metadata.indexOf(term));
		}

		/** TEMPORAL QUERY - DURING FILTER (Duration) - AKA RELATIVE **/
		DefaultPeriodDuration duration = new DefaultPeriodDuration(minutes * MINUTES_IN_MILLISECONDS);

		Filter filter = filterFactory
				.during(filterFactory.property(Metacard.MODIFIED), filterFactory.literal(duration));

		query = new QueryImpl(filter);

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(1, sourceResponse.getResults().size());

		for (Result content : sourceResponse.getResults()) {
			String term = FLAGSTAFF_QUERY_PHRASE;

			LOGGER.debug("RESULT returned: " + content);
			String metadata = content.getMetacard().getMetadata();
			assertTrue("Testing if contents has term [" + term + "]", ALL_RESULTS != metadata.indexOf(term));
		}

		provider.isAvailable();
	}
	
    @Test
    public void testTextPathQuery() throws Exception {
        deleteAllIn(provider);

        MetacardImpl flagstaffMetacard = new MockMetacard(Library.getFlagstaffRecord());
        MetacardImpl poMetacard = new MockMetacard(Library.getPurchaseOrderRecord());
        List<Metacard> list = Arrays.asList((Metacard) flagstaffMetacard, (Metacard) poMetacard);

        /** CREATE **/
        create(list);

        /** POSITIVE **/
        testTextPathPositiveExists("/rss//item",FLAGSTAFF_QUERY_PHRASE);
        testTextPathPositiveExists("/purchaseOrder/comment", PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveExists("/purchaseOrder//comment", PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveExists("/purchaseOrder/items//comment", PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveExists("/purchaseOrder[items//comment]", PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveExists("/purchaseOrder/items/item/comment", PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveExists("/purchaseOrder//item/USPrice", PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveExists("/*/*/item[.//comment]", PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveExists("purchaseOrder/items/item", PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveExists("/purchaseOrder/items/item", PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveExists("./purchaseOrder/items/item", PURCHASE_ORDER_QUERY_PHRASE);
        
        testTextPathPositiveWithSearchPhrase("/purchaseOrder/comment", "Hurry, my lawn is going wild!",
                PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveWithSearchPhrase("/purchaseOrder/items//comment", "Confirm this is electric",
                PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveWithSearchPhrase("//comment", "Hurry, my lawn is going wild!", PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveWithSearchPhrase("//comment", "Confirm this is electric", PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveWithSearchPhrase("/purchaseOrder//item/USPrice", "148.95", PURCHASE_ORDER_QUERY_PHRASE);
        testTextPathPositiveWithSearchPhrase("/purchaseOrder//item/USPrice", "39.98", PURCHASE_ORDER_QUERY_PHRASE);
        
        /** NEGATIVE **/
        testTextPathNegativeExists("/*/invalid");
        testTextPathNegativeExists("//electric");
        testTextPathNegativeExists("//partNum");
        
        testTextPathNegativeWithSearchPhrase("/purchaseOrder/comment", "invalid");
        testTextPathNegativeWithSearchPhrase("//comment", "invalid");
        testTextPathNegativeWithSearchPhrase("/purchaseOrder//item/USPrice", "invalid");
        testTextPathNegativeWithSearchPhrase("/purchaseOrder/billTo", "95819");
    }
    
    public void testTextPathPositiveExists(String xpath, String recordMatchPhrase) throws Exception {
        SourceResponse sourceResponse = queryXpathExists(xpath);
        assertEquals("Failed to find record for xpath: " + xpath, 1, sourceResponse.getResults().size());

        for (Result r : sourceResponse.getResults()) {
            assertTrue("Wrong record, keyword was not found.", ALL_RESULTS != r.getMetacard().getMetadata()
                    .indexOf(recordMatchPhrase));
        }        
    }
    
    public void testTextPathNegativeExists(String xpath) throws Exception {
        SourceResponse sourceResponse = queryXpathExists(xpath);
        assertEquals("Should not have found record for xpath: " + xpath, 0, sourceResponse.getResults().size());       
    }

    private SourceResponse queryXpathExists(String xpath) throws UnsupportedQueryException {
        Filter filter = filterBuilder.xpath(xpath).exists();
        SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
        return sourceResponse;
    }
    
    public void testTextPathPositiveWithSearchPhrase(String xpath, String searchPhrase, String recordMatchPhrase)
            throws Exception {
        SourceResponse sourceResponse = queryXpathWithSearchPhrase(xpath, searchPhrase);
        assertEquals("Failed to find record for xpath: " + xpath, 1, sourceResponse.getResults().size());

        for (Result r : sourceResponse.getResults()) {
            assertTrue("Wrong record, keyword was not found.", ALL_RESULTS != r.getMetacard().getMetadata()
                    .indexOf(recordMatchPhrase));
        }        
    }
    
    public void testTextPathNegativeWithSearchPhrase(String xpath, String searchPhrase)
            throws Exception {
        SourceResponse sourceResponse = queryXpathWithSearchPhrase(xpath, searchPhrase);
        assertEquals("Should not have found record for xpath: " + xpath, 0, sourceResponse.getResults().size());
    }

    private SourceResponse queryXpathWithSearchPhrase(String xpath, String searchPhrase)
            throws UnsupportedQueryException {
        Filter filter = filterBuilder.xpath(xpath).is().like().text(searchPhrase);
        SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
        return sourceResponse;
    }    

	@Test
	public void testNumericalFields() throws Exception {
		deleteAllIn(provider);

		/* SETUP */
		String doubleField = "hertz";
		double doubleFieldValue = 16065.435;

		String floatField = "inches";
		float floatFieldValue = 4.435f;

		String intField = "count";
		int intFieldValue = 4;

		String longField = "milliseconds";
		long longFieldValue = 9876543293L;

		String shortField = "daysOfTheWeek";
		short shortFieldValue = 1;

		Set<AttributeDescriptor> descriptors = numericalDescriptors(doubleField, floatField, intField, longField,
				shortField);

		MetacardTypeImpl mType = new MetacardTypeImpl("numberMetacardType", descriptors);

		MetacardImpl customMetacard1 = new MetacardImpl(mType);
		customMetacard1.setAttribute(Metacard.ID, "");
		customMetacard1.setAttribute(doubleField, doubleFieldValue);
		customMetacard1.setAttribute(floatField, floatFieldValue);
		customMetacard1.setAttribute(intField, intFieldValue);
		customMetacard1.setAttribute(longField, longFieldValue);
		customMetacard1.setAttribute(shortField, shortFieldValue);

		create(Arrays.asList((Metacard) customMetacard1));

		// searching double field with int value
		greaterThanQueryAssertion(doubleField, intFieldValue, 1);

		// searching float field with double value
		greaterThanQueryAssertion(floatField, 4.0, 1);

		// searching long field with int value
		greaterThanQueryAssertion(longField, intFieldValue, 1);

		// searching int field with long value
		greaterThanQueryAssertion(intField, 3L, 1);

		// searching int field with long value
		greaterThanQueryAssertion(shortField, 0L, 1);
	}

	@Test()
	public void testNumericalOperations() throws Exception {

		deleteAllIn(provider);

		/* SETUP */
		String doubleField = "hertz";
		double doubleFieldValue = 16065.435;

		String floatField = "inches";
		float floatFieldValue = 4.435f;

		String intField = "count";
		int intFieldValue = 4;

		String longField = "milliseconds";
		long longFieldValue = 9876543293L;

		String shortField = "daysOfTheWeek";
		short shortFieldValue = 1;

		Set<AttributeDescriptor> descriptors = numericalDescriptors(doubleField, floatField, intField, longField,
				shortField);

		MetacardTypeImpl mType = new MetacardTypeImpl("anotherCustom", descriptors);

		MetacardImpl customMetacard1 = new MetacardImpl(mType);
		customMetacard1.setAttribute(Metacard.ID, "");
		customMetacard1.setAttribute(doubleField, doubleFieldValue);
		customMetacard1.setAttribute(floatField, floatFieldValue);
		customMetacard1.setAttribute(intField, intFieldValue);
		customMetacard1.setAttribute(longField, longFieldValue);
		customMetacard1.setAttribute(shortField, shortFieldValue);

		MetacardImpl customMetacard2 = new MetacardImpl(mType);
		customMetacard2.setAttribute(Metacard.ID, "");
		customMetacard2.setAttribute(doubleField, doubleFieldValue + 10.0);
		customMetacard2.setAttribute(floatField, (floatFieldValue + 10.0f));
		customMetacard2.setAttribute(intField, intFieldValue + 1);
		customMetacard2.setAttribute(longField, longFieldValue + 10L);
		customMetacard2.setAttribute(shortField, (shortFieldValue + 1));

		create(Arrays.asList((Metacard) customMetacard1, customMetacard2));

		// on exact DOUBLE
		greaterThanQueryAssertion(doubleField, doubleFieldValue, 1);
		greaterThanOrEqualToQueryAssertion(doubleField, doubleFieldValue, 2);

		// beyond the DOUBLE
		greaterThanQueryAssertion(doubleField, doubleFieldValue - 0.00000001, 2);
		greaterThanOrEqualToQueryAssertion(doubleField, doubleFieldValue - 0.00000001, 2);
		greaterThanQueryAssertion(doubleField, doubleFieldValue + 12.0, 0);
		greaterThanOrEqualToQueryAssertion(doubleField, doubleFieldValue + 12.0, 0);

		// on exact FLOAT
		greaterThanQueryAssertion(floatField, floatFieldValue, 1);
		greaterThanOrEqualToQueryAssertion(floatField, floatFieldValue, 2);

		// beyond the FLOAT
		greaterThanQueryAssertion(floatField, floatFieldValue - 0.00001f, 2);
		greaterThanOrEqualToQueryAssertion(floatField, floatFieldValue - 0.00001f, 2);
		greaterThanQueryAssertion(floatField, floatFieldValue + 12.0f, 0);
		greaterThanOrEqualToQueryAssertion(floatField, floatFieldValue + 12.0f, 0);

		// on exact LONG
		greaterThanQueryAssertion(longField, longFieldValue, 1);
		greaterThanOrEqualToQueryAssertion(longField, longFieldValue, 2);

		// beyond the LONG
		greaterThanQueryAssertion(longField, longFieldValue - 1L, 2);
		greaterThanOrEqualToQueryAssertion(longField, longFieldValue - 1L, 2);
		greaterThanQueryAssertion(longField, longFieldValue + 12L, 0);
		greaterThanOrEqualToQueryAssertion(longField, longFieldValue + 12L, 0);

		// on exact INT
		greaterThanQueryAssertion(intField, intFieldValue, 1);
		greaterThanOrEqualToQueryAssertion(intField, intFieldValue, 2);

		// beyond the INT
		greaterThanQueryAssertion(intField, intFieldValue - 1, 2);
		greaterThanOrEqualToQueryAssertion(intField, intFieldValue - 1, 2);
		greaterThanQueryAssertion(intField, intFieldValue + 2, 0);
		greaterThanOrEqualToQueryAssertion(intField, intFieldValue + 2, 0);

		// on exact SHORT
		greaterThanQueryAssertion(shortField, shortFieldValue, 1);
		greaterThanOrEqualToQueryAssertion(shortField, shortFieldValue, 2);

		// beyond the SHORT
		greaterThanQueryAssertion(shortField, (short) (shortFieldValue - 1), 2);
		greaterThanOrEqualToQueryAssertion(shortField, (short) (shortFieldValue - 1), 2);
		greaterThanQueryAssertion(shortField, (short) (shortFieldValue + 2), 0);
		greaterThanOrEqualToQueryAssertion(shortField, (short) (shortFieldValue + 2), 0);
	}

	private Set<AttributeDescriptor> numericalDescriptors(String doubleField, String floatField, String intField,
			String longField, String shortField) {
		Set<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
		descriptors.add(new AttributeDescriptorImpl(Metacard.ID, true, true, true, false, BasicTypes.STRING_TYPE));
		descriptors.add(new AttributeDescriptorImpl(doubleField, true, true, true, false, BasicTypes.DOUBLE_TYPE));
		descriptors.add(new AttributeDescriptorImpl(floatField, true, true, true, false, BasicTypes.FLOAT_TYPE));
		descriptors.add(new AttributeDescriptorImpl(intField, true, true, true, false, BasicTypes.INTEGER_TYPE));
		descriptors.add(new AttributeDescriptorImpl(longField, true, true, true, false, BasicTypes.LONG_TYPE));
		descriptors.add(new AttributeDescriptorImpl(shortField, true, true, true, false, BasicTypes.SHORT_TYPE));
		return descriptors;
	}

	private void greaterThanQueryAssertion(String fieldName, Serializable fieldValue, int count)
			throws UnsupportedQueryException {

		Filter filter = null;

		if (fieldValue instanceof Double) {
			filter = filterBuilder.attribute(fieldName).greaterThan().number((Double) fieldValue);
		} else if (fieldValue instanceof Integer) {
			filter = filterBuilder.attribute(fieldName).greaterThan().number((Integer) fieldValue);
		} else if (fieldValue instanceof Short) {
			filter = filterBuilder.attribute(fieldName).greaterThan().number((Short) fieldValue);
		} else if (fieldValue instanceof Long) {
			filter = filterBuilder.attribute(fieldName).greaterThan().number((Long) fieldValue);
		} else if (fieldValue instanceof Float) {
			filter = filterBuilder.attribute(fieldName).greaterThan().number((Float) fieldValue);
		}

		SourceResponse response = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertThat(response.getResults().size(), is(equalTo(count)));
	}

	private void greaterThanOrEqualToQueryAssertion(String fieldName, Serializable fieldValue, int count)
			throws UnsupportedQueryException {

		Filter filter = null;

		if (fieldValue instanceof Double) {
			filter = filterBuilder.attribute(fieldName).greaterThanOrEqualTo().number((Double) fieldValue);
		} else if (fieldValue instanceof Integer) {
			filter = filterBuilder.attribute(fieldName).greaterThanOrEqualTo().number((Integer) fieldValue);
		} else if (fieldValue instanceof Short) {
			filter = filterBuilder.attribute(fieldName).greaterThanOrEqualTo().number((Short) fieldValue);
		} else if (fieldValue instanceof Long) {
			filter = filterBuilder.attribute(fieldName).greaterThanOrEqualTo().number((Long) fieldValue);
		} else if (fieldValue instanceof Float) {
			filter = filterBuilder.attribute(fieldName).greaterThanOrEqualTo().number((Float) fieldValue);
		}

		SourceResponse response = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertThat(response.getResults().size(), is(equalTo(count)));
	}

	@Test()
	public void testTemporalBefore() throws Exception {

		deleteAllIn(provider);

		Metacard metacard = new MockMetacard(Library.getFlagstaffRecord());
		List<Metacard> list = Arrays.asList(metacard);

		create(list);

		/** POSITIVE CASE **/
		Filter filter = filterBuilder.attribute(Metacard.MODIFIED).before().date(new DateTime().plus(1).toDate());

		QueryImpl query = new QueryImpl(filter);

		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(1, sourceResponse.getResults().size());

		/** NEGATIVE CASE **/
		filter = filterBuilder.attribute(Metacard.MODIFIED).before().date(new DateTime().minus(10000).toDate());

		query = new QueryImpl(filter);

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(0, sourceResponse.getResults().size());

		/** Test Sort **/

	}

	/**
	 * Test for a specific IRAD problem.
	 * @throws Exception
	 */
	@Test
	public void testSortById() throws Exception {
		
		deleteAllIn(provider);
		
		List<Metacard> list = new ArrayList<Metacard>();

		DateTime now = new DateTime();

		for (int i = 0; i < 5; i++) {

			MockMetacard m = new MockMetacard(Library.getFlagstaffRecord());

			m.setEffectiveDate(now.minus(5L * i).toDate());

			m.setTitle("Record " + i);

			list.add(m);

		}

		create(list);
		
		Filter filter = filterBuilder.attribute(Metacard.EFFECTIVE).before().date(now.toDate());

		QueryImpl query = new QueryImpl(filter);

		query.setSortBy(new ddf.catalog.filter.SortByImpl(Metacard.ID, SortOrder.ASCENDING.name()));

		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(list.size(), sourceResponse.getResults().size());
		
		String currentId = "" ;
		
		for(Result r : sourceResponse.getResults() ){
			
			assertTrue(currentId.compareTo( r.getMetacard().getId() ) < 0 );
			currentId = r.getMetacard().getId();
		}
		
	}
	
	@Test
	public void testSorting() throws Exception {

		deleteAllIn(provider);

		List<Metacard> list = new ArrayList<Metacard>();

		DateTime now = new DateTime();

		for (int i = 0; i < 5; i++) {

			MockMetacard m = new MockMetacard(Library.getFlagstaffRecord());

			m.setEffectiveDate(now.minus(5L * i).toDate());

			m.setTitle("Record " + i);

			list.add(m);

		}

		create(list);

		Filter filter = null;
		QueryImpl query = null;
		SourceResponse sourceResponse = null;

		// Sort all TEMPORAL DESC

		filter = filterBuilder.attribute(Metacard.EFFECTIVE).before().date(now.toDate());

		query = new QueryImpl(filter);

		query.setSortBy(new ddf.catalog.filter.SortByImpl(Metacard.EFFECTIVE, SortOrder.DESCENDING.name()));

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(list.size(), sourceResponse.getResults().size());

		for (int i = 0; i < list.size(); i++) {
			Result r = sourceResponse.getResults().get(i);
			assertEquals("Record " + i, r.getMetacard().getTitle());
		}

		// Sort all TEMPORAL ASC

		query.setSortBy(new ddf.catalog.filter.SortByImpl(Metacard.EFFECTIVE, SortOrder.ASCENDING.name()));

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(list.size(), sourceResponse.getResults().size());

		int index = 0;
		for (int i = (list.size() - 1); i >= 0; i--) {
			Result r = sourceResponse.getResults().get(index);
			assertEquals("Record " + i, r.getMetacard().getTitle());
			index++;
		}

		// Sort all Relevancy score DESC

		filter = filterBuilder.attribute(Metacard.METADATA).like().text(FLAGSTAFF_QUERY_PHRASE);

		query = new QueryImpl(filter);

		query.setSortBy(new ddf.catalog.filter.SortByImpl(Result.RELEVANCE, SortOrder.DESCENDING.name()));

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(list.size(), sourceResponse.getResults().size());

		double currentScore = Integer.MAX_VALUE;
		for (Result r : sourceResponse.getResults()) {

			assertThat(currentScore, greaterThanOrEqualTo(r.getRelevanceScore()));
			currentScore = r.getRelevanceScore();
		}

		// Sort all Relevancy score DESC

		filter = filterBuilder.attribute(Metacard.METADATA).like().text(FLAGSTAFF_QUERY_PHRASE);

		query = new QueryImpl(filter);

		query.setSortBy(new ddf.catalog.filter.SortByImpl(Result.RELEVANCE, SortOrder.ASCENDING.name()));

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(list.size(), sourceResponse.getResults().size());

		currentScore = 0;
		for (Result r : sourceResponse.getResults()) {

			assertThat(currentScore, lessThanOrEqualTo(r.getRelevanceScore()));
			currentScore = r.getRelevanceScore();
		}
	}

	@Test
	public void testStartIndexWithSorting() throws Exception {
		deleteAllIn(provider);

		List<Metacard> metacards = new ArrayList<Metacard>();

		DateTime dt = new DateTime(1985, 1, 1, 1, 1, 1, 1, DateTimeZone.UTC);

		TreeSet<Date> calculatedDates = new TreeSet<Date>();

		for (int j = 0; j < 10; j++) {
			for (int i = 0; i < 100; i = i + 10) {

				MetacardImpl metacard = new MockMetacard(Library.getFlagstaffRecord());

				// ingest sporadically the effective dates so the default return
				// order won't be ordered
				Date calculatedDate = dt.plusDays(100 - i + 10 - j).toDate();
				calculatedDates.add(calculatedDate);
				metacard.setEffectiveDate(calculatedDate);
				metacards.add(metacard);

			}
		}

		// The TreeSet will sort them, the array will give me access to everyone
		// without an iterator
		Date[] dates = new Date[calculatedDates.size()];
		calculatedDates.toArray(dates);

		/** CREATE **/
		CreateResponse response = create(metacards);

		LOGGER.info("CREATED " + response.getCreatedMetacards().size() + " records.");

		CommonQueryBuilder queryBuilder = new CommonQueryBuilder();

		QueryImpl query = queryBuilder.queryByProperty(Metacard.CONTENT_TYPE, MockMetacard.DEFAULT_TYPE);

		int maxSize = 20;
		int startIndex = 2;

		// STARTINDEX=2, MAXSIZE=20
		query.setPageSize(maxSize);
		query.setStartIndex(startIndex);
		SortByImpl sortBy = new SortByImpl(queryBuilder.filterFactory.property(Metacard.EFFECTIVE),
				org.opengis.filter.sort.SortOrder.ASCENDING);
		query.setSortBy(sortBy);

		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(maxSize, sourceResponse.getResults().size());

		for (int i = 0; i < sourceResponse.getResults().size(); i++) {

			Result r = sourceResponse.getResults().get(i);

			Date effectiveDate = r.getMetacard().getEffectiveDate();

			DateTime currentDate = new DateTime(effectiveDate.getTime());

			LOGGER.debug("Testing current index: " + (startIndex + i));

			assertEquals(new DateTime(dates[startIndex - 1 + i].getTime()).getDayOfYear(), currentDate.getDayOfYear());

		}

		// STARTINDEX=20, MAXSIZE=5
		// a match-all queryByProperty
		query = queryBuilder.queryByProperty(Metacard.CONTENT_TYPE, MockMetacard.DEFAULT_TYPE);

		maxSize = 5;
		startIndex = 20;
		query.setPageSize(maxSize);
		query.setStartIndex(startIndex);
		sortBy = new SortByImpl(queryBuilder.filterFactory.property(Metacard.EFFECTIVE),
				org.opengis.filter.sort.SortOrder.ASCENDING);
		query.setSortBy(sortBy);

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(maxSize, sourceResponse.getResults().size());

		for (int i = 0; i < sourceResponse.getResults().size(); i++) {

			Result r = sourceResponse.getResults().get(i);

			Date effectiveDate = r.getMetacard().getEffectiveDate();

			DateTime currentDate = new DateTime(effectiveDate.getTime());

			LOGGER.debug("Testing current index: " + (startIndex + i));

			assertEquals(new DateTime(dates[startIndex - 1 + i].getTime()).getDayOfYear(), currentDate.getDayOfYear());

		}

		// STARTINDEX=80, MAXSIZE=20
		// a match-all queryByProperty
		query = queryBuilder.queryByProperty(Metacard.CONTENT_TYPE, MockMetacard.DEFAULT_TYPE);

		maxSize = 20;
		startIndex = 80;
		query.setPageSize(maxSize);
		query.setStartIndex(startIndex);
		sortBy = new SortByImpl(queryBuilder.filterFactory.property(Metacard.EFFECTIVE),
				org.opengis.filter.sort.SortOrder.ASCENDING);
		query.setSortBy(sortBy);

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(maxSize, sourceResponse.getResults().size());

		for (int i = 0; i < sourceResponse.getResults().size(); i++) {

			Result r = sourceResponse.getResults().get(i);

			Date effectiveDate = r.getMetacard().getEffectiveDate();

			DateTime currentDate = new DateTime(effectiveDate.getTime());

			LOGGER.debug("Testing current index: " + (startIndex + i));

			assertEquals(new DateTime(dates[startIndex - 1 + i].getTime()).getDayOfYear(), currentDate.getDayOfYear());

		}

		// STARTINDEX=1, MAXSIZE=100
		// a match-all queryByProperty
		query = queryBuilder.queryByProperty(Metacard.CONTENT_TYPE, MockMetacard.DEFAULT_TYPE);

		maxSize = 100;
		startIndex = 1;
		query.setPageSize(maxSize);
		query.setStartIndex(startIndex);
		sortBy = new SortByImpl(queryBuilder.filterFactory.property(Metacard.EFFECTIVE),
				org.opengis.filter.sort.SortOrder.ASCENDING);
		query.setSortBy(sortBy);

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(maxSize, sourceResponse.getResults().size());

		for (int i = 0; i < sourceResponse.getResults().size(); i++) {

			Result r = sourceResponse.getResults().get(i);

			Date effectiveDate = r.getMetacard().getEffectiveDate();

			DateTime currentDate = new DateTime(effectiveDate.getTime());

			LOGGER.debug("Testing current index: " + (startIndex + i));

			assertEquals(new DateTime(dates[startIndex - 1 + i].getTime()).getDayOfYear(), currentDate.getDayOfYear());

		}
	}

	/**
	 * Tests the offset aka start index (startIndex) functionality.
	 * 
	 * @param bundleContext
	 * @throws Exception
	 */
	@Test
	public void testStartIndex() throws Exception {

		deleteAllIn(provider);

		List<Metacard> list = Arrays.asList((Metacard) new MockMetacard(Library.getFlagstaffRecord()),
				(Metacard) new MockMetacard(Library.getFlagstaffRecord()),
				(Metacard) new MockMetacard(Library.getFlagstaffRecord()),
				(Metacard) new MockMetacard(Library.getFlagstaffRecord()),
				(Metacard) new MockMetacard(Library.getFlagstaffRecord()),
				(Metacard) new MockMetacard(Library.getFlagstaffRecord()),
				(Metacard) new MockMetacard(Library.getFlagstaffRecord()),
				(Metacard) new MockMetacard(Library.getFlagstaffRecord()),
				(Metacard) new MockMetacard(Library.getFlagstaffRecord()));

		/** CREATE **/
		create(list);

		/** CONTEXTUAL QUERY **/

		CommonQueryBuilder queryBuilder = new CommonQueryBuilder();

		QueryImpl query = queryBuilder.queryByProperty(Metacard.TITLE, FLAGSTAFF_QUERY_PHRASE);

		int index = 0;
		int maxSize = 9;
		int startIndex = 1;

		query.setPageSize(maxSize);
		query.setStartIndex(startIndex);
		query.setRequestsTotalResultsCount(true);

		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(9, sourceResponse.getResults().size());
		assertEquals(9L, sourceResponse.getHits());

		LinkedList<Result> allItems = new LinkedList<Result>();

		for (Result r : sourceResponse.getResults()) {
			allItems.add(r);
		}

		// 1
		maxSize = 1;
		startIndex = 2;
		index = startIndex - 1;

		query.setPageSize(maxSize);
		query.setStartIndex(startIndex);
		query.setRequestsTotalResultsCount(true);

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(ONE_HIT, sourceResponse.getResults().size());
		assertEquals(9L, sourceResponse.getHits());

		for (Result r : sourceResponse.getResults()) {

			assertEquals("Testing when startIndex = " + startIndex, allItems.get(index).getMetacard().getMetadata(), r
					.getMetacard().getMetadata());
			index++;
		}

		// 4
		maxSize = 1;
		startIndex = 4;
		index = startIndex - 1;
		query.setPageSize(maxSize);
		query.setStartIndex(startIndex);
		query.setRequestsTotalResultsCount(false);

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(ONE_HIT, sourceResponse.getResults().size());
		assertThat(sourceResponse.getHits(), anyOf(equalTo(-1L), equalTo(9L)));

		for (Result r : sourceResponse.getResults()) {

			assertEquals("Testing when startIndex = " + startIndex, allItems.get(index).getMetacard().getMetadata(), r
					.getMetacard().getMetadata());
			index++;
		}

		// 5
		maxSize = 5;
		startIndex = 5;
		index = startIndex - 1;
		query.setPageSize(maxSize);
		query.setStartIndex(startIndex);

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(5, sourceResponse.getResults().size());

		for (Result r : sourceResponse.getResults()) {

			assertEquals("Testing when startIndex = " + startIndex, allItems.get(index).getMetacard().getMetadata(), r
					.getMetacard().getMetadata());
			index++;
		}

		// 9
		maxSize = 9;
		startIndex = 9;
		index = startIndex - 1;
		query.setPageSize(maxSize);
		query.setStartIndex(startIndex);

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(ONE_HIT, sourceResponse.getResults().size());

		for (Result r : sourceResponse.getResults()) {

			assertEquals("Testing when startIndex = " + startIndex, allItems.get(index).getMetacard().getMetadata(), r
					.getMetacard().getMetadata());
			index++;
		}

		// Max size is very large
		maxSize = 100;
		startIndex = 9;
		index = startIndex - 1;
		query.setPageSize(maxSize);
		query.setStartIndex(startIndex);

		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(ONE_HIT, sourceResponse.getResults().size());

		for (Result r : sourceResponse.getResults()) {

			assertEquals(allItems.get(index).getMetacard().getMetadata(), r.getMetacard().getMetadata());
			index++;
		}

		// bad start index
		maxSize = 2;
		startIndex = ALL_RESULTS;
		index = startIndex - 1;
		query.setPageSize(maxSize);
		query.setStartIndex(startIndex);

		try {
			sourceResponse = provider.query(new QueryRequestImpl(query));
			Assert.fail("Expected an exception stating that the start index should be greater than 0. ");
		} catch (UnsupportedQueryException e) {
			assertTrue(e.getMessage().indexOf("greater than 0") != ALL_RESULTS);
		}

	}
	
	@Test
	public void testSpatialPointRadius() throws Exception {

		deleteAllIn(provider);
		MetacardImpl metacard1 = new MockMetacard(Library.getFlagstaffRecord());
		MetacardImpl metacard2 = new MockMetacard(Library.getTampaRecord());
		MetacardImpl metacard3 = new MockMetacard(Library.getShowLowRecord());

		// Add in the geometry
		metacard1.setLocation(FLAGSTAFF_AIRPORT_POINT_WKT);
		metacard2.setLocation(TAMPA_AIRPORT_POINT_WKT);
		metacard3.setLocation(SHOW_LOW_AIRPORT_POINT_WKT);

		List<Metacard> list = Arrays.asList((Metacard) metacard1, metacard2, metacard3);

		/** CREATE **/
		create(list);
		
		Filter filter = filterBuilder.attribute(Metacard.ID).is().like().text("*");
		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(
				new QueryImpl(filter)));
		assertEquals("Failed to find all records.", 3, sourceResponse.getResults().size());
		
		CommonQueryBuilder builder = new CommonQueryBuilder();

		// Right on Flagstaff
		QueryImpl query = builder.pointRadius(-111.67121887207031, 35.138454437255859, 10.0);
		query.setPageSize(1);
		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals("Failed to find Flagstaff record only.", 1, sourceResponse.getResults().size());

		for (Result r : sourceResponse.getResults()) {
			assertTrue("Wrong record, Flagstaff keyword was not found.", ALL_RESULTS != r.getMetacard().getMetadata()
					.indexOf(FLAGSTAFF_QUERY_PHRASE));
			LOGGER.info("Distance to Flagstaff: " + r.getDistanceInMeters());
			// assertTrue(r.getDistanceInMeters() != null);
		}

		// Right on Flagstaff, finding 2 records with 195 km radius
		query = builder.pointRadius(-111.67121887207031, 35.138454437255859, 195000);
		query.setSortBy(new ddf.catalog.filter.SortByImpl("foo", SortOrder.ASCENDING));
		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals("Failed to find the two records.", 2, sourceResponse.getResults().size());

		ArrayList<Result> results = new ArrayList<Result>();
		for (Result r : sourceResponse.getResults()) {
			results.add(r);
		}

		// must be in order because that was specified by the Sortby in the
		// querybuilder
		for (int i = 0; i < 2; i++) {
			Result result = results.get(i);

			LOGGER.info("Distance of [" + i + "]: " + result.getDistanceInMeters());

			if (i == 0) {
				assertTrue("Grabbed the wrong record.",
						ALL_RESULTS != result.getMetacard().getMetadata().indexOf(FLAGSTAFF_QUERY_PHRASE));
			}
			if (i == 1) {
				assertTrue("Grabbed the wrong record - should be Show Low.", ALL_RESULTS != result.getMetacard()
						.getMetadata().indexOf("Show Low"));
			}
		}

		// NEGATIVE CASE
		query = builder.pointRadius(80.1, 25, 195000);
		query.setPageSize(3);
		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals("Should have not found any records.", 0, sourceResponse.getResults().size());

		// FEET
		double[] coords = { -111.67121887207031, 35.138454437255859 };
		query = new QueryImpl(builder.filterFactory.dwithin(Metacard.ANY_GEO, new PointImpl(new DirectPositionImpl(
				coords), DefaultGeographicCRS.WGS84), 195000, UomOgcMapping.FOOT.name()));

		query.setStartIndex(1);

		SortByImpl sortby = new SortByImpl(builder.filterFactory.property(Result.DISTANCE),
				org.opengis.filter.sort.SortOrder.ASCENDING);
		query.setSortBy(sortby);
		query.setPageSize(3);
		sourceResponse = provider.query(new QueryRequestImpl(query));

		assertEquals(1, sourceResponse.getResults().size());
	}
	

    @Test
    public void testSpatialNearestNeighbor() throws Exception {
        deleteAllIn(provider);
        
        MetacardImpl metacard1 = new MockMetacard(Library.getFlagstaffRecord());
        MetacardImpl metacard2 = new MockMetacard(Library.getTampaRecord());
        MetacardImpl metacard3 = new MockMetacard(Library.getShowLowRecord());

        // Add in the geometry
        metacard1.setLocation(FLAGSTAFF_AIRPORT_POINT_WKT);
        metacard2.setLocation(TAMPA_AIRPORT_POINT_WKT);
        metacard3.setLocation(SHOW_LOW_AIRPORT_POINT_WKT);

        List<Metacard> list = Arrays.asList((Metacard) metacard1, metacard2, metacard3);
        create(list);
        
        // Ascending
        Filter positiveFilter = filterBuilder.attribute(Metacard.GEOGRAPHY).beyond().wkt(PHOENIX_POINT_WKT, 0);
        QueryImpl query = new QueryImpl(positiveFilter);
        SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

        assertEquals("Failed to find two records within 1000 nautical miles.", 2, sourceResponse.getResults().size());
        assertTrue("Flagstaff record was not first in ascending order.", sourceResponse.getResults().get(0)
                .getMetacard().getMetadata().indexOf(FLAGSTAFF_QUERY_PHRASE) > 0);

        // Descending
        SortBy sortby = new ddf.catalog.filter.SortByImpl(Result.DISTANCE, org.opengis.filter.sort.SortOrder.DESCENDING);
        query.setSortBy(sortby);
        sourceResponse = provider.query(new QueryRequestImpl(query));
        
        assertEquals("Failed to find two records within 1000 nautical miles.", 2, sourceResponse.getResults().size());
        assertTrue("Flagstaff record was not last in descending order.", sourceResponse.getResults().get(1)
                .getMetacard().getMetadata().indexOf(FLAGSTAFF_QUERY_PHRASE) > 0);
        
        // Using WKT polygon
        positiveFilter = filterBuilder.attribute(Metacard.GEOGRAPHY).beyond().wkt(ARIZONA_POLYGON_WKT, 0);
        query = new QueryImpl(positiveFilter);
        sourceResponse = provider.query(new QueryRequestImpl(query));

        assertEquals("Failed to find two records based on polygon centroid.", 2, sourceResponse.getResults().size());
    }
	
    @Test
    public void testSpatialDistanceWithinPolygon() throws Exception {
        Filter positiveFilter = filterBuilder.attribute(Metacard.GEOGRAPHY).withinBuffer()
                .wkt(ARIZONA_POLYGON_WKT, 50 * METERS_PER_KM);
        Filter negativeFilter = filterBuilder.attribute(Metacard.GEOGRAPHY).withinBuffer()
                .wkt(ARIZONA_POLYGON_WKT, 10 * METERS_PER_KM);
        testSpatialWithWkt(LAS_VEGAS_POINT_WKT, positiveFilter, negativeFilter);
    }

    @Test
    public void testSpatialDistanceCalculation_ExactPoint() throws Exception {
        deleteAllIn(provider);

        // given
        double radiusInKilometers = 50;
        double radiusInMeters = radiusInKilometers * METERS_PER_KM;
        Filter positiveFilter = filterBuilder.attribute(Metacard.GEOGRAPHY)
                .withinBuffer().wkt(LAS_VEGAS_POINT_WKT, radiusInMeters);

        MetacardImpl metacard = new MockMetacard(Library.getFlagstaffRecord());
        metacard.setLocation(LAS_VEGAS_POINT_WKT);
        List<Metacard> list = Arrays.asList((Metacard) metacard);

        create(list);

        QueryImpl query = new QueryImpl(positiveFilter);
        query.setSortBy(new ddf.catalog.filter.SortByImpl(Result.DISTANCE,
                SortOrder.ASCENDING));

        // when
        SourceResponse sourceResponse = provider.query(new QueryRequestImpl(
                query));

        // then
        assertEquals("Failed to find metacard WKT with filter", 1,
                sourceResponse.getResults().size());
        Result result = sourceResponse.getResults().get(0);

        assertThat(result.getDistanceInMeters(), is(notNullValue()));
        assertThat("Point radius search should be less than the radius given.",
                result.getDistanceInMeters(),
                is(lessThanOrEqualTo(radiusInMeters)));

        double oneMeter = 1.0;
        assertThat(
                "The distance should be close to zero since we are right upon the point.",
                result.getDistanceInMeters(), is(lessThanOrEqualTo(oneMeter)));

    }
    
    @Test
    public void testSpatialDistanceCalculation_BetweenTwoPoints() throws Exception {
        deleteAllIn(provider);

        // given
        double radiusInKilometers = 500 ;
        double radiusInMeters = radiusInKilometers * METERS_PER_KM;
        Filter positiveFilter = filterBuilder.attribute(Metacard.GEOGRAPHY)
                .withinBuffer().wkt(PHOENIX_POINT_WKT, radiusInMeters);

        MetacardImpl metacard = new MockMetacard(Library.getFlagstaffRecord());
        metacard.setLocation(LAS_VEGAS_POINT_WKT);
        List<Metacard> list = Arrays.asList((Metacard) metacard);

        create(list);

        QueryImpl query = new QueryImpl(positiveFilter);
        query.setSortBy(new ddf.catalog.filter.SortByImpl(Result.DISTANCE,
                SortOrder.ASCENDING));

        // when
        SourceResponse sourceResponse = provider.query(new QueryRequestImpl(
                query));

        // then
        assertEquals("Failed to find metacard WKT with filter", 1,
                sourceResponse.getResults().size());
        Result result = sourceResponse.getResults().get(0);

        assertThat(result.getDistanceInMeters(), is(notNullValue()));
        assertThat("Point radius search should be less than the radius given.",
                result.getDistanceInMeters(),
                is(lessThanOrEqualTo(radiusInMeters)));

        // expected distance calculated from
        // http://www.movable-type.co.uk/scripts/latlong.html
        double expectedDistanceBetweenCitiesInMeters = 412700;
        double precisionPercentage = .001; // +/-0.1%
        double lowerBound = expectedDistanceBetweenCitiesInMeters
                * (1 - precisionPercentage);
        double upperBound = expectedDistanceBetweenCitiesInMeters
                * (1 + precisionPercentage);

        assertThat(
                "The distance returned should at least be above the lower bound of error.",
                result.getDistanceInMeters(),
                is(greaterThanOrEqualTo(lowerBound)));
        assertThat(
                "The distance returned should at least be below the upper bound of error.",
                result.getDistanceInMeters(), is(lessThanOrEqualTo(upperBound)));

    }
    
    @Test
    public void testSpatialWithin() throws Exception {
        Filter positiveFilter = filterBuilder.attribute(Metacard.GEOGRAPHY).within().wkt(ARIZONA_POLYGON_WKT);
        Filter negativeFilter = filterBuilder.attribute(Metacard.GEOGRAPHY).within().wkt(GULF_OF_GUINEA_POLYGON_WKT);
        testSpatialWithWkt(FLAGSTAFF_AIRPORT_POINT_WKT, positiveFilter, negativeFilter);
    }
	
	@Test
	public void testSpatialQueryWithClockwiseRectangle() throws Exception {
		deleteAllIn(provider);

		MetacardImpl metacard = new MockMetacard(Library.getFlagstaffRecord());
		metacard.setLocation(FLAGSTAFF_AIRPORT_POINT_WKT);
		List<Metacard> list = Arrays.asList((Metacard) metacard);

		/** CREATE **/
		create(list);

		/** POSITIVE **/
		Filter filter = filterBuilder
				.attribute(Metacard.GEOGRAPHY)
				.intersecting()
				.wkt(CLOCKWISE_ARIZONA_RECTANGLE_WKT);
		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Failed to find Flagstaff record.", 1, sourceResponse.getResults().size());

		for (Result r : sourceResponse.getResults()) {
			assertTrue("Wrong record, Flagstaff keyword was not found.", ALL_RESULTS != r.getMetacard().getMetadata()
					.indexOf(FLAGSTAFF_QUERY_PHRASE));
		}
	}
	
	@Test
	public void testSpatialCreateAndUpdateWithClockwiseRectangle() throws Exception {
        deleteAllIn(provider);

        /** CREATE **/
        MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());
        metacard.setLocation(CLOCKWISE_ARIZONA_RECTANGLE_WKT);

        CreateResponse createResponse = create(Arrays.asList((Metacard) metacard));
        assertEquals(1, createResponse.getCreatedMetacards().size());
        
        Filter filter = filterBuilder
                .attribute(Metacard.GEOGRAPHY)
                .intersecting()
                .wkt(FLAGSTAFF_AIRPORT_POINT_WKT);
        SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

        assertEquals("Failed to find correct record.", 1, sourceResponse.getResults().size());

        /** UPDATE **/
        MockMetacard updatedMetacard = new MockMetacard(Library.getTampaRecord());
        updatedMetacard.setLocation(CLOCKWISE_ARIZONA_RECTANGLE_WKT);

        String[] ids = { metacard.getId() };
        UpdateResponse updateResponse = update(ids, Arrays.asList((Metacard) updatedMetacard));
        assertEquals(1, updateResponse.getUpdatedMetacards().size());
        
        filter = filterBuilder
                .attribute(Metacard.GEOGRAPHY)
                .intersecting()
                .wkt(FLAGSTAFF_AIRPORT_POINT_WKT);
        sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

        assertEquals("Failed to find correct record.", 1, sourceResponse.getResults().size());
	}
	
	@Test
	public void testSpatialQueryWithCounterClockwiseRectangle() throws Exception {
		deleteAllIn(provider);

		MetacardImpl metacard = new MockMetacard(Library.getFlagstaffRecord());
		metacard.setLocation(FLAGSTAFF_AIRPORT_POINT_WKT);
		List<Metacard> list = Arrays.asList((Metacard) metacard);

		/** CREATE **/
		create(list);

		/** POSITIVE **/
		Filter filter = filterBuilder
				.attribute(Metacard.GEOGRAPHY)
				.intersecting()
				.wkt(COUNTERCLOCKWISE_ARIZONA_RECTANGLE_WKT);
		SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

		assertEquals("Failed to find Flagstaff record.", 1, sourceResponse.getResults().size());

		for (Result r : sourceResponse.getResults()) {
			assertTrue("Wrong record, Flagstaff keyword was not found.", ALL_RESULTS != r.getMetacard().getMetadata()
					.indexOf(FLAGSTAFF_QUERY_PHRASE));
		}
	}

    @Test
    public void testSpatialPointIntersectsPoint() throws Exception {
        testSpatialIntersectsWithWkt(FLAGSTAFF_AIRPORT_POINT_WKT,
                FLAGSTAFF_AIRPORT_POINT_WKT, GULF_OF_GUINEA_POINT_WKT);
    }

    @Test
    public void testSpatialMultiPointIntersectsPoint() throws Exception {
        testSpatialIntersectsWithWkt(GULF_OF_GUINEA_POINT_WKT,
                GULF_OF_GUINEA_MULTIPOINT_WKT, FLAGSTAFF_AIRPORT_POINT_WKT);
    }

    @Test
    public void testSpatialMultiPointSingleIntersectsPoint() throws Exception {
        testSpatialIntersectsWithWkt(GULF_OF_GUINEA_POINT_WKT,
                GULF_OF_GUINEA_MULTIPOINT_SINGLE_WKT,
                FLAGSTAFF_AIRPORT_POINT_WKT);
    }

    @Test
    public void testSpatialPolygonIntersectsPoint() throws Exception {
        testSpatialIntersectsWithWkt(ARIZONA_POLYGON_WKT,
                FLAGSTAFF_AIRPORT_POINT_WKT, GULF_OF_GUINEA_POINT_WKT);
    }

    @Test
    public void testSpatialPolygonIntersectsLineString() throws Exception {
        testSpatialIntersectsWithWkt(ARIZONA_POLYGON_WKT, ARIZONA_INTERSECTING_LINESTING_WKT,
                GULF_OF_GUINEA_LINESTRING_WKT);
    }

    @Test
    public void testSpatialPolygonIntersectsPolygon() throws Exception {
        testSpatialIntersectsWithWkt(ARIZONA_POLYGON_WKT, ARIZONA_INTERSECTING_POLYGON_WKT,
                GULF_OF_GUINEA_POLYGON_WKT);
    }
    
    @Test
    public void testSpatialPolygonIntersectsMultiPoint() throws Exception {
        testSpatialIntersectsWithWkt(ARIZONA_POLYGON_WKT, PHOENIX_AND_LAS_VEGAS_MULTIPOINT_WKT,
                GULF_OF_GUINEA_MULTIPOINT_WKT);
    }
    
    @Test
    public void testSpatialPolygonIntersectsMultiLineString() throws Exception {
        testSpatialIntersectsWithWkt(ARIZONA_POLYGON_WKT, ARIZONA_INTERSECTING_MULTILINESTING_WKT,
                GULF_OF_GUINEA_MULTILINESTRING_WKT);
    }
    
    @Test
    public void testSpatialPolygonIntersectsMultiPolygon() throws Exception {
        testSpatialIntersectsWithWkt(ARIZONA_POLYGON_WKT, ARIZONA_INTERSECTING_MULTIPOLYGON_WKT,
                GULF_OF_GUINEA_MULTIPOLYGON_WKT);
    }
    
    @Test
    @Ignore // GeometryCollection is not supported by Spatial4j at this time
    public void testSpatialPolygonIntersectsGeometryCollection() throws Exception {
        testSpatialIntersectsWithWkt(ARIZONA_POLYGON_WKT, ARIZONA_INTERSECTING_GEOMETRYCOLLECTION_WKT,
                GULF_OF_GUINEA_GEOMETRYCOLLECTION_WKT);
    }

    @Test
    public void testSpatialPointWithinPolygon() throws Exception {
        testSpatialWithinWithWkt(FLAGSTAFF_AIRPORT_POINT_WKT, WEST_USA_CONTAINING_POLYGON_WKT,
                GULF_OF_GUINEA_POLYGON_WKT);
    }

    @Test
    public void testSpatialLineStringWithinPolygon() throws Exception {
        testSpatialWithinWithWkt(ARIZONA_INTERSECTING_LINESTING_WKT, WEST_USA_CONTAINING_POLYGON_WKT,
                GULF_OF_GUINEA_POLYGON_WKT);
    }

    @Test
    public void testSpatialPolygonWithinPolygon() throws Exception {
        testSpatialWithinWithWkt(ARIZONA_INTERSECTING_POLYGON_WKT, WEST_USA_CONTAINING_POLYGON_WKT,
                GULF_OF_GUINEA_POLYGON_WKT);
    }

    @Test
    public void testSpatialMultiPointWithinPolygon() throws Exception {
        testSpatialWithinWithWkt(PHOENIX_AND_LAS_VEGAS_MULTIPOINT_WKT, WEST_USA_CONTAINING_POLYGON_WKT,
                GULF_OF_GUINEA_POLYGON_WKT);
    }

    @Test
    public void testSpatialMultiLineStringWithinPolygon() throws Exception {
        testSpatialWithinWithWkt(ARIZONA_INTERSECTING_MULTILINESTING_WKT, WEST_USA_CONTAINING_POLYGON_WKT,
                GULF_OF_GUINEA_POLYGON_WKT);
    }

    @Test
    public void testSpatialMultiPolygonWithinPolygon() throws Exception {
        testSpatialWithinWithWkt(ARIZONA_INTERSECTING_MULTIPOLYGON_WKT, WEST_USA_CONTAINING_POLYGON_WKT,
                GULF_OF_GUINEA_POLYGON_WKT);
    }

    @Test
    @Ignore // GeometryCollection is not supported by Spatial4j at this time
    public void testSpatialGeometryCollectionWithinPolygon() throws Exception {
        testSpatialWithinWithWkt(ARIZONA_INTERSECTING_GEOMETRYCOLLECTION_WKT, WEST_USA_CONTAINING_POLYGON_WKT,
                GULF_OF_GUINEA_POLYGON_WKT);
    }

    @Test
    public void testSpatialPolygonContainsPoint() throws Exception {
        Filter positiveFilter = filterBuilder.attribute(Metacard.GEOGRAPHY).containing()
                .wkt(FLAGSTAFF_AIRPORT_POINT_WKT);
        Filter negativeFilter = filterBuilder.attribute(Metacard.GEOGRAPHY).containing().wkt(GULF_OF_GUINEA_POINT_WKT);
        testSpatialWithWkt(ARIZONA_POLYGON_WKT, positiveFilter, negativeFilter);
    }

    @Test
    public void testSpatialAnyGeo() throws Exception {
        Filter positiveFilter = filterBuilder.attribute(Metacard.ANY_GEO).within().wkt(ARIZONA_POLYGON_WKT);
        Filter negativeFilter = filterBuilder.attribute(Metacard.ANY_GEO).within().wkt(GULF_OF_GUINEA_POLYGON_WKT);
        testSpatialWithWkt(FLAGSTAFF_AIRPORT_POINT_WKT, positiveFilter, negativeFilter);
    }

    private void testSpatialWithinWithWkt(String metacardWkt, String positiveWkt, String negativeWkt)
            throws Exception {
        Filter positiveFilter = filterBuilder.attribute(Metacard.ANY_GEO).within().wkt(positiveWkt);
        Filter negativeFilter = filterBuilder.attribute(Metacard.ANY_GEO).within().wkt(negativeWkt);
        testSpatialWithWkt(metacardWkt, positiveFilter, negativeFilter);
    }
    
    private void testSpatialIntersectsWithWkt(String metacardWkt, String positiveWkt, String negativeWkt)
            throws Exception {
        Filter positiveFilter = filterBuilder.attribute(Metacard.ANY_GEO).intersecting().wkt(positiveWkt);
        Filter negativeFilter = filterBuilder.attribute(Metacard.ANY_GEO).intersecting().wkt(negativeWkt);
        testSpatialWithWkt(metacardWkt, positiveFilter, negativeFilter);

        positiveFilter = filterBuilder.attribute(Metacard.ANY_GEO).intersecting().wkt(metacardWkt);
        testSpatialWithWkt(positiveWkt, positiveFilter, negativeFilter);
    }
	
    private void testSpatialWithWkt(String metacardWkt, Filter positiveFilter, Filter negativeFilter) throws Exception {
        deleteAllIn(provider, 4);

        MetacardImpl metacard = new MockMetacard(Library.getFlagstaffRecord());
        metacard.setLocation(metacardWkt);
        List<Metacard> list = Arrays.asList((Metacard) metacard);

        create(list);

        SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(positiveFilter)));
        assertEquals("Failed to find metacard WKT with filter", 1, sourceResponse.getResults().size());

        sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(negativeFilter)));
        assertEquals("Should not have found metacard record.", 0, sourceResponse.getResults().size());
    }
	
	@Test
	public void testGetContentTypesSimple() throws Exception {

		deleteAllIn(provider);

		MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
		MockMetacard metacard2 = new MockMetacard(Library.getShowLowRecord());
		MockMetacard metacard3 = new MockMetacard(Library.getTampaRecord());

		metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_1);
		metacard2.setContentTypeName(SAMPLE_CONTENT_TYPE_2);
		metacard3.setContentTypeName(SAMPLE_CONTENT_TYPE_2);
		metacard3.setContentTypeVersion(SAMPLE_CONTENT_VERSION_3);

		List<Metacard> list = Arrays.asList((Metacard) metacard1, metacard2, metacard3);

		create(list);

		Set<ContentType> contentTypes = provider.getContentTypes();
		assertEquals(3, contentTypes.size());

		assertThat(contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_1,
				MockMetacard.DEFAULT_VERSION)));
		assertThat(contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_2,
				MockMetacard.DEFAULT_VERSION)));
		assertThat(contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_2,
				SAMPLE_CONTENT_VERSION_3)));

	}

	@Test
	public void testGetContentTypesComplicated() throws Exception {

		deleteAllIn(provider);

		List<Metacard> list = new ArrayList<Metacard>();

		// Single content type and version
		MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
		metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_1);
		metacard1.setContentTypeVersion(SAMPLE_CONTENT_VERSION_1);
		list.add(metacard1);

		// one content type with multiple versions
		metacard1 = new MockMetacard(Library.getFlagstaffRecord());
		metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_2);
		metacard1.setContentTypeVersion(SAMPLE_CONTENT_VERSION_1);
		list.add(metacard1);
		MockMetacard metacard2 = new MockMetacard(Library.getFlagstaffRecord());
		metacard2.setContentTypeName(SAMPLE_CONTENT_TYPE_2);
		metacard2.setContentTypeVersion(SAMPLE_CONTENT_VERSION_2);
		list.add(metacard2);

		// multiple records with different content type but same version
		metacard1 = new MockMetacard(Library.getFlagstaffRecord());
		metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_3);
		metacard1.setContentTypeVersion(SAMPLE_CONTENT_VERSION_3);
		list.add(metacard1);
		metacard2 = new MockMetacard(Library.getFlagstaffRecord());
		metacard2.setContentTypeName(SAMPLE_CONTENT_TYPE_3);
		metacard2.setContentTypeVersion(SAMPLE_CONTENT_VERSION_4);
		list.add(metacard2);

		// multiple records with different content type and different version
		metacard1 = new MockMetacard(Library.getFlagstaffRecord());
		metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_4);
		metacard1.setContentTypeVersion(SAMPLE_CONTENT_VERSION_1);
		list.add(metacard1);
		metacard2 = new MockMetacard(Library.getFlagstaffRecord());
		metacard2.setContentTypeName(SAMPLE_CONTENT_TYPE_1);
		metacard2.setContentTypeVersion(SAMPLE_CONTENT_VERSION_4);
		list.add(metacard2);
		metacard1 = new MockMetacard(Library.getFlagstaffRecord());
		metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_4);
		metacard1.setContentTypeVersion(SAMPLE_CONTENT_VERSION_1);
		list.add(metacard1);
		metacard2 = new MockMetacard(Library.getFlagstaffRecord());
		metacard2.setContentTypeName(SAMPLE_CONTENT_TYPE_1);
		metacard2.setContentTypeVersion(SAMPLE_CONTENT_VERSION_4);
		list.add(metacard2);

		create(list);

		Set<ContentType> contentTypes = provider.getContentTypes();
		assertEquals(7, contentTypes.size());

		assertThat(contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_1,
				SAMPLE_CONTENT_VERSION_1)));
		assertThat(contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_2,
				SAMPLE_CONTENT_VERSION_1)));
		assertThat(contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_2,
				SAMPLE_CONTENT_VERSION_2)));
		assertThat(contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_3,
				SAMPLE_CONTENT_VERSION_3)));
		assertThat(contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_3,
				SAMPLE_CONTENT_VERSION_4)));
		assertThat(contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_4,
				SAMPLE_CONTENT_VERSION_1)));
		assertThat(contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_1,
				SAMPLE_CONTENT_VERSION_4)));

	}

	@Test
	public void testGetContentTypesOne() throws Exception {

		deleteAllIn(provider);

		MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());

		metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_1);

		List<Metacard> list = Arrays.asList((Metacard) metacard1);

		create(list);

		Set<ContentType> contentTypes = provider.getContentTypes();
		assertEquals(1, contentTypes.size());

		assertThat(contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_1,
				MockMetacard.DEFAULT_VERSION)));

	}

	@Test
	public void testGetContentTypesNone() throws Exception {

		deleteAllIn(provider);

		assertEquals(0, provider.getContentTypes().size());

	}

	@Test
	public void testIsAvalaible() throws Exception {

		assertTrue(provider.isAvailable());

	}

	/**
	 * Test that makes sure sourceId is returned for deletions, creates, and
	 * updates.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSourceId() throws Exception {

		assertThat(provider.getId(), notNullValue());

		// need more here, how can we test this further

	}

	@Test
	public void testDescribable() throws Exception {

		LOGGER.debug("version: " + provider.getVersion());

		LOGGER.debug("description: " + provider.getDescription());

		LOGGER.debug("org: " + provider.getOrganization());

		LOGGER.debug("name: " + provider.getTitle());

		assertNotNull(provider.getOrganization());

		assertNotNull(provider.getVersion());

		assertNotNull(provider.getDescription());

		assertNotNull(provider.getOrganization());

		assertNotNull(provider.getTitle());

	}

	private void verifyDeletedRecord(MockMetacard metacard, CreateResponse createResponse,
			DeleteResponse deleteResponse, Metacard deletedMetacard) {
		assertEquals(1, deleteResponse.getDeletedMetacards().size());
		assertEquals(createResponse.getCreatedMetacards().get(0).getId(), deletedMetacard.getId());
		assertEquals(MockMetacard.DEFAULT_TITLE, deletedMetacard.getTitle());
		assertEquals(MockMetacard.DEFAULT_LOCATION, deletedMetacard.getLocation());
		assertEquals(MockMetacard.DEFAULT_TYPE, deletedMetacard.getContentTypeName());
		assertEquals(MockMetacard.DEFAULT_VERSION, deletedMetacard.getContentTypeVersion());
		assertNotNull(deletedMetacard.getMetadata());
		assertTrue(!deletedMetacard.getMetadata().isEmpty());
		assertFalse(deletedMetacard.getCreatedDate().after(new Date()));
		assertFalse(deletedMetacard.getModifiedDate().after(new Date()));
		assertEquals(metacard.getEffectiveDate(), deletedMetacard.getEffectiveDate());
		assertEquals(metacard.getExpirationDate(), deletedMetacard.getExpirationDate());
		assertTrue(Arrays.equals(metacard.getThumbnail(), deletedMetacard.getThumbnail()));
		assertEquals(metacard.getLocation(), deletedMetacard.getLocation());
	}
}
