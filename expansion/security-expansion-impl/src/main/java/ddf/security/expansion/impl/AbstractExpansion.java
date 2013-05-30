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
package ddf.security.expansion.impl;

import ddf.security.expansion.Expansion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Created with IntelliJ IDEA.
 * User: beyelerb
 * Date: 5/29/13
 * Time: 1:29 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractExpansion implements Expansion
{
    public static final String DEFAULT_VALUE_SEPARATOR = " ";
    public static final String RULE_SPLIT_REGEX = ":";
    protected static final Logger LOGGER = LoggerFactory.getLogger(RegexExpansion.class);
    protected Pattern rulePattern = Pattern.compile(RULE_SPLIT_REGEX);  //("\\[(.+)\\|(.*)\\]");
    protected Map<String, List<String[]>> expansionTable;
    private String key;
    private String attributeSeparator = DEFAULT_VALUE_SEPARATOR;

    @Override
    public Map<String, Set<String>> expand(Map<String, Set<String>> map)
    {
        if ((map == null) || (map.isEmpty()))
            return map;

        Set<String> expandedSet;
        for (String thisKey : map.keySet())
        {
            expandedSet = expand(thisKey, map.get(thisKey));
            map.put(thisKey, expandedSet);
        }

        return map;
    }

    @Override
    public Set<String> expand(String key, Set<String> values)
    {
        Set<String> result;
        // if there's nothing to expand, just return
        if ((values == null) || (values.isEmpty()))
        {
            return values;
        }

        // if no rules have been established yet, return the original
        if ((expansionTable == null) || (expansionTable.isEmpty()))
        {
            return values;
        }

        // if they didn't specify a key value, just return the original string
        if ((key == null) || (key.isEmpty()))
        {
            LOGGER.warn("Expand called with a null key value - no expansion attempted.");
            return values;
        }

        List<String[]> mappingRuleList = expansionTable.get(key);

        // if there are not matching keys in the expansion table - return the original string
        if (mappingRuleList == null)
        {
            return values;
        }

        /*
         * This expansion loop builds on itself, so the order of the rules is important -
         * the expanded set of strings is processed for expansion by subsequent rules.
         *
         * Each list element in the expansion table is a two-element array with the regular
         * expression to search for and the replacement value. The replacement value can be
         * empty in which case the found value is deleted.
         */
        result = values;
        String original;
        String expandedValue;
        Set<String> temp;
        Set<String> expandedSet = new HashSet<String>();
        Set<String> currentSet = new HashSet<String>();
        currentSet.addAll(values);
        LOGGER.debug("Original key of {} with value[s]: {}", key, values);
        for (String[] rule : mappingRuleList)
        {
            expandedSet.clear();
            if ((rule != null) && (rule.length == 2))
            {
                if ((rule[0] != null) && (!rule[0].isEmpty()))
                {
                    LOGGER.trace("Processing expansion entry: {} => {}", rule[0], rule[1]);
                    // now go through and expand each string in the passed in set
                    for (String s : currentSet)
                    {
                        original = s;
                        expandedValue = doExpansion(s, rule);
                        LOGGER.debug("Expanded value from '{}' to '{}'", original, expandedValue);
                        expandedSet.addAll(split(expandedValue, attributeSeparator));
                    }
                }
            } else
            {
                LOGGER.warn("Expansion table contains invalid entries - skipping.");
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

    protected abstract String doExpansion(String original, String[] rule);

    @Override
    public Map<String, List<String[]>> getExpansionMap()
    {
        return Collections.unmodifiableMap(expansionTable);
    }

    public void setExpansionMap(Map<String, List<String[]>> table)
    {
        expansionTable = table;
    }

    public void addExpansionRule(String key, String[] rule)
    {
        if ((key == null) || (key.isEmpty()) || (rule == null) || (rule.length != 2))
        {
            LOGGER.warn("Attempt to add expansion rule with null/empty key or incomplete rule.");
            return;
        }

        if ((rule[0] == null) || (rule[0].isEmpty()))
        {
            LOGGER.warn("Attempt to add expansion rule with null/empty search criteria: {}", rule[0]);
            return;
        }

        if (expansionTable == null)
            expansionTable = new HashMap<String, List<String[]>>();

        List<String[]> list = expansionTable.get(key);
        if (list == null)
        {
            list = new ArrayList<String[]>();
            expansionTable.put(key, list);
        }

        list.add(rule);
    }

    public boolean removeExpansionRule(String key, String[] rule)
    {
        boolean result = false;
        if ((key == null) || (key.isEmpty()) || (rule == null) || (rule.length != 2))
            return false;

        if (expansionTable == null)
            return false;

        List<String[]> list = expansionTable.get(key);
        if (list != null)
        {
            result = list.remove(rule);
            if (list.size() == 0)
                expansionTable.remove(key);
        }
        return result;
    }

    public void addExpansionList(String key, List<String[]> list)
    {
        if ((key == null) || (key.isEmpty()) || (list == null) || (list.size() == 0))
        {
            LOGGER.warn("Attempt to add list of expansion rules with null/empty key or null/empty list.");
            return;
        }

        if (expansionTable == null)
            expansionTable = new HashMap<String, List<String[]>>();

        // put each rule in individually in order to validate each one
        for (String[] rule : list)
        {
            addExpansionRule(key, rule);
        }
    }

    public void setExpansionRules(List<String> rulesList)
    {
        if (expansionTable == null)
            expansionTable = new HashMap<String, List<String[]>>();

        if ((rulesList == null) || (rulesList.isEmpty()))
        {
            expansionTable.clear();
        } else
        {
            String key;
            String[] rule;
            for (String r : rulesList)
            {
                String[] parts = rulePattern.split(r, -2); // allow for empty trailing strings
                if (parts.length >= 2)
                {
                    key = parts[0];
                    rule = new String[2];
                    rule[0] = parts[1];
                    rule[1] = parts.length > 2 ? parts[2] : "";

                    if (parts.length > 3)
                    {
                        LOGGER.warn("Rule being added with more than key:search:replace parts - ignoring the rest - rule: {}", r);
                    }
                    addExpansionRule(key, rule);
                } else
                {
                    LOGGER.warn("Attempt to add rule without enough data - format is key:search[:replace] - rule: '{}'", r);
                }
            }
        }
    }

    /**
     * Takes a string with potentially multiple values and splits it into a collection of strings.
     * @param source  input source string potentially containing multiple tokens
     * @param separator  the sequence separating the individual tokens
     * @return  a collection containing the individual tokens extracted from the specified source string
     */
    protected Collection<String> split(String source, String separator)
    {
        List<String> tmpList = new ArrayList<String>();
        if ((source != null) && (!source.isEmpty()) && (separator != null))
        {
            try
            {
                String[] splitValues = source.split(separator);
                for(String value : splitValues)
                {
                    String tmpValue = value.trim();
                    if (!tmpValue.isEmpty())
                        tmpList.add(tmpValue);
                }
            } catch (PatternSyntaxException e)
            {
                LOGGER.warn("Invalid separator - causing splitting errors - separator: {}", separator);
            }
        }
        return tmpList;
    }

    public void setAttributeSeparator(String separator)
    {
        attributeSeparator = separator;
    }
}
