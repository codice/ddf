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
package org.codice.ddf.endpoints;

import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.support.StringVar;

/**
 * This parser is based on a modified version of the
 * "IC/DoD Keyword Query Language Specification, V2.0" DRAFT (4 September 2012). This spec includes
 * an EBNF that this parser is based on. All changes to that EBNF were made to add variable
 * whitespace handling and to make parsing more efficient.
 */

@SuppressWarnings({ "InfiniteRecursion" })
@BuildParseTree
public class KeywordTextParser extends BaseParser<ASTNode> {
    protected static final String OR_STRING = "OR";

    protected static final String AND_STRING = "AND";

    protected static final String NOT_STRING = "NOT";

    protected static final String SPACE_STRING = " ";

    protected final Rule OR_OPERATOR = Terminal(OR_STRING);

    protected final Rule AND_OPERATOR = Terminal(AND_STRING);

    protected final Rule NOT_OPERATOR = Terminal(NOT_STRING);

    protected final Rule LPAR = Terminal("(");

    protected final Rule RPAR = Terminal(")");

    protected final Rule DBLQUOTE = Terminal("\"");

    protected final Rule SPACE_RULE = Terminal(SPACE_STRING);

    // This method exists to detect end of input.
    public Rule InputPhrase() {
        return Sequence(KeywordQueryExpression(), EOI);
    }

    /**
     * Original Keyword Query Specification EBNF excerpt <br/>
     * <keyword-query-expression> ::= <term> (<boolean-operator> <term>)*;
     * <p>
     * The implementation was changed to allow whitespace. <br/>
     * keyword query expression = optional whitespace, term, {boolean operator, term}, optional
     * whitespace;
     * </p>
     */
    Rule KeywordQueryExpression() {
        StringVar operator = new StringVar();
        return Sequence(
                OptionalWhiteSpace(),
                Term(),
                ZeroOrMore(BooleanOperator(), operator.set(match()), Term(),
                        push(new OperatorASTNode(operator.get(), pop(1), pop()))),
                OptionalWhiteSpace());
    }

    /**
     * Original Keyword Query Specification EBNF excerpt <br/>
     * <boolean-operator> ::= <and> | <or> | <not>;
     * <p>
     * The implementation was changed to evaluate OR and NOT first, so all spaces aren't evaluated
     * as ANDs. <br/>
     * boolean operator = or | not | and;
     * </p>
     */
    Rule BooleanOperator() {
        return FirstOf(Or(), Not(), And());
    }

    /**
     * Original Keyword Query Specification EBNF excerpt <br/>
     * <and> ::= “ AND ” | “ ”;
     * <p>
     * The implementation was changed to allow whitespace and to not require boolean operators to be
     * wrapped in spaces. <br/>
     * and = (optional whitespace, "AND", optional whitespace) | mandatory whitespace;
     * </p>
     */
    Rule And() {
        return FirstOf(Sequence(OptionalWhiteSpace(), AND_OPERATOR, OptionalWhiteSpace()),
                MandatoryWhiteSpace());
    }

    /**
     * Original Keyword Query Specification EBNF excerpt <br/>
     * <or> ::= “ OR ”;
     * <p>
     * The implementation was changed to allow whitespace and to not require boolean operators to be
     * wrapped in spaces. <br/>
     * or = (optional whitespace, "OR", optional whitespace);
     * </p>
     */
    Rule Or() {
        return Sequence(OptionalWhiteSpace(), OR_OPERATOR, OptionalWhiteSpace());
    }

    /**
     * Original Keyword Query Specification EBNF excerpt <br/>
     * <not> ::= “ NOT ”;
     * <p>
     * The implementation was changed to allow whitespace and to not require boolean operators to be
     * wrapped in spaces. <br/>
     * not = (optional whitespace, "NOT", optional whitespace);
     * </p>
     */
    Rule Not() {
        return Sequence(OptionalWhiteSpace(), NOT_OPERATOR, OptionalWhiteSpace());
    }

