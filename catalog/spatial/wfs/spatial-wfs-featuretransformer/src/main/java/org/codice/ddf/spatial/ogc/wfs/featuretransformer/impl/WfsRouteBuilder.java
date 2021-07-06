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
package org.codice.ddf.spatial.ogc.wfs.featuretransformer.impl;

import ddf.catalog.data.Metacard;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.processor.aggregate.AbstractListAggregationStrategy;
import org.codice.ddf.spatial.ogc.wfs.catalog.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollectionImpl;

public final class WfsRouteBuilder extends RouteBuilder {

  static final String FEATURECOLLECTION_ENDPOINT_URL = "direct://wfsTransformFeatureCollection";

  private static final String FEATUREMEMBER_ENDPOINT_URL = "direct://wfsTransformFeatureMember";

  @Override
  public void configure() {
    /*
     * This route is designed to be called via a Camel proxy using the FeatureTransformationService
     * interface. Arguments are bound to the message body as an array, i.e.
     *
     * method: apply(InputStream featureCollection, WfsMetadata metadata)
     * message body: new Object[] { featureCollection, metadata }
     */
    from(FEATURECOLLECTION_ENDPOINT_URL)
        .id("TransformFeatureCollectionRoute")
        .streamCaching()
        .setHeader("metadata", simple("${body[1]}"))
        // Stream caching doesn't work when the input stream is located in a header. Copying the
        // stream to the body first is a workaround. It causes the stream to be cached, then we can
        // copy that cache to the header so we can access it later.
        .setBody(simple("${body[0]}"))
        .setHeader("xml", body())
        .setHeader(
            "numberOfFeatures",
            XPathBuilder.xpath("/wfs:FeatureCollection/@numberOfFeatures", Long.class)
                .namespace("wfs", "http://www.opengis.net/wfs"))
        .setBody(simple("${header.metadata.featureMemberNodeNames}"))
        .split(body(), new MetacardListAggregationStrategy())
        .setHeader("featureMemberNodeName", simple("${body}"))
        .bean(
            "wfsTransformerProcessor",
            "setActiveFeatureMemberNodeName(${header.metadata}, ${header.featureMemberNodeName})")
        .setBody(header("xml"))
        .split(
            body().tokenizeXML("${header.featureMemberNodeName}", "FeatureCollection"),
            new MetacardAggregationStrategy())
        .streaming()
        .to(FEATUREMEMBER_ENDPOINT_URL)
        .end()
        .end()
        .choice()
        .when(body().isInstanceOf(List.class))
        .bean(
            WfsCollectionFactory.class, "createWfsCollection(${body}, ${header.numberOfFeatures})")
        .otherwise()
        .bean(WfsCollectionFactory.class, "createEmptyWfsCollection()");

    from(FEATUREMEMBER_ENDPOINT_URL)
        .id("TransformFeatureMemberRoute")
        .bean("wfsTransformerProcessor", "apply(${body}, ${header.metadata})");
  }

  private static class MetacardAggregationStrategy
      extends AbstractListAggregationStrategy<Metacard> {
    @Override
    public Metacard getValue(final Exchange exchange) {
      return (Metacard) exchange.getIn().getBody(Optional.class).orElse(null);
    }
  }

  private static class MetacardListAggregationStrategy implements AggregationStrategy {
    @Override
    public Exchange aggregate(final Exchange oldExchange, final Exchange newExchange) {
      final List<Metacard> incomingMetacards = newExchange.getIn().getBody(List.class);

      if (oldExchange == null) {
        final List<Metacard> metacards = new ArrayList<>();
        if (incomingMetacards != null) {
          metacards.addAll(incomingMetacards);
        }
        newExchange.getIn().setBody(metacards);
        return newExchange;
      }

      final List<Metacard> aggregateMetacards = oldExchange.getIn().getBody(List.class);
      if (incomingMetacards != null) {
        aggregateMetacards.addAll(incomingMetacards);
      }
      return oldExchange;
    }
  }

  // Must be public for Camel bean binding
  public static class WfsCollectionFactory {

    private WfsCollectionFactory() {}

    public static WfsFeatureCollection createEmptyWfsCollection() {
      return new WfsFeatureCollectionImpl(0);
    }

    public static WfsFeatureCollection createWfsCollection(
        final List<Metacard> featureMembers, final Long numberOfFeatures) {
      if (numberOfFeatures != null) {
        return new WfsFeatureCollectionImpl(numberOfFeatures, featureMembers);
      } else {
        return new WfsFeatureCollectionImpl(featureMembers.size(), featureMembers);
      }
    }
  }
}
