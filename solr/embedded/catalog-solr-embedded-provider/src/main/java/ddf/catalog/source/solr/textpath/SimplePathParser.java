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

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.DontLabel;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.StringVar;

import ddf.catalog.source.solr.textpath.SimplePathNode.NodeType;

/**
 * SimplePathParser is a subset of the TextPath BNF that focuses on paths only.
 * <br/><br/>
 * BNF taken from Oracle's 11g TextPath Specification
 * <a href="http://docs.oracle.com/cd/E11882_01/appdev.112/e16659/xdb09sea.htm#i1006756">
 * http://docs.oracle.com/cd/E11882_01/appdev.112/e16659/xdb09sea.htm#i1006756</a>
 * <br/>
 * Parser is written in the Parboiled style which does not adhere to all Java
 * naming conventions. See <a
 * href="https://github.com/sirthias/parboiled/wiki/Style-Guide">Parboiled
 * Styling Guide</a> for more details.
 * 
 * <br/>
 * Corrected the Oracle BNF to allow NCName to accept a colon as a valid
 * character (It is not part of the BNF but Oracle DB allows it). <br/>
 * <br/>
 * Also order might have been changed in some of the rules to force the parser
 * to explore paths that would not be explored because they are subsets of a
 * Rule. For example, the possibility of <code>//</code> must be searched before
 * <code>/</code> because <code>/</code> would be matched first and
 * <code>//</code> would never be explored.
 * 
 * @author Ashraf Barakat
 * @author Phillip Klinefelter
 * @author ddf.isgs@lmco.com
 * 
 */
@SuppressWarnings({ "InfiniteRecursion" })
@BuildParseTree
public class SimplePathParser extends BaseParser<SimplePathNode> {

	/**
	 * Single forward slash '/'
	 */
    final Rule FS = Terminal("/");

	/**
	 * Double forward slash '//'
	 */
	final Rule DFS = Terminal("//");
	final Rule DOT = Terminal(".");
	final Rule DASH = Terminal("-");
	final Rule UNDERSCORE = Terminal("_");
	final Rule COLON = Terminal(":");
	final Rule STAR = Terminal("*");
	final Rule LBRK = Terminal("[");
	final Rule RBRK = Terminal("]");

	/**
	 * Original Oracle BNF excerpt <br/>
	 * HasPathArg ::= LocationPath | EqualityExpr
	 * 
     * <p>
     * The implementation was changed for parsing performance gain and simplicity. <br/>
     * HasPathArg ::= LocationPath
     * </p>
	 * 
	 * @return
	 */
	public Rule TextPath() {
		return Sequence(LocationPath(), EOI, push(new SimplePathNode(NodeType.TEXT_PATH, pop(), null)));
	}

	/**
	 * Original Oracle BNF excerpt <br/>
	 * LocationPath ::= RelativeLocationPath | AbsoluteLocationPath
	 */
	@DontLabel
	Rule LocationPath() {
        return FirstOf(Sequence(RelativeLocationPath(), push(new SimplePathNode(NodeType.STEP_PATH, pop(), null))),
                Sequence(AbsoluteLocationPath(), push(new SimplePathNode(NodeType.STEP_PATH, pop(), null))));
	}

	/**
	 * Original Oracle BNF excerpt <br/>
	 * AbsoluteLocationPath ::= ("/" RelativeLocationPath) | ("//"
	 * RelativeLocationPath)
	 * 
	 * <p>
	 * The implementation was changed for parsing performance gain.<br/>
	 * AbsoluteLocationPath ::= ( ("//" | "/") RelativeLocationPath)
	 * </p>
	 */
	Rule AbsoluteLocationPath() {
	    StringVar slash = new StringVar();
		return Sequence(FirstOf(DFS, FS), slash.set(match()), RelativeLocationPath(), push(new SimplePathNode(SimplePathNode.getNodeType(slash.get()), pop(), null)));
	}
	
	/**
	 * Original Oracle BNF excerpt <br/>
	 * RelativeLocationPath ::= Step | (RelativeLocationPath "/" Step) |
	 * (RelativeLocationPath "//" Step)
	 * 
	 * <p>
	 * The implementation was changed for parsing performance gain. <br/>
	 * RelativeLocationPath ::= Step | (RelativeLocationPath ("//" | "/") Step)*
	 * </p>
	 * 
	 * @return
	 */
	Rule RelativeLocationPath() {
	    StringVar slash = new StringVar();
        return Sequence(
                Step(),
                ZeroOrMore(Sequence(FirstOf(DFS, FS), slash.set(match()), Step(), push(new SimplePathNode(
                        NodeType.RELATIVE_PATH, pop(1), new SimplePathNode(SimplePathNode.getNodeType(slash.get()), pop(),
                                null))))));
	}

	/**
	 * Original Oracle BNF excerpt <br/>
	 * Step ::= ("@" NCName) | NCName | (NCName Predicate) | Dot | "*"
	 * 
	 * <p>
	 * The implementation was changed for parsing performance gain and simplicity. <br/>
	 * Step ::= (NCName Predicate) | NCName | Dot | "*"
	 * </p>
	 * 
	 */
	// @formatter:off
	Rule Step() {
		return FirstOf(
				Sequence(NCName(), Predicate()),
				NCName(),
				Sequence(DOT, push(new SimplePathNode(NodeType.DOT, null, null))),
				Sequence(STAR, push(new SimplePathNode(NodeType.STAR, null, null)))
				);
	}
	// @formatter:on

	/**
	 * Original Oracle BNF excerpt <br/>
	 * Predicate ::= ("[" OrExp "]") | ("[" Digit+ "]")
	 * 
	 * <p>
	 * The implementation was changed for parsing performance gain and simplicity. <br>
	 * Predicate ::= ("[" RelativeLocationPath "]")
	 * </p>
	 */
	Rule Predicate() {
		return Sequence(Sequence(LBRK, RelativeLocationPath(), RBRK), push(new SimplePathNode(
                NodeType.PREDICATE, pop(1), pop())));
	}

	/**
	 * Original Oracle BNF excerpt <br/>
	 * NCName ::= (Letter | Underscore) NCNameChar*
	 * 
	 * @return
	 */
	@SuppressSubnodes
	Rule NCName() {
        return Sequence(Sequence(FirstOf(Letter(), UNDERSCORE), ZeroOrMore(NCNameChar())), push(new SimplePathNode(
                NodeType.NCNAME, new SimplePathNode(match()), null)));
	}

	/**
	 * Original Oracle BNF excerpt <br/>
	 * NCNameChar ::= Letter | Digit | Dot | Dash | Underscore
	 * 
	 */
	@SuppressNode
	Rule NCNameChar() {
		return FirstOf(Letter(), Digit(), DOT, DASH, UNDERSCORE, COLON);
	}

	/**
	 * Original Oracle BNF excerpt <br/>
	 * Letter ::= ([a-z] | [A-Z])
	 */
	@SuppressNode
	Rule Letter() {
		return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'));
	}

	/**
	 * Original Oracle BNF excerpt <br/>
	 * Digit ::= [0-9]
	 * 
	 */
	@SuppressNode
	Rule Digit() {
		return CharRange('0', '9');
	}

	@SuppressNode
	Rule Terminal(String t) {
		return Sequence(Spacing(), String(t), Spacing()).label('\'' + t + '\'');
	}

	@SuppressNode
	Rule Spacing() {
		return ZeroOrMore(AnyOf(" \t\r\n\f").label("Whitespace"));
	}

}
