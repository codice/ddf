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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public final class WfsRouteBuilder extends RouteBuilder {

  protected static final String FEATURECOLLECTION_ENDPOINT_URL =
      "direct://wfsTransformFeatureCollection";

  protected static final String FEATUREMEMBER_ENDPOINT_URL = "direct://wfsTransformFeatureMember";

  public void configure() {
    from(FEATURECOLLECTION_ENDPOINT_URL)
        .id("TransformFeatureCollectionRoute")
        .setHeader("metadata", simple("${body.getArgs()[1]}"))
        .setHeader("xml", simple("${body.getArgs()[0]}"))
        .setBody(simple("${header.metadata.featureMemberNodeNames}"))
        .streamCaching()
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
        .when(body().isInstanceOf(InputStream.class))
        .setBody(constant(Collections.emptyList()));

    from(FEATUREMEMBER_ENDPOINT_URL)
        .id("TransformFeatureMemberRoute")
        .bean("wfsTransformerProcessor", "apply(${body}, ${header.metadata})");
  }

  public static class MetacardAggregationStrategy implements AggregationStrategy {

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
      Optional<Metacard> metacardOpt = newExchange.getIn().getBody(Optional.class);

      if (oldExchange == null) {
        final List<Metacard> metacardList = new ArrayList<>();
        metacardOpt.ifPresent(metacardList::add);
        newExchange.getIn().setBody(metacardList);
        return newExchange;
      }

      final List<Metacard> metacardList =
          Optional.ofNullable(oldExchange.getIn().getBody(List.class)).orElse(new ArrayList<>());

      metacardOpt.ifPresent(metacardList::add);
      return oldExchange;
    }
  }

  private class MetacardListAggregationStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
      List<Metacard> incomingMetacards = newExchange.getIn().getBody(List.class);

      if (oldExchange == null) {
        List<Metacard> metacards = new ArrayList<>();
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
}
