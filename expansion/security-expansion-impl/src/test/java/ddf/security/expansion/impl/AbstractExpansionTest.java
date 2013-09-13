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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Test the methods of the abstract class here (using instances of StraightExpansionImpl.
 */
public class AbstractExpansionTest {
    private static final Logger logger = Logger.getLogger(AbstractExpansionTest.class);

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

    private String rule1aStr = "role:VP-Sales:VP-Sales VP Sales";

    private String rule1bStr = "role:VP-Engineering:VP-Engineering VP Engineering";

    private String rule1cStr = "role:VP-Finance:VP-Finance VP Finance";

    private String rule2Str = "role:VP:VP Manager";

    private String rule3Str = "role:Manager:Manager Employee";

    private String rule4Str = "xyz:VP:";

    private String ruleBogus1 = "xyz";

    private String ruleBogus2 = "xyz:";

    private String ruleBogus3 = "xyz:123:456:789:10";

    public List<String[]> rulesList1 = new ArrayList<String[]>();

    public List<String[]> rulesList2 = new ArrayList<String[]>();

    public Map<String, List<String[]>> testmap = new HashMap<String, List<String[]>>();

    @BeforeClass
    public static void setupLogging() {
        BasicConfigurator.configure();
        logger.setLevel(Level.TRACE);
    }

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
    public void testSetGetExpansionTable() {
        StraightExpansionImpl exp = new StraightExpansionImpl();
        exp.setExpansionMap(testmap);
        Map<String, List<String[]>> map = exp.getExpansionMap();
        assertMapsAreEqual(map, testmap);
    }

    @Test
    public void testAddExpansionRules() {
        StraightExpansionImpl exp = new StraightExpansionImpl();
        Map<String, List<String[]>> map = new HashMap<String, List<String[]>>();
        exp.addExpansionList("role", rulesList1);
        assertMapsAreEqual(exp.getExpansionMap(), testmap);

        exp = new StraightExpansionImpl();
        exp.addExpansionRule("role", rule1a);
        exp.addExpansionRule("role", rule1b);
        exp.addExpansionRule("role", rule1c);
        exp.addExpansionRule("role", rule2);
        exp.addExpansionRule("role", rule3);
        assertMapsAreEqual(exp.getExpansionMap(), testmap);

        exp.addExpansionRule("xyz", rule4);
        assert (exp.getExpansionMap().size() == (testmap.size() + 1));
        exp.removeExpansionRule("xyz", rule4);
        assertMapsAreEqual(exp.getExpansionMap(), testmap);

    }

    @Test
    public void testSetExpansionRules() {
        StraightExpansionImpl exp = new StraightExpansionImpl();
        Map<String, List<String[]>> map = new HashMap<String, List<String[]>>();
        exp = new StraightExpansionImpl();
        List<String> listOfRules = new ArrayList<String>();
        listOfRules.add(rule1aStr);
        listOfRules.add(rule1bStr);
        listOfRules.add(rule1cStr);
        listOfRules.add(rule2Str);
        listOfRules.add(rule3Str);
        exp.setExpansionRules(listOfRules);
        assertMapsAreEqual(exp.getExpansionMap(), testmap);

        exp = new StraightExpansionImpl();
        listOfRules.clear();
        listOfRules.add(rule4Str);
        exp.setExpansionRules(listOfRules);
        map = exp.getExpansionMap();
        assert (map.containsKey("xyz"));
        List<String[]> list = map.get("xyz");
        assert (list.size() == 1);
        String[] rules = list.get(0);
        assert (rules[0].equals("VP"));
        assert (rules[1].equals(""));

        exp = new StraightExpansionImpl();
        listOfRules.clear();
        listOfRules.add(ruleBogus1);
        listOfRules.add(ruleBogus2);
        listOfRules.add(ruleBogus3);
        exp.setExpansionRules(listOfRules);
        map = exp.getExpansionMap();
        assert (map.size() == 1);
        list = map.get("xyz");
        assert (list.size() == 1);
        rules = list.get(0);
        assert (rules[0].equals("123"));
        assert (rules[1].equals("456"));

    }

    @Test
    public void testSplit() throws Exception {
        StraightExpansionImpl exp = new StraightExpansionImpl();

        Collection<String> result = exp.split("A B C", " ");
        assert (result.size() == 3);
        assert (result.contains("A"));
        assert (result.contains("B"));
        assert (result.contains("C"));

        String test = "A|B|C";
        result = exp.split(test, " ");
        assert (result.size() == 1);
        assert (result.contains(test));

        result = exp.split(test, "\\|");
        assert (result.size() == 3);
        assert (result.contains("A"));
        assert (result.contains("B"));
        assert (result.contains("C"));

        result = exp.split("", " ");
        assert (result.size() == 0);

        result = exp.split(null, " ");
        assert (result.size() == 0);

        result = exp.split(test, null);
        assert (result.size() == 0);

        result = exp.split("A|B||C", "\\|");
        assert (result.size() == 3);
        assert (result.contains("A"));
        assert (result.contains("B"));
        assert (result.contains("C"));

        result = exp.split("A01B2C3D4", "[0-9]");
        assert (result.size() == 4);
        assert (result.contains("A"));
        assert (result.contains("B"));
        assert (result.contains("C"));
        assert (result.contains("D"));
    }

    @Test
    public void testLoadConfiguration() {
        Map<String, List<String[]>> map = new HashMap<String, List<String[]>>();
        StraightExpansionImpl exp = new StraightExpansionImpl();

        URL testConfigFile = ClassLoader.getSystemResource("testExpansionConfig.cfg");
        String filename = testConfigFile.getFile();
        exp.loadConfiguration(filename);

        map = exp.getExpansionMap();
        assertMapsAreEqual(map, testmap);

        // make sure exisitng rules get cleared on reload
        exp.addExpansionRule("xyz", rule4);

        exp.loadConfiguration(filename);
        map = exp.getExpansionMap();
        assertMapsAreEqual(map, testmap);

        exp.loadConfiguration("");
        map = exp.getExpansionMap();
        assert (map.isEmpty());

        exp.loadConfiguration(null);
        map = exp.getExpansionMap();
        assert (map.isEmpty());

        exp = new StraightExpansionImpl();
        exp.loadConfiguration(null);
        map = exp.getExpansionMap();
        assert (map.isEmpty());
    }
}