    /**
     * Original Keyword Query Specification EBNF excerpt <br/>
     * <term> ::= <keyword> | <phrase> | <group>;
     * <p>
     * The implementation was changed to evaluate the most specific rule first. <br/>
     * term = group | phrase | keyword;
     * </p>
     */
    Rule Term() {
        return FirstOf(Group(), Phrase(), Keyword());
    }

    /**
     * Original Keyword Query Specification EBNF excerpt <br/>
     * <phrase> ::= '"' <keyword> (' '<keyword>)* '"';
     * <p>
     * The implementation was changed to allow whitespace. <br/>
     * phrase = optional whitespace, '"', optional whitespace, keyword, { optional whitespace,
     * keyword}, optional whitespace, '"';
     * </p>
     */

    Rule Phrase() {
        Action stackPhraseRewriteAction = new StackPhraseRewriteAction();
        // only grab leading spaces
        return Sequence(OptionalWhiteSpace(), DBLQUOTE, OptionalWhiteSpace(),
                push(new PhraseDelimiterASTNode()), Keyword(),
                ZeroOrMore(Sequence(OptionalWhiteSpace(), Keyword())), stackPhraseRewriteAction,
                OptionalWhiteSpace(), DBLQUOTE);
    }

    /**
     * Original Keyword Query Specification EBNF excerpt <br/>
     * <group> ::= '('<keyword-query-expression>')';
     * <p>
     * The implementation was changed to allow whitespace. <br/>
     * group = optional whitespace, '(', optional whitespace, keyword query expression, optional
     * whitespace, ')';
     * </p>
     */
    Rule Group() {
        // only grab leading spaces
        return Sequence(OptionalWhiteSpace(), LPAR, OptionalWhiteSpace(), KeywordQueryExpression(),
                OptionalWhiteSpace(), RPAR);
    }

    /**
     * Original Keyword Query Specification excerpt <br/>
     * "A keyword is a single string (containing no whitespaces) such as "test" or "hello"."
     * <p>
     * All characters except: EOI, whitespace, (, ), ". <br/>
     * </p>
     */
    Rule Keyword() {
        // TODO the default value is used to allow the parser to keep running during error
        // recovery... is this right?
        return Sequence(OneOrMore(NoneOf(" \t\n\f()\"")), push(new KeywordASTNode(
                matchOrDefault("defaultKeyword"))));
    }

    // Previously, all strings were wrapped in Spacing() by this function
    @SuppressNode
    Rule Terminal(String t) {
        return String(t).label('\'' + t + '\'');
    }

    /**
     * This was added to allow whitespace. optional whitespace = {' '};
     */
    @SuppressNode
    Rule OptionalWhiteSpace() {
        return ZeroOrMore(AnyOf(" \t\r\n\f").label("Optional Whitespace"));
    }

    /**
     * This was added to allow whitespace. mandatory whitespace = ' ', optional whitespace;
     */
    Rule MandatoryWhiteSpace() {
        return OneOrMore(AnyOf(" \t\r\n\f").label("Mandatory Whitespace"));
    }

    public class StackPhraseRewriteAction implements Action {
        // pop all keywords off of the stack and combine them with quotes and push them back
        @Override
        public boolean run(Context context) {
            StringBuilder keywords = new StringBuilder("");

            // loop through the stack until it's empty or we hit a non-keyword
            while (!isStackEmpty() && !peek().isPhraseStartDelimiter()) {
                // restore the original order since popping them off the top reverses the order
                keywords.insert(0, pop().getKeyword());
                keywords.insert(0, SPACE_STRING);
            }

            // make sure we clear the phrase start marker from the stack
            if (peek().isPhraseStartDelimiter()) {
                drop();
            }

            // push the keywords minus the leading space back onto the stack as a single keyword
            push(new KeywordASTNode(keywords.toString().substring(1)));

            return true;
        }

        // there doesn't appear to be a better way to do this in parboiled without accessing the
        // stack directly
        private boolean isStackEmpty() {
            try {
                peek();
            } catch (IllegalArgumentException iae) {
                return true;
            }

            return false;
        }
    }
}
