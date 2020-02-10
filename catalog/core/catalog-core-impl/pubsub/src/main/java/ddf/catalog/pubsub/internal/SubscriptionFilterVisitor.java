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
package ddf.catalog.pubsub.internal;

import ddf.catalog.data.Metacard;
import ddf.catalog.impl.filter.FuzzyFunction;
import ddf.catalog.pubsub.EventProcessorImpl.DateType;
import ddf.catalog.pubsub.criteria.geospatial.SpatialOperator;
import ddf.catalog.pubsub.predicate.ContentTypePredicate;
import ddf.catalog.pubsub.predicate.ContextualPredicate;
import ddf.catalog.pubsub.predicate.EntryPredicate;
import ddf.catalog.pubsub.predicate.GeospatialPredicate;
import ddf.catalog.pubsub.predicate.Predicate;
import ddf.catalog.pubsub.predicate.TemporalPredicate;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.LikeFilterImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.jts.spatialschema.geometry.GeometryImpl;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.During;
import org.opengis.temporal.Period;
import org.opengis.temporal.PeriodDuration;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionFilterVisitor extends DefaultFilterVisitor {
  public static final double EQUATORIAL_RADIUS_IN_METERS = 6378137.0;

  public static final String LUCENE_ESCAPE_CHAR = "\\";

  public static final String LUCENE_WILDCARD_CHAR = "*";

  public static final String LUCENE_SINGLE_CHAR = "?";

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionFilterVisitor.class);

  private static final String QUOTE = "\"";

  // private static final String FUZZY_FUNCTION_NAME = "fuzzy";

  public SubscriptionFilterVisitor() {}

  /** A helper method to combine multiple predicates by a logical AND */
  public static Predicate and(final Predicate left, final Predicate right) {
    notNull(left, "left");
    notNull(right, "right");

    return new Predicate() {
      public boolean matches(Event properties) {
        return left.matches(properties) && right.matches(properties);
      }

      @Override
      public String toString() {
        return "(" + left + ") AND (" + right + ")";
      }
    };
  }

  /** A helper method to combine multiple predicates by a logical OR */
  public static Predicate or(final Predicate left, final Predicate right) {
    notNull(left, "left");
    notNull(right, "right");

    return new Predicate() {
      public boolean matches(Event properties) {
        return left.matches(properties) || right.matches(properties);
      }

      @Override
      public String toString() {
        return "(" + left + ") OR (" + right + ")";
      }
    };
  }

  /** A helper method to combine multiple predicates by a logical NOT */
  public static Predicate not(final Predicate predicate) {
    notNull(predicate, "predicate");

    return new Predicate() {
      public boolean matches(Event properties) {
        return !predicate.matches(properties);
      }

      @Override
      public String toString() {
        return "(NOT (" + predicate + ")";
      }
    };
  }

  /**
   * Asserts whether the value is <b>not</b> <tt>null</tt>
   *
   * @param value the value to test
   * @param name the key that resolved the value
   * @throws IllegalArgumentException is thrown if assertion fails
   */
  public static void notNull(Object value, String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " must be specified");
    }
  }

  @Override
  public Object visit(Not filter, Object data) {
    LOGGER.debug("ENTERING: NOT filter");

    Predicate returnPredicate = null;

    Filter filterToNot = filter.getFilter();
    Predicate predicateToNot = (Predicate) filterToNot.accept(this, null);
    returnPredicate = not(predicateToNot);
    LOGGER.debug("EXITING: NOT filter");

    return returnPredicate;
  }

  @Override
  public Object visit(Or filter, Object data) {
    LOGGER.debug("ENTERING: OR filter");
    Predicate returnPredicate = null;

    List<Predicate> predList = new ArrayList<Predicate>();
    List<Filter> childList = filter.getChildren();
    if (childList != null) {
      for (Filter child : childList) {
        if (child == null) {
          continue;
        }

        predList.add((Predicate) child.accept(this, data));
      }
    }

    for (Predicate p : predList) {
      if (returnPredicate == null) {
        returnPredicate = p;
      } else {
        returnPredicate = or(returnPredicate, p);
      }
    }

    LOGGER.debug("EXITING: OR filter");

    return returnPredicate;
  }

  @Override
  public Object visit(And filter, Object data) {
    LOGGER.debug("ENTERING: AND filter");

    Predicate returnPredicate = null;

    List<Predicate> predList = new ArrayList<Predicate>();
    List<Filter> childList = filter.getChildren();
    if (childList != null) {
      for (Filter child : childList) {
        if (child == null) {
          continue;
        }

        predList.add((Predicate) child.accept(this, data));
      }
    }

    ContentTypePredicate currentContentTypePred = null;
    LOGGER.debug("predicate list size: {}", predList.size());
    for (Predicate p : predList) {
      if (p == null) {
        // filterless subscription
        LOGGER.debug("Found null predicate.  Indicates Filterless Subscription.");
      } else if (p instanceof ContentTypePredicate) {
        LOGGER.debug("Found ContentType Predicate.");

        if (currentContentTypePred == null) {
          currentContentTypePred = (ContentTypePredicate) p;
        } else {
          ContentTypePredicate incomingContentTypePred = (ContentTypePredicate) p;
          String currentType = currentContentTypePred.getType();
          String currentVersion = currentContentTypePred.getVersion();
          String incomingType = incomingContentTypePred.getType();
          String incomingVersion = incomingContentTypePred.getVersion();

          // Case 1
          // First ContentTypePredicate found has just a type and no version. Second
          // ContentTypePredicate has version and no type.
          // Combine the two.
          if (currentType != null && incomingType == null && incomingVersion != null) {
            currentContentTypePred.setVersion(incomingVersion);

            // Case 2
            // First ContentTypePredicate has no type but has a version. Second
            // ContentTypePredicate has no version, but it has a type.
          } else if (currentType == null && currentVersion != null && incomingType != null) {
            currentContentTypePred.setType(incomingType);
          }
        }

        if (returnPredicate == null) {
          LOGGER.debug("first return predicate");
          returnPredicate = currentContentTypePred;
        } else {
          LOGGER.debug(
              "ANDing the predicates. Pred1: {} Pred2: {}",
              returnPredicate,
              currentContentTypePred);
          returnPredicate = and(returnPredicate, currentContentTypePred);
        }

      } else { // if Spatial Predicate, Temporal Predicate, Contextual, or Entry Predicate

        if (returnPredicate == null) {
          LOGGER.debug("first return predicate");
          returnPredicate = p;
        } else {
          LOGGER.debug("ANDing the predicates. Pred1: {} Pred2: {}", returnPredicate, p);
          returnPredicate = and(returnPredicate, p);
        }
      }
    }

    LOGGER.debug("EXITING: AND filter");

    return returnPredicate;
  }

  /** DWithin filter maps to a Point/Radius distance Spatial search criteria. */
  @Override
  public Object visit(DWithin filter, Object data) {
    LOGGER.debug("ENTERING: DWithin filter");
    LOGGER.debug("Must have received point/radius query criteria.");

    double radius = filter.getDistance();
    org.locationtech.jts.geom.Geometry jtsGeometry =
        getJtsGeometery((LiteralExpressionImpl) filter.getExpression2());

    double radiusInDegrees = (radius * 180.0) / (Math.PI * EQUATORIAL_RADIUS_IN_METERS);
    LOGGER.debug("radius in meters : {}", radius);
    LOGGER.debug("radius in degrees : {}", radiusInDegrees);

    Predicate predicate = new GeospatialPredicate(jtsGeometry, null, radiusInDegrees);

    LOGGER.debug("EXITING: DWithin filter");

    return predicate;
  }

  /** Within filter maps to a CONTAINS Spatial search criteria. */
  @Override
  public Object visit(Within filter, Object data) {
    LOGGER.debug("ENTERING: Within filter");
    LOGGER.debug("Must have received CONTAINS query criteria: {}", filter.getExpression2());

    org.locationtech.jts.geom.Geometry jtsGeometry =
        getJtsGeometery((LiteralExpressionImpl) filter.getExpression2());

    Predicate predicate =
        new GeospatialPredicate(jtsGeometry, SpatialOperator.CONTAINS.name(), 0.0);

    LOGGER.debug("EXITING: Within filter");

    return predicate;
  }

  /** Intersects filter maps to a OVERLAPS Spatial search criteria. */
  @Override
  public Object visit(Intersects filter, Object data) {
    LOGGER.debug("ENTERING: Intersects filter");
    LOGGER.debug("Must have received OVERLAPS query criteria.");

    org.locationtech.jts.geom.Geometry jtsGeometry =
        getJtsGeometery((LiteralExpressionImpl) filter.getExpression2());

    Predicate predicate =
        new GeospatialPredicate(jtsGeometry, SpatialOperator.OVERLAPS.name(), 0.0);
    LOGGER.debug("EXITING: Intersects filter");

    return predicate;
  }

  /** During filter maps to a Temporal (Absolute and Modified) search criteria. */
  @Override
  public Object visit(During filter, Object data) {
    LOGGER.debug("ENTERING: During filter");

    AttributeExpressionImpl temporalTypeAttribute =
        (AttributeExpressionImpl) filter.getExpression1();
    String temporalType = temporalTypeAttribute.getPropertyName();
    LiteralExpressionImpl timePeriodLiteral = (LiteralExpressionImpl) filter.getExpression2();
    Object literal = timePeriodLiteral.getValue();

    Predicate returnPredicate = null;
    if (literal instanceof Period) {

      Period timePeriod = (Period) literal;
      // Extract the start and end dates from the OGC TOverlaps filter
      Date start = timePeriod.getBeginning().getPosition().getDate();
      Date end = timePeriod.getEnding().getPosition().getDate();
      LOGGER.debug("time period lowerBound = {}", start);
      LOGGER.debug("time period upperBound = {}", end);

      LOGGER.debug("EXITING: (temporal) filter");

      returnPredicate = new TemporalPredicate(start, end, DateType.getDateType(temporalType));
      // CREATE RELATIVE
    } else if (literal instanceof PeriodDuration) {
      DefaultPeriodDuration duration = (DefaultPeriodDuration) literal;

      long offset = duration.getTimeInMillis();
      LOGGER.debug("EXITING: (temporal) filter");
      returnPredicate = new TemporalPredicate(offset, DateType.getDateType(temporalType));
    }

    LOGGER.debug("temporalType: {}", temporalType);
    LOGGER.debug("Temporal Predicate: {}", returnPredicate);
    LOGGER.debug("EXITING: During filter");

    return returnPredicate;
  }

  /** PropertyIsEqualTo filter maps to a Type/Version(s) and Entry search criteria. */
  @Override
  public Object visit(PropertyIsEqualTo filter, Object data) {
    LOGGER.debug("ENTERING: PropertyIsEqualTo filter");

    // TODO: consider if the contentType parameters are invalid (i.e. anything where type is
    // null)
    AttributeExpressionImpl exp1 = (AttributeExpressionImpl) filter.getExpression1();
    String propertyName = exp1.getPropertyName();
    LiteralExpressionImpl exp2 = (LiteralExpressionImpl) filter.getExpression2();

    Predicate predicate = null;

    if (Metacard.ID.equals(propertyName)) {
      String entryId = (String) exp2.getValue();
      LOGGER.debug("entry id for new entry predicate: {}", entryId);
      predicate = new EntryPredicate(entryId);
    } else if (Metacard.CONTENT_TYPE.equals(propertyName)) {
      String typeValue = (String) exp2.getValue();
      predicate = new ContentTypePredicate(typeValue, null);
    } else if (Metacard.CONTENT_TYPE_VERSION.equals(propertyName)) {
      String versionValue = (String) exp2.getValue();
      predicate = new ContentTypePredicate(null, versionValue);
    } else if (Metacard.RESOURCE_URI.equals(propertyName)) {
      URI productUri = null;

      if (exp2.getValue() instanceof URI) {
        productUri = (URI) exp2.getValue();
        predicate = new EntryPredicate(productUri);
      } else {

        try {
          productUri = new URI((String) exp2.getValue());
          predicate = new EntryPredicate(productUri);
        } catch (URISyntaxException e) {

          LOGGER.debug("URI Syntax exception creating EntryPredicate", e);
          throw new UnsupportedOperationException(
              "Could not create a URI object from the given ResourceURI.", e);
        }
      }
    }

    LOGGER.debug("EXITING: PropertyIsEqualTo filter");

    return predicate;
  }

  /** PropertyIsLike filter maps to a Contextual search criteria. */
  @Override
  public Object visit(PropertyIsLike filter, Object data) {
    LOGGER.debug("ENTERING: PropertyIsLike filter");

    String wildcard = filter.getWildCard();
    String escape = filter.getEscape();
    String single = filter.getSingleChar();
    boolean isFuzzy = false;
    List<String> textPathList = null;

    LikeFilterImpl likeFilter = (LikeFilterImpl) filter;

    Expression expression = likeFilter.getExpression();

    // This block handles if the PropertyIsLike filter is representing a Content Type
    // or Content Type Version. If that is the case, then create and return a
    // ContentTypePredicate
    if (expression instanceof PropertyName) {
      PropertyName propertyName = (PropertyName) expression;
      if (Metacard.CONTENT_TYPE.equals(propertyName.getPropertyName())) {
        LOGGER.debug("Expression is ContentType.");
        String typeValue = likeFilter.getLiteral();
        ContentTypePredicate predicate = new ContentTypePredicate(typeValue, null);
        return predicate;
      } else if (Metacard.CONTENT_TYPE_VERSION.equals(propertyName.getPropertyName())) {
        LOGGER.debug("Expression is ContentTypeVersion.");
        String versionValue = likeFilter.getLiteral();
        ContentTypePredicate predicate = new ContentTypePredicate(null, versionValue);
        return predicate;
      }
    }

    if (expression instanceof AttributeExpressionImpl) {
      AttributeExpressionImpl textPathExpression = (AttributeExpressionImpl) expression;
      textPathList = extractXpathSelectors(textPathExpression);
    } else if (expression instanceof FuzzyFunction) {
      FuzzyFunction fuzzyFunction = (FuzzyFunction) expression;
      LOGGER.debug("fuzzy search");
      isFuzzy = true;
      List<Expression> expressions = fuzzyFunction.getParameters();
      AttributeExpressionImpl firstExpression = (AttributeExpressionImpl) expressions.get(0);

      if (!Metacard.ANY_TEXT.equals(firstExpression.getPropertyName())) {
        LOGGER.debug("fuzzy search has a text path section");
        textPathList = extractXpathSelectors(firstExpression);
      }
    }

    String searchPhrase = likeFilter.getLiteral();
    LOGGER.debug("raw searchPhrase = [{}]", searchPhrase);

    String sterilizedSearchPhrase = sterilize(searchPhrase, wildcard, escape, single);
    LOGGER.debug("sterilizedSearchPhrase = [{}]", sterilizedSearchPhrase);

    ContextualPredicate contextPred =
        new ContextualPredicate(
            sterilizedSearchPhrase, isFuzzy, likeFilter.isMatchingCase(), textPathList);

    LOGGER.debug("EXITING: PropertyIsLike filter");

    return contextPred;
  }

  private List<String> extractXpathSelectors(AttributeExpressionImpl textPathExpression) {
    List<String> textPathList = new ArrayList<String>();
    String selectors;
    selectors = textPathExpression.getPropertyName();
    LOGGER.debug("sub filter visitor selectors = {}", selectors);

    // Copy text paths into contextual criteria if any specified other than default "anyText"
    // which needs to be "removed" as it means nothing to the source
    if (selectors != null && !selectors.isEmpty() && !selectors.contains(Metacard.ANY_TEXT)) {
      String[] xpathExpressions = selectors.split(",");
      for (String textPath : xpathExpressions) {
        LOGGER.debug("adding text path to list: {}", textPath);
        textPathList.add(textPath);
      }
    }
    return textPathList;
  }

  @Override
  public Object visit(PropertyName expression, Object data) {
    LOGGER.debug("ENTERING: PropertyName expression");

    // countOccurrence( expression );

    LOGGER.debug("EXITING: PropertyName expression");

    return data;
  }

  @Override
  public Object visit(Literal expression, Object data) {
    LOGGER.debug("ENTERING: Literal expression");

    // countOccurrence( expression );

    LOGGER.debug("EXITING: Literal expression");

    return data;
  }

  @Override
  public Object visit(IncludeFilter filter, Object data) {
    LOGGER.debug("ENTERING: Visit Filter Includes");

    LOGGER.debug("EXITING: Visit Filter Includes");

    return null;
  }

  // translate filter contextual criteria to lucene syntax
  private String sterilize(String searchPhrase, String wildcard, String escape, String single) {

    // remove any spaces leading or trailing
    String returnSearchPhrase = searchPhrase.trim();

    if (!escape.equals(LUCENE_ESCAPE_CHAR)) {
      returnSearchPhrase =
          returnSearchPhrase.replaceAll(
              "(?<!" + "\\Q" + escape + "\\E)" + "\\Q" + escape + "\\E",
              Matcher.quoteReplacement(LUCENE_ESCAPE_CHAR));
    }
    if (!wildcard.equals(LUCENE_WILDCARD_CHAR)) {
      // it is required to change the wildcard character of the
      // filter into the wildcard character of the Catalog Provider

      // one problem exists here is that this assumes that backslash is the escape character.
      returnSearchPhrase =
          returnSearchPhrase.replaceAll(
              "(?<!\\\\)" + "\\Q" + wildcard + "\\E", LUCENE_WILDCARD_CHAR);
    }

    String[] splitTokens = returnSearchPhrase.split(" ");

    // if this is a phrase versus a single word, wrap in quotes
    if (splitTokens.length > 1) {
      returnSearchPhrase = QUOTE + returnSearchPhrase + QUOTE;
    }

    return returnSearchPhrase;
  }

  private org.locationtech.jts.geom.Geometry getJtsGeometery(LiteralExpressionImpl geoExpression) {
    org.locationtech.jts.geom.Geometry jtsGeometry;

    if (geoExpression.getValue() instanceof GeometryImpl) {
      GeometryImpl geo = (GeometryImpl) geoExpression.getValue();
      jtsGeometry = geo.getJTSGeometry();
    } else if (geoExpression.getValue() instanceof org.locationtech.jts.geom.Geometry) {
      jtsGeometry = (org.locationtech.jts.geom.Geometry) geoExpression.getValue();
    } else {
      throw new UnsupportedOperationException(
          "Unsupported implementation of Geometry for spatial filters.");
    }

    return jtsGeometry;
  }
}
