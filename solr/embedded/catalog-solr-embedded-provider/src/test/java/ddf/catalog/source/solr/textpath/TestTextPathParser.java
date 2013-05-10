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
package ddf.catalog.source.solr.textpath;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

import ddf.catalog.source.solr.textpath.TextPathNode;
import ddf.catalog.source.solr.textpath.TextPathParser;

public class TestTextPathParser {
	private static final Logger LOGGER = Logger.getLogger(TestTextPathParser.class);

	@Test
	public void testPositives() {

		List<String> inputs = new ArrayList<String>();
		inputs.add("_");
		inputs.add("c");
		inputs.add("comment");
		inputs.add("_comment");
		inputs.add("/comment");
		inputs.add("//comment");
		inputs.add("/purchaseOrder/comment");
		inputs.add("/purchaseOrder//comment");
		inputs.add("/purchaseOrder/items//comment");
		inputs.add("/purchaseOrder/items/item/comment");
		inputs.add("/purchaseOrder//item/USPrice");
		inputs.add("/purchaseOrder//item/USPrice=\"148.95\"");
		// Relative EqualityExpr
		inputs.add("//items/item[@partNum=\"872-AA\"]/comment");
		inputs.add("//items/item[@partNum!=\"872-AA\"]/comment");
		inputs.add("//items/item[\"872-AA\"=@partNum]/comment");
		inputs.add("//items/item[\"872-AA\"!=@partNum]/comment");
		inputs.add("//items/item[@partNum=\"872-AA\" and @a='b']/comment");
		inputs.add("//items/item[@partNum=\"872-AA\" or @a='b']/comment");
		// Absolute EqualityExpr
		inputs.add("items/item[/partNum=\"872-AA\"]/comment");
		inputs.add("items/item[/partNum!=\"872-AA\"]/comment");
		inputs.add("items/item[\"872-AA\"=/partNum]/comment");
		inputs.add("items/item[\"872-AA\"!=/partNum]/comment");
		inputs.add("items/item[/partNum=\"872-AA\" and @a='b']/comment");
		inputs.add("items/item[/partNum=\"872-AA\" or @a='b']/comment");

		inputs.add("/purchaseOrder/@orderDate");
		inputs.add("/*/*/item[.//comment]");
		inputs.add("purchaseOrder/items/item");
		inputs.add("/purchaseOrder/items/item");
		inputs.add("./purchaseOrder/items/item");
		inputs.add("chapter[title=\"Introduction\"]");
		inputs.add("chapter[not(title)]");
		inputs.add("chapter[(title)]");
		inputs.add("chapter[(content/title)]");
		inputs.add("chapter[content/title]");
		inputs.add("chapter-section[(title)]");
		inputs.add("chapter[(title='a')]");
		inputs.add("chapter[(title='a' or b='a') or c='a' and d='b']");
		inputs.add("chapter[title='a\"b\"']");
		inputs.add("employee[@secretary and @assistant]");
		inputs.add("/purchaseOrder/comment[2]");
		inputs.add("/ds:Root/ds:sy/@IC:rTo");
		inputs.add("/ds:Root/ds:sy//@IC:rTo");
		inputs.add("/ds:Root/ds:sy[@IC:rTo]");

		for (String input : inputs) {
			TextPathParser parser = Parboiled.createParser(TextPathParser.class);

			ParsingResult<?> result = new ReportingParseRunner(parser.TextPath()).run(input);

			assertEquals("Failed on input [" + input + "]. Parse Error [" + getErrorOutput(result) + "]", 0,
					result.parseErrors.size());
			assertEquals("Failed to parse [" + input + "] properly.", input,
					ParseTreeUtils.getNodeText(result.parseTreeRoot, result.inputBuffer));

		}

	}

	@Test
	public void testNegatives() {

		List<String> inputs = new ArrayList<String>();
		inputs.add("0");
		inputs.add("-comment");
		inputs.add("9comment");
		inputs.add("..//comment");
		inputs.add("//item[@price > 2*@discount]");
		inputs.add("para[@type=\"warning\"][5]");
		inputs.add("@*");
		inputs.add("para[last()]");
		inputs.add("not(/purchaseOrder/@orderDate)");
		inputs.add("/purchaseOrder/comment[-2]");
		inputs.add("/*/@*");

		for (String input : inputs) {
			TextPathParser parser = Parboiled.createParser(TextPathParser.class);

			ParsingResult<?> result = new ReportingParseRunner(parser.TextPath()).run(input);

			assertThat("[" + input + "] should have failed.", result.parseErrors.size(), greaterThan(0));

		}

	}

	@Test
	public void testSpacing() {
	
		List<String> inputs = new ArrayList<String>();
		inputs.add("  //  comment");
		inputs.add("/ purchaseOrder/ comment");
		inputs.add("//items/item[@  partNum  = \"872-AA\"]/comment");
		inputs.add("//items/item[@partNum !=  \"872-AA\"] / comment");
		inputs.add("//items/item[ @partNum=\"872-AA\"   \nand @a='b']/comment");
		inputs.add("/*/*/item[ .  //  comment]");
		inputs.add("\t.\t/purchaseOrder/items/item");
		inputs.add("chapter[ title=\" Introduction   \"]");
		inputs.add("chapter[  not  (  title  )  ]");
		inputs.add("chapter[  (  title='  a  '  )  ]  ");
		inputs.add("chapter[(title='a'   or    b='a') or  c='a'   and   d='b '    ]");
		inputs.add(" / purchaseOrder / comment [ 2  ]");
	
		for (String input : inputs) {
			TextPathParser parser = Parboiled.createParser(TextPathParser.class);
	
			ParsingResult<?> result = new ReportingParseRunner(parser.TextPath()).run(input);
	
			assertEquals("Failed on input [" + input + "]. Parse Error [" + getErrorOutput(result) + "]", 0,
					result.parseErrors.size());
			assertEquals("Failed to parse [" + input + "] properly.", input,
					ParseTreeUtils.getNodeText(result.parseTreeRoot, result.inputBuffer));
	
		}
	
	}

	// We have been using this for debugging purposes, its not meant to be a test.
	@Test
	@Ignore
	public void trace() {
		List<String> inputs = new ArrayList<String>();
		inputs.add("/rss//items");

		for (String input : inputs) {
		    TextPathParser parser = Parboiled.createParser(TextPathParser.class);
			ParsingResult<TextPathNode> result = new TracingParseRunner<TextPathNode>(parser.TextPath()).run(input);

			assertNotNull("Parse result was null for: " + input, result.resultValue);
			LOGGER.info("Parse result: " + result.resultValue.getValue());
			
			visualize(result);
		}
	}

	/**
	 * Use this method when you want the tree to be printed to System.out for
	 * debugging purposes
	 * 
	 * @param result
	 */
	protected void visualize(ParsingResult<?> result) {

		String output = ParseTreeUtils.printNodeTree(result);

		LOGGER.info(output);

		LOGGER.info("PARSE ERROR: "
				+ (!result.parseErrors.isEmpty() ? ErrorUtils.printParseError(result.parseErrors.get(0)) : "NOTHING"));

	}

	protected String getErrorOutput(ParsingResult<?> result) {

		if (!result.parseErrors.isEmpty()) {
			return ErrorUtils.printParseError(result.parseErrors.get(0));
		}
		return "";
	}

}
