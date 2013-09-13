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
package com.lmco.ddf.opensearch.query;

import com.lmco.ddf.endpoints.ASTNode;
import com.lmco.ddf.endpoints.KeywordFilterGenerator;
import com.lmco.ddf.endpoints.KeywordTextParser;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.opengis.filter.Filter;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestKeywordTextParser {
    // private static final Logger LOGGER = Logger.getLogger(TestKeywordTextParser.class);
    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(OpenSearchQueryTest.class));

    @Rule
    public MethodRule watchman = new TestWatchman() {
        public void starting(FrameworkMethod method) {
            LOGGER.debug("***************************  STARTING: {}  **************************",
                    method.getName());
        }

        public void finished(FrameworkMethod method) {
            LOGGER.debug("***************************  END: {}  **************************",
                    method.getName());
        }
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testPositives() {

        // TODO add more expressions to test

        List<String> inputs = new ArrayList<String>();
        inputs.add("נּ€");
        inputs.add("A AND B");
        inputs.add("A AND   B AND C");
        inputs.add("A OR B");
        inputs.add("(A OR B)  ");
        inputs.add("  (\"B\")");
        inputs.add("(  B)");
        inputs.add("(\"A OR B  \"  )");
        inputs.add("A NOT B");
        inputs.add("A B");
        inputs.add("AND");
        inputs.add("\"AND\"");
        inputs.add("test tester3");
        inputs.add("A B OR C");
        inputs.add("A \"B\" OR C");
        inputs.add("A \"test\" OR C");
        inputs.add("A B OR \"C\"");
        inputs.add("$%'^&_+` ''''veryInterest*ingKeyword!@#$%^&_+-=`~1234567890[]{}\\|?/><,.:; OR %^&_+-=*`~123");
        inputs.add("test TeSt1 OR another3test");
        inputs.add("A B C D E A N D N O T O R F G H I \"J 1 2 3 4 5 6\" 2 * 7 8 9 # $ % ^ ! } [ ` ; ' <");
        inputs.add("(\"A14356377856nyin8o789l;;l453 234l56;23$$#%#$@^#@&&!\" B) OR C");
        inputs.add("(A AND B) OR C");
        inputs.add("(A AND B) NOT ((C OR D) AND (B NOT A)) OR E");
        inputs.add("A AND B AND C AND D OR E NOT F");
        inputs.add("A B AND C D OR E NOT F");
        inputs.add("(A B AND C D) OR E NOT F");
        inputs.add("A NOT A");
        inputs.add("A (NOT C) D");
        inputs.add("(((Apple) AND (((Orange) OR Banana))))");
        inputs.add("A  B");
        inputs.add("(\"A14356377856nyin8o789l;;l453      234l56;23$$#%#$@^#@&&!\" B) OR C");
        inputs.add("( A )");
        inputs.add(" AND ");
        inputs.add("    keyword2       ");
        inputs.add(" AND");
        inputs.add("AND ");
        inputs.add(" AND ");
        inputs.add("\" AND \"");

        inputs.add("(\"Keyword \")");
        inputs.add("\" Keyword\"");
        inputs.add("( \"Keyword\")");
        inputs.add("A ( OR ) B");
        inputs.add("((((((((((((\"stuff   stuff2\"))))))))))) OR C)");
        inputs.add("(\"A14356377856nyin8o789l;;l453      234l56;23$$#%#$@^#@&&!\" B) OR C");

        for (String input : inputs) {
            KeywordTextParser parser = Parboiled.createParser(KeywordTextParser.class);

            ParsingResult<?> result = new ReportingParseRunner(parser.InputPhrase()).run(input);
            LOGGER.debug("input = " + input + "\t\t=====>result matched = " + result.matched);
            assertEquals("Failed on input [" + input + "]. Parse Error [" + getErrorOutput(result)
                    + "]", 0, result.parseErrors.size());
            assertEquals("Failed to parse [" + input + "] properly.", input,
                    ParseTreeUtils.getNodeText(result.parseTreeRoot, result.inputBuffer));

        }

    }

    @Test
    public void testNegatives() {
        // TODO add more expressions to test

        List<String> inputs = new ArrayList<String>();

        // these should fail even with loose parsing
        inputs.add("");
        inputs.add("()");
        inputs.add("( )");
        inputs.add("  ");

        inputs.add("( Keyword");
        inputs.add("(Keyword");
        inputs.add("Keyword)");
        inputs.add("Keyword )");
        inputs.add("product2 NOT))(((( anothertitle");
        inputs.add("(A AND B) NOT ((C OR D) AND (B NOT A) OR E");
        inputs.add("(A AND B) NOT ((C\" AND (B NOT A)) OR E");
        inputs.add("(A AND B) NOT (\"C\" AND \"B)) OR E");
        inputs.add("(A AND B) NOT (\"A \"C\"\" AND (B)) OR E");
        inputs.add("(\"A)()()(((()))()((()))))()(((((()))((\" B) OR C"); // this could be made valid
                                                                         // if an escape character
                                                                         // were introduced
        inputs.add("() (stuff) OR C");
        inputs.add("(((((((((((stuff))))))))))) OR C)"); // one missing leading parenthesis
        inputs.add("((((((((((((stuff)))))))))) OR C)"); // one missing trailing parenthesis
        inputs.add("((((((((((((\"stuff (stuff2)\"))))))))))) OR C)");
        inputs.add("(\"A)()()(((()))()((()))))()(((((()))((\" B) OR C");

        inputs.add("\"\"");
        inputs.add("\"");

        for (String input : inputs) {
            KeywordTextParser parser = Parboiled.createParser(KeywordTextParser.class);

            ParsingResult<?> result = new ReportingParseRunner(parser.InputPhrase()).run(input);

            LOGGER.debug("input = " + input + "\t\t=====>result matched = " + result.matched);

            assertThat("[" + input + "] should have failed.", result.parseErrors.size(),
                    greaterThan(0));

        }

    }

    @Test
    public void testSpacing() {
        // TODO add more expressions to test
        List<String> inputs = new ArrayList<String>();
        inputs.add(" A B OR C");
        inputs.add(" A B OR C ");
        inputs.add(" A B  OR C ");
        inputs.add(" A       B OR C");
        inputs.add("      A B OR C");
        inputs.add("A B      OR            C           ");
        inputs.add("    A                 B   OR   C     NOT D AND           E   ");

        for (String input : inputs) {
            KeywordTextParser parser = Parboiled.createParser(KeywordTextParser.class);

            ParsingResult<?> result = new ReportingParseRunner(parser.InputPhrase()).run(input);

            assertEquals("Failed on input [" + input + "]. Parse Error [" + getErrorOutput(result)
                    + "]", 0, result.parseErrors.size());
            assertEquals("Failed to parse [" + input + "] properly.", input,
                    ParseTreeUtils.getNodeText(result.parseTreeRoot, result.inputBuffer));

        }

    }

    // We have been using this for debugging purposes, its not meant to be a test.
    @Ignore
    @Test
    public void trace() {
        Map<String, String> inputToOutput = new LinkedHashMap<String, String>();

        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

        List<String> inputs = new ArrayList<String>();
        // inputs.add("A \"(test test2)\" OR test2");
        inputs.add("A B  C D");

        for (String input : inputs) {
            KeywordTextParser parser = Parboiled.createParser(KeywordTextParser.class);

            ParsingResult<ASTNode> result = new TracingParseRunner(parser.InputPhrase()).run(input);
            // ParsingResult<ASTNode> result = new
            // ReportingParseRunner(parser.InputPhrase()).run(input);

            KeywordFilterGenerator kfg = new KeywordFilterGenerator(filterBuilder);
            Filter filter = kfg.getFilterFromASTNode(result.resultValue);

            inputToOutput.put(input, filter.toString());
            // visualize(result);
        }

        for (Map.Entry<String, String> iteration : inputToOutput.entrySet()) {
            System.out.println(iteration.getKey() + " : " + iteration.getValue());
        }
    }

    /**
     * Use this method when you want the tree to be printed to System.out for debugging purposes
     * 
     * @param result
     */
    protected void visualize(ParsingResult<?> result) {

        String output = ParseTreeUtils.printNodeTree(result);

        System.out.println(output);

        System.out.println("PARSE ERROR: "
                + (!result.parseErrors.isEmpty() ? ErrorUtils.printParseError(result.parseErrors
                        .get(0)) : "NOTHING"));

    }

    protected String getErrorOutput(ParsingResult<?> result) {

        if (!result.parseErrors.isEmpty()) {
            return ErrorUtils.printParseError(result.parseErrors.get(0));
        }
        return "";
    }

}
