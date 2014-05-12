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
package ddf.security.expansion.impl;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StraightExpansionImplTest {

    /*
     * Rules for the various tests \A and \z represent the start of input and end of input
     */
    private String[] rule1a = new String[] {"VP-Sales", "VP-Sales VP Sales"};

    private String[] rule1b = new String[] {"VP-Engineering", "VP-Engineering VP Engineering"};

    private String[] rule1c = new String[] {"VP-Finance", "VP-Finance VP Finance"};

    private String[] rule2 = new String[] {"VP", "VP Manager"};

    private String[] rule3 = new String[] {"Manager", "Manager Employee"};

    private String[] rule4 = new String[] {"VP", ""};

    private String[] rule5 = new String[] {"AZ", "AZ Arizona"};

    private String[] rule6a = new String[] {"AZ", "AZ USA"};

    private String[] rule6b = new String[] {"VA", "VA USA"};

    private String[] rule6c = new String[] {"MD", "MD USA"};

    private String rule1aStr = "VP-Sales:VP-Sales VP Sales";

    private String rule1bStr = "VP-Engineering:VP-Engineering VP Engineering";

    private String rule1cStr = "VP-Finance:VP-Finance VP Finance";

    private String rule2Str = "VP:VP Manager";

    private String rule3Str = "Manager:Manager Employee";

    private String rule4Str = "VP:";

    public List<String[]> rulesList1 = new ArrayList<String[]>();

    public List<String[]> rulesList2 = new ArrayList<String[]>();

    public Map<String, List<String[]>> testmap = new HashMap<String, List<String[]>>();

    @Before
    public void setupData() {
        rulesList1.add(rule1a);
        rulesList1.add(rule1b);
        rulesList1.add(rule1c);
        rulesList1.add(rule2);
        rulesList1.add(rule3);

        rulesList2.add(rule4);

        testmap.put("role", rulesList1);
    }

    private void assertMapsAreEqual(Map<String, List<String[]>> m1, Map<String, List<String[]>> m2) {
        assert (m1.size() == m2.size());
        assert (m1.keySet().equals(m2.keySet()));
        for (String k : m1.keySet()) {
            List<String[]> l1 = m1.get(k);
            List<String[]> l2 = m2.get(k);
            Iterator iter = l2.iterator();
            String[] a2;
            for (String[] a1 : l1) {
                a2 = (String[]) iter.next();
                assert (Arrays.equals(a1, a2));
            }
        }
    }

    @Test
    public void testExpandSimple() throws Exception {
        HashSet<String> testSet = new HashSet<String>();
        StraightExpansionImpl exp = new StraightExpansionImpl();
        Set<String> result;

        testSet.add("staff");
        // no rules defined - should return the same set
        exp.expand(null, testSet);
        assert (testSet.size() == 1);
        assert (testSet.contains("staff"));

        // add a rule for the "role" key
        String[] rule = new String[] {"staff", "staff employee"};
        exp.addExpansionRule("role", rule);

        // calling with unknown key - should return the same set
        exp.expand("name", testSet);
        assert (testSet.size() == 1);
        assert (testSet.contains("staff"));

        // calling with null values - returns null
        result = exp.expand("role", null);
        assert (result == null);

        exp.removeExpansionRule("role", rule);
        testSet.clear();
        exp.addExpansionRule("role", rule1a);
        exp.addExpansionRule("role", rule1b);
        exp.addExpansionRule("role", rule1c);
        exp.addExpansionRule("role", rule2);
        exp.addExpansionRule("role", rule3);
        testSet.add("VP-Sales");
        exp.expand("role", testSet);
        assert (testSet.size() == 5);
        assert (testSet.contains("VP-Sales"));
        assert (testSet.contains("VP"));
        assert (testSet.contains("Sales"));
        assert (testSet.contains("Manager"));
        assert (testSet.contains("Employee"));

        testSet.clear();
        exp = new StraightExpansionImpl();
        exp.addExpansionRule("role", new String[] {"VP", ""});
        testSet.add("VP-Sales");
        testSet.add("VP");
        // calling with empty replacement string - should delete matched string
        exp.expand("role", testSet);
        assert (testSet.size() == 1);
        assert (testSet.contains("VP-Sales"));
    }

    @Test
    public void testExpandMap() throws Exception {
        StraightExpansionImpl exp = new StraightExpansionImpl();

        // build the data map
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        HashSet<String> roleSet = new HashSet<String>();
        roleSet.add("VP-Sales");
        HashSet<String> locSet = new HashSet<String>();
        locSet.add("Goodyear");
        locSet.add("AZ");
        map.put("role", roleSet);
        map.put("location", locSet);

        // test with null rule set and valid data - should leave data untouched
        exp.expand(map);
        Set<String> result = map.get("role");
        assert (result.size() == 1);
        assert (result.contains("VP-Sales"));
        result = map.get("location");
        assert (result.size() == 2);
        assert (result.contains("Goodyear"));
        assert (result.contains("AZ"));

        // build the expansion rule set
        exp.setExpansionMap(testmap);
        exp.addExpansionRule("location", rule5);
        exp.addExpansionRule("location", rule6a);
        exp.addExpansionRule("location", rule6b);
        exp.addExpansionRule("location", rule6c);

        // test with valid rules but null data - should return null
        Map<String, Set<String>> resultMap = exp.expand(null);
        assert (resultMap == null);

        // test with valid rules but empty data - should return empty data
        resultMap = exp.expand(new HashMap<String, Set<String>>());
        assert (resultMap.size() == 0);

        // test with valid rules and valid data
        resultMap = exp.expand(map);
        // should return the exact same object (updated)
        assert (resultMap == map);

        result = resultMap.get("role");
        assert (result.size() == 5);
        assert (result.contains("VP-Sales"));
        assert (result.contains("VP"));
        assert (result.contains("Sales"));
        assert (result.contains("Manager"));
        assert (result.contains("Employee"));

        result = resultMap.get("location");
        assert (result.size() == 4);
        assert (result.contains("Goodyear"));
        assert (result.contains("AZ"));
        assert (result.contains("Arizona"));
        assert (result.contains("USA"));

        // verify the regex rules work - don't expand unless the entire token
        // build the data map
        map.clear();
        locSet.clear();
        locSet.add("AZTEC");
        map.put("location", locSet);

        resultMap = exp.expand(map);
        result = resultMap.get("location");
        assert (result.size() == 1);
        assert (result.contains("AZTEC"));
    }

}
