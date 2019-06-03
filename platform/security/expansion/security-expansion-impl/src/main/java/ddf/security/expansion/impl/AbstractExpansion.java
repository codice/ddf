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
package ddf.security.expansion.impl;

import ddf.security.expansion.Expansion;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all expansion services. Provides the generic setting/getting for attribute
 * separator as well as the actual map of expansion rules. Defines an abstrace <code>doExpansion
 * </code> method to be overridden with the appropriate logic.
 */
public abstract class AbstractExpansion implements Expansion {
  /** Default string that separates individual attributes in the replacement strings. */
  public static final String DEFAULT_VALUE_SEPARATOR = " ";

  /** Separator for various parts of the rules when presented as a string. */
  public static final String RULE_SPLIT_REGEX = ":";

  /** String to mark the line as a comment in the configuration file */
  public static final String CFG_COMMENT_STR = "#";

  /**
   * String to identify the line containing the attribute separator string in the configuration
   * file.
   */
  public static final String SEPARATOR_PREFIX = "separator=";

  /** Default filename for the user attribute mapping configuration file. */
  public static final String DEFAULT_CONFIG_FILE_NAME = "ddf-user-attribute-ruleset.cfg";

  protected static final Logger LOGGER = LoggerFactory.getLogger(RegexExpansion.class);

  private static final String ATTRIBUTE_SEPARATOR = "attributeSeparator";

  private static final String EXPANSION_FILE_NAME = "expansionFileName";

  protected Pattern rulePattern = Pattern.compile(RULE_SPLIT_REGEX); // ("\\[(.+)\\|(.*)\\]");

  protected Map<String, List<String[]>> expansionTable;

  private String attributeSeparator = DEFAULT_VALUE_SEPARATOR;

  private String expansionFilename = DEFAULT_CONFIG_FILE_NAME;

  /*
   * @see ddf.security.expansion.Expansion#expand(Map<String, Set<String>>)
   */
  @Override
  public Map<String, Set<String>> expand(Map<String, Set<String>> map) {
    if ((map == null) || (map.isEmpty())) {
      return map;
    }

    Set<String> expandedSet;
    for (Map.Entry<String, Set<String>> thisKey : map.entrySet()) {
      expandedSet = expand(thisKey.getKey(), thisKey.getValue());
      map.put(thisKey.getKey(), expandedSet);
    }

    return map;
  }

  /*
   * @see ddf.security.expansion.Expansion#expand(String, Set<String>)
   */
  @Override
  public Set<String> expand(String key, Set<String> values) {
    // if there's nothing to expand, just return
    if ((values == null) || (values.isEmpty())) {
      return values;
    }

    // if no rules have been established yet, return the original
    if ((expansionTable == null) || (expansionTable.isEmpty())) {
      return values;
    }

    // if they didn't specify a key value, just return the original string
    if ((key == null) || (key.isEmpty())) {
      LOGGER.debug("Expand called with a null key value - no expansion attempted.");
      return values;
    }

    List<String[]> mappingRuleList = expansionTable.get(key);

    // if there are not matching keys in the expansion table - return the original string
    if (mappingRuleList == null) {
      return values;
    }

    /*
     * This expansion loop builds on itself, so the order of the rules is important - the
     * expanded set of strings is processed for expansion by subsequent rules.
     *
     * Each list element in the expansion table is a two-element array with the regular
     * expression to search for and the replacement value. The replacement value can be empty in
     * which case the found value is deleted.
     */

    String original;
    String expandedValue;
    Set<String> temp;
    Set<String> expandedSet = new HashSet<String>();
    Set<String> currentSet = new HashSet<String>();
    currentSet.addAll(values);
    LOGGER.debug("Original key of {} with value[s]: {}", key, values);
    for (String[] rule : mappingRuleList) {
      expandedSet.clear();
      if ((rule != null) && (rule.length == 2)) {
        if ((rule[0] != null) && (!rule[0].isEmpty())) {
          LOGGER.trace("Processing expansion entry: {} => {}", rule[0], rule[1]);
          // now go through and expand each string in the passed in set
          for (String s : currentSet) {
            original = s;
            expandedValue = doExpansion(s, rule);
            LOGGER.debug("Expanded value from '{}' to '{}'", original, expandedValue);
            expandedSet.addAll(split(expandedValue, attributeSeparator));
          }
        }
      } else {
        LOGGER.debug("Expansion table contains invalid entries - skipping.");
      }
      temp = currentSet;
      currentSet = expandedSet;
      expandedSet = temp;
    }

    LOGGER.debug("Expanded result for key {} is {}", key, currentSet);
    // update the original set passed in for expansion
    values.clear();
    values.addAll(currentSet);
    return currentSet;
  }

