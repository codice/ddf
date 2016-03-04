/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package ddf.test.itests.catalog;

import static org.boon.Maps.idxMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.given;
import static ddf.test.itests.common.CswQueryBuilder.AND;
import static ddf.test.itests.common.CswQueryBuilder.PROPERTY_IS_LIKE;

import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.core.MediaType;

import org.boon.json.JsonFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.HistoryMetacardImpl;
import ddf.common.test.BeforeExam;
import ddf.test.itests.AbstractIntegrationTest;
import ddf.test.itests.common.CswQueryBuilder;
import ddf.test.itests.common.Library;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestVersioning extends AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestVersioning.class);

    private static Function<String, Map<String, Object>> jsonToMap =
            (String s) -> JsonFactory.create()
                    .parser()
                    .parseMap(s);

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            basePort = getBasePort();
            getAdminConfig().setLogLevels();
            getServiceManager().waitForRequiredApps(getDefaultRequiredApps());
            getServiceManager().waitForAllBundles();
            getCatalogBundle().waitForCatalogProvider();
            getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
            getServiceManager().startFeature(true, "catalog-versioning-plugin");
        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testVersioningMetacard() throws Exception {
        String id = TestCatalog.ingestGeoJson(Library.getSimpleGeoJson());
        String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                HistoryMetacardImpl.ID_HISTORY,
                id)
                .addAttributeFilter(PROPERTY_IS_LIKE,
                        Metacard.TAGS,
                        HistoryMetacardImpl.HISTORY_TAG)
                .addLogicalOperator(AND)
                .getQuery("urn:catalog:metacard");

        given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(query)
                .when()
                .post(CSW_PATH.getUrl())
                .then()
                .log()
                .all()
                .assertThat()
                .body(hasXPath("count(//metacard)", is("1")),
                        hasXPath("//metacard/string[@name='history-id']/value", is(id)),
                        hasXPath("//metacard/string[@name='metacard-tags']/value",
                                is(HistoryMetacardImpl.HISTORY_TAG)),
                        hasXPath("//metacard/string[@name='history-tags']/value",
                                containsString(Metacard.DEFAULT_TAG)),
                        hasXPath("//metacard/string[@name='edited-by']/value",
                                containsString("Guest")),
                        hasXPath("//metacard/dateTime[@name='versioned']/value"),
                        hasXPath("//metacard/string[@name='state']/value", is("Created")));

        Map root = jsonToMap.apply(Library.getSimpleGeoJson());

        String newTitle = "Brand New Title";
        idxMap(root, "properties").put("title", newTitle);

        given().header("Content-Type", MediaType.APPLICATION_JSON)
                .body(JsonFactory.create()
                        .toJson(root))
                .when()
                .put(REST_PATH.getUrl() + id)
                .then()
                .log()
                .all()
                .assertThat()
                .statusCode(200);

        given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(query)
                .when()
                .post(CSW_PATH.getUrl())
                .then()
                .log()
                .all()
                .assertThat()
                .body(hasXPath("count(//metacard)", is("2")),
                        hasXPath("//metacard/string[@name='state']/value[text()='Updated']"),
                        hasXPath("//metacard/string[@name='title']/value[text()='" + newTitle
                                + "']"));

        given().when()
                .delete(REST_PATH.getUrl() + id)
                .then()
                .log()
                .all()
                .assertThat()
                .statusCode(200);

        given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(query)
                .when()
                .post(CSW_PATH.getUrl())
                .then()
                .log()
                .all()
                .assertThat()
                .body(hasXPath("count(//metacard)", is("3")),
                        hasXPath("//metacard/string[@name='state']/value[text()='Deleted']"));
    }

}
