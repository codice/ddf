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
package ddf.catalog.source.solr.textpath;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.DontLabel;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;

/**
 * BNF taken from Oracle's 11g TextPath Specification
 * http://docs.oracle.com/cd/E11882_01/appdev.112/e16659/xdb09sea.htm#i1006756
 * 
 * Tweaks were made to the BNF for performance and because of mistakes found in the BNF.
 * 
 * Parser is written in the Parboiled style which does not adhere to all Java naming conventions.
 * See <a href="https://github.com/sirthias/parboiled/wiki/Style-Guide">Parboiled Styling Guide</a>
 * for more details.
 * 
 * <br/>
 * <b>Some mistakes that were corrected in the Oracle BNF:</b> <li>Allowing NCName to accept a colon
 * as a valid character (It is not part of the BNF but Oracle DB allows it).</li> <li>
 * BNF has a copy-paste error for <code>EqualityExpr</code>. It is missing
 * <code>AbsoluteLocationPath</code> permutations as well.</li>
 * 
 * <br/>
 * <br/>
 * Also order might have been changed in some of the rules to force the parser to explore paths that
 * would not be explored because they are subsets of a Rule. For example, the possibility of
 * <code>//</code> must be searched before <code>/</code> because <code>/</code> would be matched
 * first and <code>//</code> would never be explored.
 * 
 * @author Ashraf Barakat
 * @author Phillip Klinefelter
 * @author ddf.isgs@lmco.com
 * 
 */
@SuppressWarnings({"InfiniteRecursion"})
@BuildParseTree
public class TextPathParser extends BaseParser<Object> {

    /**
     * Single Forward slash '/'
     */
    private final Rule FS = Terminal("/");

    /**
     * Double forward slash '//'
     */
    private final Rule DFS = Terminal("//");

    private final Rule DOT = Terminal(".");

    private final Rule DASH = Terminal("-");

    private final Rule UNDERSCORE = Terminal("_");

    private final Rule COLON = Terminal(":");

    private final Rule STAR = Terminal("*");

    private final Rule LBRK = Terminal("[");

    private final Rule RBRK = Terminal("]");

    private final Rule OR = Terminal("or");

    private final Rule AND = Terminal("and");

    private final Rule NOT = Terminal("not");

    private final Rule LPAR = Terminal("(");

    private final Rule RPAR = Terminal(")");

    private final Rule EQ = Terminal("=");

    private final Rule NEQ = Terminal("!=");

    private final Rule AT = Terminal("@");

    /**
     * Double Quote (")
     */
    private final Rule DQ = Terminal("\"");

    /**
     * Single Quote (')
     */
    private final Rule SQ = Terminal("'");

    /**
     * HasPathArg ::= LocationPath | EqualityExpr
     * 
     * @return
     */
    public Rule TextPath() {
        return Sequence(FirstOf(EqualityExpr(), LocationPath()), EOI);
    }

    /**
     * Original Oracle BNF excerpt <br/>
     * LocationPath ::= RelativeLocationPath | AbsoluteLocationPath
     */
    @DontLabel
    Rule LocationPath() {
        return FirstOf(RelativeLocationPath(), AbsoluteLocationPath());
    }

    /**
     * Original Oracle BNF excerpt <br/>
     * AbsoluteLocationPath ::= ("/" RelativeLocationPath) | ("//" RelativeLocationPath)
     * 
     * <p>
     * The implementation was changed for parsing performance gain.<br/>
     * AbsoluteLocationPath ::= ( ("//" | "/") RelativeLocationPath)
     * </p>
     */
    Rule AbsoluteLocationPath() {

        return Sequence(FirstOf(DFS, FS), RelativeLocationPath());
    }

    /**
     * Original Oracle BNF excerpt <br/>
     * RelativeLocationPath ::= Step | (RelativeLocationPath "/" Step) | (RelativeLocationPath "//"
     * Step)
     * 
     * <p>
     * The implementation was changed for parsing performance gain. <br/>
     * RelativeLocationPath ::= Step | (RelativeLocationPath ("//" | "/") Step)*
     * </p>
     * 
     * @return
     */
    Rule RelativeLocationPath() {
        return Sequence(Step(), ZeroOrMore(Sequence(FirstOf(DFS, FS), Step())));
    }