  /**
   * This is the method that will do the actual expansion - interpreting the rules and expanding the
   * values. It is abstract and will be overridden by each concrete implementation.
   *
   * @param original the original value of the attribute
   * @param rule the rule that describes the expansion for one specific attribute value
   * @return the (possibly) expanded result of applying the rule to the original value
   */
  protected abstract String doExpansion(String original, String[] rule);

  /*
   * @see ddf.security.expansion.Expansion#getExpansionMap()
   */
  @Override
  public Map<String, List<String[]>> getExpansionMap() {
    if (expansionTable == null) {
      return new HashMap<String, List<String[]>>();
    }

    return Collections.unmodifiableMap(expansionTable);
  }

  /**
   * Sets the expansion map (which includes a set of keys corresponding to attribute names, each
   * with a corresponding list of rules that apply to that attribute. If the passed in table is
   * null, an empty expansion map is created.
   *
   * @param table the complete map of attributes and their corresponding list of rules
   */
  public void setExpansionMap(Map<String, List<String[]>> table) {
    if (table == null) {
      expansionTable = new HashMap<String, List<String[]>>();
    } else {
      expansionTable = table;
    }
  }

  /**
   * Adds an individual expansion rule to the existing (or newly created) expansion map. If an entry
   * already exists for the given attribute key, this rule is added to that entry, if an entry
   * doesn't exist, it is created and then this rule added. If invalid input is received, nothing is
   * done.
   *
   * @param key the attribute for which the corresponding rule should be added
   * @param rule the expansion rule to apply to the corresponding attribute
   */
  public void addExpansionRule(String key, String[] rule) {
    if ((key == null) || (key.isEmpty()) || (rule == null) || (rule.length != 2)) {
      LOGGER.warn("Attempt to add expansion rule with null/empty key or incomplete rule.");
      return;
    }

    if ((rule[0] == null) || (rule[0].isEmpty())) {
      LOGGER.warn("Attempt to add expansion rule with null/empty search criteria: {}", rule[0]);
      return;
    }

    if (expansionTable == null) {
      expansionTable = new HashMap<String, List<String[]>>();
    }

    List<String[]> list = expansionTable.get(key);
    if (list == null) {
      list = new ArrayList<String[]>();
      expansionTable.put(key, list);
    }

    list.add(rule);
  }

  /**
   * Removes a single rule from the expansion map (if it exists). Returns a boolean indicating if
   * the specified rule was successfully removed.
   *
   * @param key the attribute for which the corresponding rule should be removed
   * @param rule the rule to be removed from the list of rules for the corresponding attribute
   * @return true if the rule was found and removed, false otherwise
   */
  public boolean removeExpansionRule(String key, String[] rule) {
    boolean result = false;
    if ((key == null) || (key.isEmpty()) || (rule == null) || (rule.length != 2)) {
      return false;
    }

    if (expansionTable == null) {
      return false;
    }

    List<String[]> list = expansionTable.get(key);
    if (list != null) {
      result = list.remove(rule);
      if (list.isEmpty()) {
        expansionTable.remove(key);
      }
    }
    return result;
  }

  /**
   * Adds a list of rules corresponding to the give key in the expansion map. Convenience method for
   * adding each rule individually.
   *
   * @param key the attribute for which the corresponding list of rules will be added
   * @param list the list of rules to be added to the corresponding attribute
   */
  public void addExpansionList(String key, List<String[]> list) {
    if ((key == null) || (key.isEmpty()) || (list == null) || (list.isEmpty())) {
      LOGGER.warn("Attempt to add list of expansion rules with null/empty key or null/empty list.");
      return;
    }

    if (expansionTable == null) {
      expansionTable = new HashMap<String, List<String[]>>();
    }

    // put each rule in individually in order to validate each one
    for (String[] rule : list) {
      addExpansionRule(key, rule);
    }
  }

  /**
   * Adds a list of rules provided in String form. This is a convenience method for adding each rule
   * individually.
   *
   * @param rulesList list of rules (in String form) to be added to the expansion map
   */
  public void setExpansionRules(List<String> rulesList) {
    if (expansionTable == null) {
      expansionTable = new HashMap<String, List<String[]>>();
    }

    if ((rulesList == null) || (rulesList.isEmpty())) {
      expansionTable.clear();
    } else {
      String key;
      String[] rule;
      for (String r : rulesList) {
        addExpansionRule(r);
      }
    }
  }

