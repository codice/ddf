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
package ddf.catalog.source.solr.textpath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.filter.Filter;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.data.Result;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.operation.QueryImpl;
import ddf.catalog.operation.QueryRequestImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.ConfigurationFileProxy;
import ddf.catalog.source.solr.ConfigurationStore;
import ddf.catalog.source.solr.Library;
import ddf.catalog.source.solr.MockMetacard;
import ddf.catalog.source.solr.SolrCatalogProvider;
import ddf.catalog.source.solr.SolrFilterDelegateFactoryImpl;
import ddf.catalog.source.solr.SolrProviderTestCase;
import ddf.catalog.source.solr.SolrServerFactory;

/**
 * Tests the TextPath support of the {@link SolrCatalogProvider}.
 * 
 * @author Phillip Klinefelter
 */
@Ignore
public class TestTextPath extends SolrProviderTestCase {

    @BeforeClass
    public static void setup() throws IngestException, UnsupportedQueryException {
        ConfigurationFileProxy configurationFileProxy = new ConfigurationFileProxy(null,
                ConfigurationStore.getInstance());
        ConfigurationStore.getInstance().setDataDirectoryPath("target/solr");

        provider = new SolrCatalogProvider(SolrServerFactory.getEmbeddedSolrServer(
                "solrconfig.xml", "schema.xml", configurationFileProxy),
                new GeotoolsFilterAdapterImpl(), new SolrFilterDelegateFactoryImpl());

        provider.setId(MASKED_ID);

        deleteAllIn(provider);
        MetacardImpl metacard1 = new MockMetacard(Library.getFlagstaffRecord());
        MetacardImpl metacard2 = new MockMetacard(Library.getTampaRecord());
        MetacardImpl metacard3 = new MockMetacard(Library.getShowLowRecord());
        MetacardImpl metacard4 = new MockMetacard(Library.getPurchaseOrderRecord());

        List<Metacard> list = Arrays.asList((Metacard) metacard1, metacard2, metacard3, metacard4);

        createIn(list, provider);
    }

    @Test
    @Ignore
    public void rootDescendantOrSelfPath() throws Exception {
        assertTextPath("//comment");

        assertNotTextPath("//foo");
    }

    @Test
    @Ignore
    public void childOnlyPath() throws Exception {
        assertTextPath("/purchaseOrder/comment");
        assertTextPath("/purchaseOrder/items/item/comment");
        assertTextPath("/purchaseOrder/items");

        assertNotTextPath("/foo/bar");
    }

    @Test
    @Ignore
    public void childThenDescendantOrSelfPath() throws Exception {
        assertTextPath("/purchaseOrder//comment");
        assertTextPath("/purchaseOrder/items//comment");

        assertNotTextPath("/foo//bar");
    }

    @Test
    @Ignore
    public void childThenDescendantOrSelfThenChildPath() throws Exception {
        assertTextPath("/purchaseOrder//item/USPrice");

        assertNotTextPath("/foo//bar/baz");
    }

    @Test
    @Ignore
    public void rootEqualityExpression() throws Exception {
        assertTextPath("/purchaseOrder//item/USPrice=\"148.95\"");
        assertTextPath("\"148.95\"=/purchaseOrder//item/USPrice");
        assertTextPath("/purchaseOrder//item/USPrice!=\"9.99\"");
        assertTextPath("/purchaseOrder//item/USPrice", "148.95");
    }

    @Test
    @Ignore
    public void predicateEqualityExpression() throws Exception {
        assertTextPath("/purchaseOrder//item[@partNum=\"872-AA\" and @a=\"b\"]/comment");
        assertTextPath("/purchaseOrder//item[@partNum=\"872-AA\" or @c=\"d\"]/comment");
        assertTextPath("/purchaseOrder//item[@partNum=\"926-AA\" and not(@a)]/shipDate");
        assertTextPath("/purchaseOrder//item[@partNum and @a]/comment");

        assertNotTextPath("/purchaseOrder//item[@partNum=\"872-AA\" and @c=\"d\"]/comment");
    }

    @Test
    @Ignore
    public void predicateAttributeEqualityExpression() throws Exception {
        assertTextPath("//items/item[@partNum=\"872-AA\"]/comment");

        assertNotTextPath("//items/item[@partNum=\"111-ZZ\"]/comment");
    }

    @Test
    @Ignore
    public void predicateNodeTextEqualityExpression() throws Exception {
        assertTextPath("//items/item[productName=\"Lawnmower\"]");

        assertNotTextPath("//items/item[productName=\"invalid\"]");
    }

    @Test
    @Ignore
    public void predicatePathExpression() throws Exception {
        assertTextPath("//items[item/productName]");

        assertNotTextPath("//items[item/invalid]");
    }

    @Test
    @Ignore
    public void predicateIndex() throws Exception {
        assertTextPath("//items/item[1]/comment");

        assertNotTextPath("//items/item[2]/comment");
    }

    @Test
    @Ignore
    public void childThenAttributePath() throws Exception {
        assertTextPath("/purchaseOrder/@orderDate");

        assertNotTextPath("/purchaseOrder/@invalid");
    }

    @Test
    @Ignore
    public void childWildcardsWithPredicateDescendant() throws Exception {
        assertTextPath("/*/*/item[.//comment]");

        assertNotTextPath("/*/*/item[.//invalid]");
    }

    @Test
    @Ignore
    public void rootNodeContextOnly() throws Exception {
        assertTextPath("purchaseOrder/items/item");
        assertTextPath("/purchaseOrder/items/item");
        assertTextPath("./purchaseOrder/items/item");

        assertNotTextPath("items/item");
        assertNotTextPath("/items/item");
        assertNotTextPath("./items/item");
    }

    @Test
    @Ignore
    public void laxSpacing() throws Exception {
        assertTextPath("  //  items  /  item  [  @  partNum  =  \"872-AA\"  ]  /  comment  ");
    }

    private void assertTextPath(String textPath) throws UnsupportedQueryException {
        assertXpathFilter(filterBuilder.xpath(textPath).exists());
    }

    private void assertTextPath(String textPath, String value) throws UnsupportedQueryException {
        assertXpathFilter(filterBuilder.xpath(textPath).is().like().text(value));
    }

    private void assertXpathFilter(Filter xpathFilter) throws UnsupportedQueryException {
        SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(
                xpathFilter)));
        assertEquals("Failed to find metacard with correct XML node.", 1, sourceResponse
                .getResults().size());

        for (Result r : sourceResponse.getResults()) {
            assertTrue("Wrong record, Purchase order keyword was not found.", ALL_RESULTS != r
                    .getMetacard().getMetadata().indexOf("872-AA"));
        }
    }

    private void assertNotTextPath(String textPath) throws UnsupportedQueryException {
        SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(
                filterBuilder.xpath(textPath).exists())));
        assertEquals("Found a metacard and should not have.", 0, sourceResponse.getResults().size());
    }

}