    /**
     * Original Oracle BNF excerpt <br/>
     * Step ::= ("@" NCName) | NCName | (NCName Predicate) | Dot | "*"
     * 
     * <p>
     * The implementation was changed for parsing performance gain. <br/>
     * Step ::= (NCName Predicate) | ("@" NCName) | NCName | Dot | "*"
     * </p>
     * 
     */
    // @formatter:off
	Rule Step() {
		return FirstOf(
				Sequence(NCName(), Predicate()).label("NCName_Predicate"),
				Sequence(AT, NCName()).label("Attribute"),
				NCName(),
				DOT,
				STAR
				);
	}
	// @formatter:on

    /**
     * Original Oracle BNF excerpt <br/>
     * Predicate ::= ("[" OrExp "]") | ("[" Digit+ "]")
     * 
     * <p>
     * The implementation was changed for parsing performance gain. <br>
     * Predicate ::= ("[" ( OrExp | Digit+ ) "]")
     * </p>
     */
    Rule Predicate() {
        return Sequence(LBRK, FirstOf(OrExp(), OneOrMore(Digit())), RBRK);
    }

    /**
     * Original Oracle BNF excerpt <br/>
     * OrExpr ::= AndExpr | (OrExpr "or" AndExpr)
     * 
     * <p>
     * The implementation was changed for parsing performance gain. <br>
     * OrExpr ::= AndExpr ("or" AndExpr)*
     * </p>
     * 
     */
    Rule OrExp() {

        return Sequence(AndExpr(), ZeroOrMore(Sequence(Spacing(), OR, Spacing(), AndExpr())));

    }

    /**
     * Original Oracle BNF excerpt <br/>
     * AndExpr ::= BooleanExpr | (AndExpr "and" BooleanExpr)
     * <p>
     * The implementation was changed for parsing performance gain. <br>
     * AndExpr ::= BooleanExpr ("and" BooleanExpr)*
     * </p>
     */
    Rule AndExpr() {
        return Sequence(BooleanExpr(),
                ZeroOrMore(Sequence(Spacing(), AND, Spacing(), BooleanExpr())));
    }

    /**
     * Original Oracle BNF excerpt <br/>
     * BooleanExpr ::= RelativeLocationPath | EqualityExpr | ("(" OrExpr ")") | ("not" "(" OrExpr
     * ")")
     */
    // @formatter:off
	Rule BooleanExpr() {
		return FirstOf(
				Sequence(NOT, LPAR, OrExp(), RPAR),
				EqualityExpr(),
				RelativeLocationPath(),
				Sequence(LPAR, OrExp(), RPAR)
				);
	}

	
	/**
	 * Original Oracle BNF excerpt <br/>
	 * EqualityExpr ::= (RelativeLocationPath "=" Literal) 
	 * 				| (Literal "=" RelativeLocationPath) 
	 * 				| (RelativeLocationPath "=" Literal) 
	 * 				| (Literal "!=" RelativeLocationPath) 
	 * 				| (RelativeLocationPath "=" Literal) 
	 * 				| (Literal "!=" RelativeLocationPath)
	 * 
	 * This was corrected.
	 */
	Rule EqualityExpr() {
		return FirstOf(
				Sequence(RelativeLocationPath(), EQ, Literal()),
				Sequence(Literal(), EQ, RelativeLocationPath()), 
				Sequence(RelativeLocationPath(), NEQ, Literal()),
				Sequence(Literal(), NEQ, RelativeLocationPath()), 
				Sequence(AbsoluteLocationPath(), EQ, Literal()),
				Sequence(Literal(), EQ, AbsoluteLocationPath()), 
				Sequence(AbsoluteLocationPath(), NEQ, Literal()),
				Sequence(Literal(), NEQ, AbsoluteLocationPath())
				);
	}
	// @formatter:on
    /**
     * Original Oracle BNF excerpt <br/>
     * Literal ::= (DoubleQuote [~"]* DoubleQuote) | (SingleQuote [~']* SingleQuote)
     */

    @SuppressSubnodes
    Rule Literal() {
        return FirstOf(Sequence(DQ, ZeroOrMore(NoneOf("\"")), DQ),
                Sequence(SQ, ZeroOrMore(NoneOf("'")), SQ));
    }

    /**
     * Original Oracle BNF excerpt <br/>
     * NCName ::= (Letter | Underscore) NCNameChar*
     * 
     * @return
     */
    @SuppressSubnodes
    Rule NCName() {
        return Sequence(FirstOf(Letter(), UNDERSCORE), ZeroOrMore(NCNameChar()));
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