  /**
   * Adds an individual rule (expressed in String form) The String form of the rule is a three-part
   * string with each part separated by a colon (:)<br>
   * &lt;attribute name&gt;:&lt;original value&gt;:&lt;replacement value&gt;
   *
   * @param ruleStr the rule to be added (in String form)
   */
  private void addExpansionRule(String ruleStr) {
    String key;
    String[] rule;
    String[] parts = rulePattern.split(ruleStr, -2); // allow for empty trailing strings
    if (parts.length >= 2) {
      key = parts[0];
      rule = new String[2];
      rule[0] = parts[1];
      rule[1] = parts.length > 2 ? parts[2] : "";

      if (parts.length > 3) {
        LOGGER.warn(
            "Rule being added with more than key:search:replace parts - ignoring the rest - rule: {}",
            ruleStr);
      }
      addExpansionRule(key, rule);
    } else {
      LOGGER.warn(
          "Attempt to add rule without enough data - format is key:search[:replace] - rule: '{}'",
          ruleStr);
    }
  }

  /**
   * Takes a string with potentially multiple values and splits it into a collection of strings.
   *
   * @param source input source string potentially containing multiple tokens
   * @param separator the sequence separating the individual tokens
   * @return a collection containing the individual tokens extracted from the specified source
   *     string
   */
  protected Collection<String> split(String source, String separator) {
    List<String> tmpList = new ArrayList<String>();
    if ((source != null) && (!source.isEmpty()) && (separator != null)) {
      try {
        String[] splitValues = source.split(separator);
        for (String value : splitValues) {
          String tmpValue = value.trim();
          if (!tmpValue.isEmpty()) {
            tmpList.add(tmpValue);
          }
        }
      } catch (PatternSyntaxException e) {
        LOGGER.warn("Invalid separator - causing splitting errors - separator: {}", separator);
      }
    }
    return tmpList;
  }

  /**
   * Sets the separator to be used in splitting up replacement strings. If a null or empty value is
   * passed in, the default separator (a space) is used.
   *
   * @param separator the separator to be used to split up replacement strings
   */
  public void setAttributeSeparator(String separator) {
    if ((separator == null) || (separator.isEmpty())) {
      attributeSeparator = DEFAULT_VALUE_SEPARATOR;
    } else {
      attributeSeparator = separator;
    }
  }

  /**
   * Sets the name of the configuration file defining the attribute separator and the mapping of
   * attributes to their expanded values. This file is read initially, and whenever the file name is
   * set. If the name is null or empty, the existing map of rules is cleared.
   *
   * @param filename the name of the configuration file to be loaded into the expansion service
   */
  public void setExpansionFileName(String filename) {
    if ((filename != null) && (!filename.isEmpty())) {
      expansionFilename = filename;
      LOGGER.info("Loading mapping rulesets from configuration file: {}", filename);
      loadConfiguration(expansionFilename);
    } else {
      LOGGER.warn(
          "Null or empty mapping configuration file name: {} - clearing existing map.", filename);
      expansionTable.clear();
    }
  }

  public void update(Map<String, String> properties) {
    LOGGER.debug("Updating Expansion Properties.");
    if (properties != null) {
      if (properties.containsKey(ATTRIBUTE_SEPARATOR)) {
        setAttributeSeparator(properties.get(ATTRIBUTE_SEPARATOR));
      }
      if (properties.containsKey(EXPANSION_FILE_NAME)) {
        setExpansionFileName(properties.get(EXPANSION_FILE_NAME));
      }
    }
  }

  /**
   * Does the work of reading the configuration file and configuring the expansion map and attribute
   * separator.
   *
   * @param filename the name of the file to be read and processed
   */
  protected void loadConfiguration(String filename) {

    if (filename == null) {
      setExpansionMap(null);
      return;
    }

    // first clear out the existing table
    if (expansionTable != null) {
      expansionTable.clear();
    }
    File file = null;
    filename = StringUtils.strip(filename);
    if (!Paths.get(filename).isAbsolute()) {
      // relative path
      String relPath = System.getProperty("ddf.home");
      if (StringUtils.isBlank(relPath)) {
        LOGGER.warn(
            "ddf.home property was not set or is NULL, loading of properties may be impacted.");
      }
      file = new File(relPath, filename);
    } else {
      // absolute path
      file = new File(filename);
    }
    try (BufferedReader br =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8.name()))) {
      String line;
      while ((line = br.readLine()) != null) {
        if ((line.length() > 0) && (!line.startsWith(CFG_COMMENT_STR))) {
          if (line.startsWith(SEPARATOR_PREFIX)) {
            if (line.length() > SEPARATOR_PREFIX.length()) {
              attributeSeparator = line.substring(SEPARATOR_PREFIX.length());
            } else {
              attributeSeparator = DEFAULT_VALUE_SEPARATOR;
            }
          } else {
            addExpansionRule(line);
          }
        }
      }
      LOGGER.debug("Finished loading mapping configuration file.");
    } catch (IOException e) {
      LOGGER.warn("Unexpected exception reading mapping configuration file {}", filename, e);
      setExpansionMap(null);
    }
  }
}
