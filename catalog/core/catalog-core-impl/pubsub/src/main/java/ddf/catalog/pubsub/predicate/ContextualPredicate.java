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
package ddf.catalog.pubsub.predicate;

import ddf.catalog.pubsub.criteria.contextual.ContextualEvaluationCriteria;
import ddf.catalog.pubsub.criteria.contextual.ContextualEvaluationCriteriaImpl;
import ddf.catalog.pubsub.criteria.contextual.ContextualEvaluator;
import ddf.catalog.pubsub.criteria.contextual.ContextualTokenizer;
import ddf.catalog.pubsub.internal.PubSubConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.store.Directory;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextualPredicate implements Predicate {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContextualPredicate.class);

  private String searchPhrase;

  private boolean fuzzy;

  private boolean caseSensitiveSearch;

  private Collection<String> textPaths;

  public ContextualPredicate(
      String searchPhrase,
      boolean fuzzy,
      boolean caseSensitiveSearch,
      Collection<String> textPaths) {
    this.fuzzy = fuzzy;
    this.caseSensitiveSearch = caseSensitiveSearch;

    if (textPaths != null && !textPaths.isEmpty()) {
      LOGGER.debug("text paths size: {}", textPaths.size());
      this.textPaths = new ArrayList<String>(textPaths);
    }
    this.searchPhrase = normalizePhrase(searchPhrase, fuzzy);
  }

  public static boolean isContextual(String searchPhrase) {
    return !searchPhrase.isEmpty();
  }

  /**
   * Normalizes a search phrase for a Lucene query
   *
   * @param inputPhrase the input phrase
   * @param isFuzzy true indicates the criteria is fuzzy
   * @return a search phrase aligned to Lucene syntax
   */
  public static String normalizePhrase(String inputPhrase, boolean isFuzzy) {
    String phrase = "";
    if (!"".equals(inputPhrase)) {
      phrase = inputPhrase.trim();
      String parts[] = phrase.split("\"");
      LOGGER.debug("phrase = [{}]    parts.length = {}", phrase, parts.length);
      // if multiple parts found, then exact (quoted) phrases are present
      if (parts.length > 1) {
        // Odd parts are in quotes, i.e., exact (quoted) phrases, so skip them
        // Even parts are individual words or operators
        for (int i = 0; i < parts.length; i++) {
          LOGGER.debug("parts[{}] = {}", i, parts[i]);
          if (i % 2 == 0) {
            if (!parts[i].isEmpty()) {
              parts[i] = normalizeBooleanOperators(parts[i]);
              parts[i] = escapeSpecialCharacters(parts[i]);

              if (isFuzzy && !isBooleanOperator(parts[i])) {
                parts[i] = parts[i] + "~";
                parts[i] = parts[i].replace("~~", "~");

                LOGGER.debug("Fuzzy Search adding a tilde: {}", parts[i]);
              }
            } else {
              LOGGER.debug("part[{}] was empty", i);
            }
          } else {
            parts[i] = escapeSpecialCharacters(parts[i]);
          }
        }

        StringBuilder phraseBuilder = new StringBuilder("");
        for (int i = 0; i < parts.length; i++) {
          phraseBuilder.append(parts[i]);
          if (i < (parts.length - 1)) {
            phraseBuilder.append("\"");
          }
        }
        phrase = phraseBuilder.toString();
      } else {
        LOGGER.debug("parts.length <= 1:  phrase = {}", phrase);
        phrase = normalizeBooleanOperators(phrase);
        phrase = escapeSpecialCharacters(phrase);
        if (isFuzzy) {
          String[] words = phrase.trim().split("[ ]+");
          for (int i = 0; i < words.length; i++) {
            String[] subParts = words[i].split("[\\(\\)]+");
            for (String subPart : subParts) {
              if (!subPart.isEmpty() && !isBooleanOperator(subPart)) {
                String fuzzySubPart = subPart + "~";
                phrase = phrase.replaceFirst(Pattern.quote(subPart), fuzzySubPart);
                LOGGER.debug("2. Fuzzy Search adding a tilde: {}", subPart);
                LOGGER.debug("phrase = {}", phrase);
              }
            }

            phrase = phrase.replace("~~", "~");
          }

          LOGGER.debug("2. Fuzzy-fied phrase: {}", phrase);
        }
      }

      // Pass thru the last literal double quote
      if (inputPhrase.lastIndexOf('"') == inputPhrase.length() - 1) {
        phrase = phrase + "\"";
      }

    } else {
      phrase = "";
    }
    LOGGER.debug("Normalization complete. \nBefore: {}\nAfter: {}", inputPhrase, phrase);

    return phrase;
  }

  private static String escapeSpecialCharacters(String phrase) {
    StringBuilder sb = new StringBuilder();
    char[] chars = phrase.trim().toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char currentChar = chars[i];
      // * is escaped by the subscription when not a wildcard
      // if the character has already been manually escaped, don't double escape
      char nullChar = '\0';
      char nextChar = nullChar;
      if (i + 1 < chars.length) {
        nextChar = chars[i + 1];
      }
      if (currentChar == '\\'
          && nextChar != nullChar
          && ContextualTokenizer.SPECIAL_CHARACTERS_SET.contains(nextChar)) {
        // these two tokens constitute an escaped character,
        // so consume them together
        i = i + 1;
        sb.append(currentChar);
        sb.append(nextChar);
      } else if (currentChar != '*'
          && ContextualTokenizer.SPECIAL_CHARACTERS_SET.contains(currentChar)) {
        // handle unescaped special characters
        sb.append("\\");
        sb.append(currentChar);
      } else {
        sb.append(currentChar);
      }
    }
    phrase = sb.toString();
    return phrase;
  }

  /**
   * Normalize all Boolean operators in the phrase since Lucene grammar requires all boolean
   * operators to be uppercase.
   *
   * @param phrase the input phrase
   * @return the normalized phrase
   */
  private static String normalizeBooleanOperators(String phrase) {
    phrase = phrase.replace(" not ", " NOT ");
    phrase = phrase.replace(" or ", " OR ");
    phrase = phrase.replace(" and ", " AND ");
    phrase = phrase.replace(" & ", "AND");
    phrase = phrase.replace(" | ", "OR");

    return phrase;
  }

  private static boolean isBooleanOperator(String input) {
    int index =
        StringUtils.indexOfAny(
            input.trim().toLowerCase(), new String[] {"not", "and", "or", "&", "|"});

    return index == 0;
  }

  public boolean matches(Event properties) {
    String methodName = "matches";
    LOGGER.debug("ENTERING: {}", methodName);

    LOGGER.debug("Headers: {}", properties);

    ContextualEvaluationCriteria cec = null;
    Map<String, Object> contextualMap =
        (Map<String, Object>) properties.getProperty(PubSubConstants.HEADER_CONTEXTUAL_KEY);

    if (contextualMap == null) {
      LOGGER.debug("No contextual metadata to search against.");
      return false;
    }

    String operation = (String) properties.getProperty(PubSubConstants.HEADER_OPERATION_KEY);
    LOGGER.debug("operation = {}", operation);
    String metadata = (String) contextualMap.get("METADATA");
    LOGGER.debug("metadata = [{}]", metadata);

    // If deleting a catalog entry and the entry's metadata is only the word "deleted" (i.e.,
    // the
    // source is deleting the catalog entry and did not send any metadata with the delete
    // event), then
    // cannot apply any contextual filtering - just send the event on to the subscriber
    if (operation.equals(PubSubConstants.DELETE)
        && metadata.equals(PubSubConstants.METADATA_DELETED)) {
      LOGGER.debug(
          "Detected a DELETE operation where metadata is just the word 'deleted', so send event on to subscriber");
      return true;
    }

    // If predicate specified one or more text paths, then extract the entry's metadata from the
    // Event properties and
    // pass it and the text path(s) to the evaluation criteria (which will build a Lucene index
    // on the metadata using the
    // text paths)
    if (this.textPaths != null && !this.textPaths.isEmpty()) {
      LOGGER.debug("creating criteria with textPaths and metadata document");
      try {
        cec =
            new ContextualEvaluationCriteriaImpl(
                searchPhrase,
                fuzzy,
                caseSensitiveSearch,
                this.textPaths.toArray(new String[this.textPaths.size()]),
                (String) contextualMap.get("METADATA"));
      } catch (IOException e) {
        LOGGER.debug("IO exception during context evaluation", e);
        return false;
      }

      // This predicate has no text paths specified, so can use default Lucene search index, which
      // indexed the entry's entire metadata
      // per the default XPath expressions in ContextualEvaluator, from the event's properties
      // data
    } else {
      LOGGER.debug("using default Lucene search index for metadata");
      cec =
          new ContextualEvaluationCriteriaImpl(
              searchPhrase,
              fuzzy,
              caseSensitiveSearch,
              (Directory) contextualMap.get("DEFAULT_INDEX"));
    }

    try {
      return ContextualEvaluator.evaluate(cec);
    } catch (IOException e) {
      LOGGER.debug("IO Exception evaluating context criteria", e);
    } catch (ParseException e) {
      LOGGER.debug("Parse Exception evaluating context criteria", e);
    }

    LOGGER.debug("EXITING: {}", methodName);

    return false;
  }

  public String getSearchPhrase() {
    return searchPhrase;
  }

  public boolean isFuzzy() {
    return fuzzy;
  }

  public boolean isCaseSensitive() {
    return caseSensitiveSearch;
  }

  public boolean hasTextPaths() {
    return textPaths != null && !textPaths.isEmpty();
  }

  public Collection<String> getTextPaths() {
    return textPaths;
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
