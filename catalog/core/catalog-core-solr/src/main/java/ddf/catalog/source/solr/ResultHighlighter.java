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
package ddf.catalog.source.solr;

import static ddf.catalog.Constants.QUERY_HIGHLIGHT_KEY;
import static ddf.catalog.source.solr.DynamicSchemaResolver.FIRST_CHAR_OF_SUFFIX;

import com.google.common.collect.Sets;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.Highlight;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResultAttributeHighlight;
import ddf.catalog.operation.ResultHighlight;
import ddf.catalog.operation.impl.HighlightImpl;
import ddf.catalog.operation.impl.ResultAttributeHighlightImpl;
import ddf.catalog.operation.impl.ResultHighlightImpl;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultHighlighter {

  public static final String HIGHLIGHT_ENABLE_PROPERTY = "solr.highlight.enabled";

  public static final String SOLR_HIGHLIGHT_BLACKLIST = "solr.highlight.blacklist";

  public static final String SOLR_HIGHLIGHT_SNIPPETS = "solr.highlight.snippets";

  public static final String SOLR_HIGHLIGHT_KEY = "hl";

  private static final String HIGHLIGHT_PRE_TAG = "<em>";

  private static final String HIGHLIGHT_POST_TAG = "</em>";

  private static final int SURROUNDING_CONTEXT_SIZE = 20;

  private static final Pattern HIGHLIGHT_PATTERN =
      Pattern.compile(
          HIGHLIGHT_PRE_TAG + "(((?!" + HIGHLIGHT_PRE_TAG + ").)*?)" + HIGHLIGHT_POST_TAG);

  private static final Pattern TAG_PATTERN =
      Pattern.compile("(" + HIGHLIGHT_PRE_TAG + "|" + HIGHLIGHT_POST_TAG + ")");

  private static final Logger LOGGER = LoggerFactory.getLogger(ResultHighlighter.class);

  private final Set<String> highlightBlacklist =
      Sets.newHashSet(System.getProperty(SOLR_HIGHLIGHT_BLACKLIST, "").split("\\s*,\\s*"));

  private DynamicSchemaResolver resolver;

  private String mappedMetacardIdField;

  public ResultHighlighter(DynamicSchemaResolver resolver) {
    this.resolver = resolver;
  }

  public void setMappedMetacardIdField(String mappedMetacardIdField) {
    this.mappedMetacardIdField = mappedMetacardIdField;
  }

  public void processPreQuery(QueryRequest request, SolrQuery query) {
    if (userHighlightIsOn(request)) {
      enableHighlighter(query);
    }
  }

  public void processPostQuery(QueryResponse response, Map<String, Serializable> responseProps) {
    extractHighlighting(response, responseProps);
  }

  private Boolean userHighlightIsOn(QueryRequest request) {
    Boolean userHighlight;
    if (request.getProperties().get(QUERY_HIGHLIGHT_KEY) != null) {
      userHighlight = (Boolean) request.getProperties().get(QUERY_HIGHLIGHT_KEY);
    } else {
      userHighlight = isSystemHighlightingEnabled();
    }
    return userHighlight;
  }

  protected boolean isSystemHighlightingEnabled() {
    return BooleanUtils.toBoolean(System.getProperty(HIGHLIGHT_ENABLE_PROPERTY, "false"));
  }

  private String getSnippetSetting() {
    return System.getProperty(SOLR_HIGHLIGHT_SNIPPETS, "20");
  }

  protected void enableHighlighter(SolrQuery query) {
    query.setParam(SOLR_HIGHLIGHT_KEY, true);
    query.setParam("hl.fl", "*");
    query.setParam("hl.requireFieldMatch", true);
    query.setParam("hl.method", "unified");
    query.setParam("hl.preserveMulti", true);
    query.setParam("hl.snippets", getSnippetSetting());
  }

  protected void extractHighlighting(
      QueryResponse response, Map<String, Serializable> responseProps) {
    Map<String, Map<String, List<String>>> highlights = response.getHighlighting();
    if (highlights == null) {
      return;
    }
    List<ResultHighlight> resultsHighlights = new ArrayList<>();
    for (String resultId : highlights.keySet()) {

      Map<String, List<String>> fieldHighlight = highlights.get(resultId);
      Map<String, Set<Highlight>> consolidated = new HashMap<>();
      if (fieldHighlight != null && fieldHighlight.size() > 0) {
        for (Map.Entry<String, List<String>> entry : fieldHighlight.entrySet()) {
          String solrField = entry.getKey();
          String normalizedKey = resolveHighlightFieldName(solrField);
          if (isHighlightBlacklisted(normalizedKey)
              || isHighlightBlacklisted(solrField)
              || !isMetacardAttribute(normalizedKey, resultId, response)) {
            continue;
          }
          Set<Highlight> consolidatedSet = consolidated.get(normalizedKey);
          if (consolidatedSet == null) {
            consolidatedSet = new HashSet<>();
          }
          consolidatedSet.addAll(
              createHighlights(
                  entry.getValue(),
                  getAttributeValue(resultId, normalizedKey + SchemaFields.TEXT_SUFFIX, response)));
          if (!consolidatedSet.isEmpty()) {
            consolidated.put(normalizedKey, consolidatedSet);
          }
        }
      }
      if (!consolidated.isEmpty()) {
        List<ResultAttributeHighlight> attributeHighlights = new ArrayList<>();
        for (Map.Entry<String, Set<Highlight>> fieldEntry : consolidated.entrySet()) {
          attributeHighlights.add(
              new ResultAttributeHighlightImpl(
                  fieldEntry.getKey(), new ArrayList<>(fieldEntry.getValue())));
        }

        resultsHighlights.add(
            new ResultHighlightImpl(getMetacardId(resultId, response), attributeHighlights));
      }
    }

    if (!resultsHighlights.isEmpty()) {
      responseProps.put(QUERY_HIGHLIGHT_KEY, (Serializable) resultsHighlights);
    }
  }

  private List<Highlight> createHighlights(
      List<String> highlightResults, Collection<Object> baseValues) {
    List<Highlight> highlights = new ArrayList<>();
    if (baseValues != null && !baseValues.isEmpty()) {
      List<HighlightContext> highlightedValues = new ArrayList<>();
      highlightResults
          .stream()
          .forEach(result -> highlightedValues.addAll(getHighlightedValues(result)));

      int index = 0;
      for (Object value : baseValues) {
        if (value != null) {
          highlights.addAll(extractHighlights(value.toString(), highlightedValues, index));
        }
        index++;
      }
    }
    return highlights;
  }

  private List<Highlight> extractHighlights(
      String text, List<HighlightContext> values, int valueIndex) {
    List<Highlight> highlights = new ArrayList<>();
    TagIndices sourceTagIndices = new TagIndices(text);
    for (HighlightContext context : values) {
      int index = -1;
      int length = context.highlightedToken.length();
      HighlightContext source = new HighlightContext();
      source.surroundingContext = text;
      source.tokenContextOffset = 0;
      source.resolveSurroundingContext(false);
      do {
        index = source.surroundingContext.indexOf(context.surroundingContext, index + 1);
        if (index > -1) {
          int beginIndex = index + context.tokenContextOffset;
          int endIndex = index + context.tokenContextOffset + length;
          if (sourceTagIndices.getStartTagIndices().contains(beginIndex) && !context.embeded) {
            continue;
          }
          int sourceHighlightOffset =
              sourceTagIndices.countStartBefore(beginIndex) * HIGHLIGHT_PRE_TAG.length()
                  + sourceTagIndices.countEndBefore(beginIndex) * HIGHLIGHT_POST_TAG.length();
          beginIndex += sourceHighlightOffset;
          endIndex += sourceHighlightOffset;
          highlights.add(new HighlightImpl(beginIndex, endIndex, valueIndex));
        }
      } while (index > -1);
    }
    return highlights;
  }

  private List<HighlightContext> getHighlightedValues(String highlightedText) {
    List<HighlightContext> values = new ArrayList<>();
    Matcher matcher = HIGHLIGHT_PATTERN.matcher(highlightedText);
    while (matcher.find()) {
      String token = matcher.group(1);
      HighlightContext context = new HighlightContext();
      context.highlightedToken = token;
      context.surroundingContext = highlightedText;
      context.tokenContextOffset = matcher.start();
      context.embeded = isHighlightEmbeded(highlightedText, matcher.start());
      context.resolveSurroundingContext(true);
      values.add(context);
    }

    return values;
  }

  private boolean isHighlightEmbeded(String text, int highlightIndex) {
    if (highlightIndex - HIGHLIGHT_PRE_TAG.length() < 0) {
      return false;
    } else {
      return text.substring(highlightIndex - HIGHLIGHT_PRE_TAG.length(), highlightIndex)
          .equals(HIGHLIGHT_PRE_TAG);
    }
  }

  private Collection<Object> getAttributeValue(
      String resultId, String fieldName, QueryResponse response) {
    Optional<SolrDocument> metacard = getResponseDocument(resultId, response);
    if (metacard.isPresent()) {
      return metacard.get().getFieldValues(fieldName);
    }
    return null;
  }

  private String resolveHighlightFieldName(String solrFieldName) {
    int firstIndexOfSuffix = solrFieldName.indexOf(FIRST_CHAR_OF_SUFFIX);

    if (firstIndexOfSuffix != -1) {
      return solrFieldName.substring(0, firstIndexOfSuffix);
    }
    return solrFieldName;
  }

  private boolean isHighlightBlacklisted(String fieldName) {
    List<String> blacklist =
        highlightBlacklist
            .stream()
            .filter(item -> fieldName.matches(item))
            .collect(Collectors.toList());
    return !blacklist.isEmpty() || resolver.isPrivateField(fieldName);
  }

  private boolean isMetacardAttribute(String fieldName, String resultId, QueryResponse response) {
    MetacardType type = getMetacardType(resultId, response);
    if (type != null) {
      return type.getAttributeDescriptor(fieldName) != null;
    }

    return false;
  }

  private String getMetacardId(String resultId, QueryResponse response) {
    if (StringUtils.isNotBlank(mappedMetacardIdField)) {
      Optional<SolrDocument> document = getResponseDocument(resultId, response);
      if (document.isPresent()) {
        Object val = document.get().getFirstValue(mappedMetacardIdField);
        if (val != null) {
          return val.toString();
        }
      }
    }
    return resultId;
  }

  private Optional<SolrDocument> getResponseDocument(String resultId, QueryResponse response) {
    return response
        .getResults()
        .stream()
        .filter(doc -> resultId.equals(doc.getFirstValue(Core.ID + SchemaFields.TEXT_SUFFIX)))
        .findFirst();
  }

  private MetacardType getMetacardType(String resultId, QueryResponse response) {
    Optional<SolrDocument> metacard = getResponseDocument(resultId, response);
    if (metacard.isPresent()) {
      try {
        return resolver.getMetacardType(metacard.get());
      } catch (MetacardCreationException mce) {
        LOGGER.debug("Unable to read metacard type", mce);
      }
    }
    return null;
  }

  static class HighlightContext {
    String highlightedToken;
    String surroundingContext;
    int tokenContextOffset;
    boolean embeded = false;

    public void resolveSurroundingContext(boolean trimContext) {
      int index = -1;
      int adjustment = 0;
      do {
        index = surroundingContext.indexOf(HIGHLIGHT_PRE_TAG, index + 1);
        if (index != -1 && index < tokenContextOffset) {
          adjustment += HIGHLIGHT_PRE_TAG.length();
        }
      } while (index > -1);

      do {
        index = surroundingContext.indexOf(HIGHLIGHT_POST_TAG, index + 1);
        if (index != -1 && index < tokenContextOffset) {
          adjustment += HIGHLIGHT_POST_TAG.length();
        }
      } while (index > -1);
      tokenContextOffset -= adjustment;
      surroundingContext = surroundingContext.replaceAll(HIGHLIGHT_PRE_TAG, "");
      surroundingContext = surroundingContext.replaceAll(HIGHLIGHT_POST_TAG, "");

      if (trimContext) {
        if (surroundingContext.length() > SURROUNDING_CONTEXT_SIZE * 2) {
          int trimStart = Math.max(0, tokenContextOffset - SURROUNDING_CONTEXT_SIZE);
          int trimEnd =
              Math.min(surroundingContext.length(), tokenContextOffset + SURROUNDING_CONTEXT_SIZE);
          surroundingContext = surroundingContext.substring(trimStart, trimEnd);
          tokenContextOffset -= trimStart;
        }
      }
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(25, 49)
          .append(highlightedToken)
          .append(surroundingContext)
          .append(tokenContextOffset)
          .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      HighlightContext context = (HighlightContext) o;

      return new EqualsBuilder()
          .append(highlightedToken, context.highlightedToken)
          .append(surroundingContext, context.surroundingContext)
          .append(tokenContextOffset, context.tokenContextOffset)
          .isEquals();
    }
  }

  static class TagIndices {

    List<Integer> startTagIndices = new ArrayList<>();
    List<Integer> endTagIndices = new ArrayList<>();

    public TagIndices(String text) {
      int offset = 0;
      Matcher matcher = TAG_PATTERN.matcher(text);
      while (matcher.find()) {
        String tag = matcher.group(0);
        int index = matcher.start() - offset;
        if (tag.equals(HIGHLIGHT_PRE_TAG)) {
          startTagIndices.add(index);
        } else {
          endTagIndices.add(index);
        }
        offset += tag.length();
      }
    }

    public List<Integer> getStartTagIndices() {
      return startTagIndices;
    }

    public List<Integer> getEndTagIndices() {
      return endTagIndices;
    }

    public int countStartBefore(int index) {
      return (int) getStartTagIndices().stream().filter(val -> val <= index).count();
    }

    public int countEndBefore(int index) {
      return (int) getEndTagIndices().stream().filter(val -> val <= index).count();
    }
  }
}
