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
package org.codice.ddf.spatial.ogc.wfs.catalog.message;

import ddf.catalog.data.Metacard;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public final class WfsRouteBuilder extends RouteBuilder {
  protected static final String ENDPOINT_URL = "direct://wfsTransform";

  public void configure() {
    from(ENDPOINT_URL)
        .setHeader("metadata", simple("${body.getArgs()[1]}"))
        .setBody(simple("${body.getArgs()[0]}"))
        .streamCaching()
        .split(
            body().tokenizeXML("${header.metadata.featureMemberNodeName}", "FeatureCollection"),
            new WfsMemberAggregationStrategy())
        .streaming()
        .bean("wfsTransformerProcessor", "apply(${body}, ${header.metadata})")
        .end();
  }

  public static class WfsMemberAggregationStrategy implements AggregationStrategy {
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
}
