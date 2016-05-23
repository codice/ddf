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
/* Copyright (c) 2006-2015 by OpenLayers Contributors (see authors.txt for
 * full list of contributors). Published under the 2-clause BSD license.
 * See license.txt in the OpenLayers distribution or repository for the
 * full text of the license. */
// jshint ignore: start
define(function () {
    'use strict';

    var comparisonClass = "Comparison",
        logicalClass = "Logical",
        spatialClass = "Spatial",
        temporalClass = "Temporal",
        timePatter = /([0-9]{4})(-([0-9]{2})(-([0-9]{2})(T([0-9]{2}):([0-9]{2})(:([0-9]{2})(\.([0-9]+))?)?(Z|(([-+])([0-9]{2}):([0-9]{2})))?)?)?)?/i,
        patterns = {
            PROPERTY: /^([_a-zA-Z]\w*|"\w*")/,
            COMPARISON: /^(=|<>|<=|<|>=|>|LIKE|ILIKE)/i,
            IS_NULL: /^IS NULL/i,
            COMMA: /^,/,
            LOGICAL: /^(AND|OR)/i,
            VALUE: /^('([^']|'')*'|-?\d+(\.\d*)?|\.\d+)/,
            LPAREN: /^\(/,
            RPAREN: /^\)/,
            SPATIAL: /^(BBOX|INTERSECTS|DWITHIN|WITHIN|CONTAINS)/i,
            NOT: /^NOT/i,
            BETWEEN: /^BETWEEN/i,
            BEFORE: /^BEFORE/i,
            AFTER: /^AFTER/i,
            DURING: /^DURING/i,
            TIME: new RegExp('^' + timePatter.source),
            TIME_PERIOD: new RegExp('^' + timePatter.source + '/' + timePatter.source),
            GEOMETRY: function (text) {
                var type = /^(POINT|LINESTRING|POLYGON|MULTIPOINT|MULTILINESTRING|MULTIPOLYGON|GEOMETRYCOLLECTION)/.exec(text);
                if (type) {
                    var len = text.length;
                    var idx = text.indexOf("(", type[0].length);
                    if (idx > -1) {
                        var depth = 1;
                        while (idx < len && depth > 0) {
                            idx++;
                            switch (text.charAt(idx)) {
                                case '(':
                                    depth++;
                                    break;
                                case ')':
                                    depth--;
                                    break;
                                default:
                                // in default case, do nothing
                            }
                        }
                    }
                    return [text.substr(0, idx + 1)];
                }
            },
            END: /^$/
        },

        follows = {
            LPAREN: ['GEOMETRY', 'SPATIAL', 'PROPERTY', 'VALUE', 'LPAREN'],
            RPAREN: ['NOT', 'LOGICAL', 'END', 'RPAREN'],
            PROPERTY: ['COMPARISON', 'BETWEEN', 'COMMA', 'IS_NULL', 'BEFORE', 'AFTER', 'DURING'],
            BETWEEN: ['VALUE'],
            IS_NULL: ['END'],
            COMPARISON: ['VALUE'],
            COMMA: ['GEOMETRY', 'VALUE', 'PROPERTY'],
            VALUE: ['LOGICAL', 'COMMA', 'RPAREN', 'END'],
            SPATIAL: ['LPAREN'],
            LOGICAL: ['NOT', 'VALUE', 'SPATIAL', 'PROPERTY', 'LPAREN'],
            NOT: ['PROPERTY', 'LPAREN'],
            GEOMETRY: ['COMMA', 'RPAREN'],
            BEFORE: ['TIME'],
            AFTER: ['TIME'],
            DURING: ['TIME_PERIOD'],
            TIME: ['LOGICAL', 'RPAREN', 'END'],
            TIME_PERIOD: ['LOGICAL', 'RPAREN', 'END']
        },

        precedence = {
            'RPAREN': 3,
            'LOGICAL': 2,
            'COMPARISON': 1
        },

        classes = {
            '=': comparisonClass,
            '<>': comparisonClass,
            '<': comparisonClass,
            '<=': comparisonClass,
            '>': comparisonClass,
            '>=': comparisonClass,
            'LIKE': comparisonClass,
            'ILIKE': comparisonClass,
            'BETWEEN': comparisonClass,
            'IS NULL': comparisonClass,
            'AND': logicalClass,
            'OR': logicalClass,
            'NOT': logicalClass,
            'BBOX': spatialClass,
            'INTERSECTS': spatialClass,
            'DWITHIN': spatialClass,
            'WITHIN': spatialClass,
            'CONTAINS': spatialClass,
            'GEOMETRY': spatialClass,
            'BEFORE': temporalClass,
            'AFTER': temporalClass,
            'DURING': temporalClass
        },
        dateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    function tryToken(text, pattern) {
        if (pattern instanceof RegExp) {
            return pattern.exec(text);
        } else {
            return pattern(text);
        }
    }

    function nextToken(text, tokens) {
        var i, token, len = tokens.length;
        for (i = 0; i < len; i++) {
            token = tokens[i];
            var pat = patterns[token];
            var matches = tryToken(text, pat);
            if (matches) {
                var match = matches[0];
                var remainder = text.substr(match.length).replace(/^\s*/, "");
                return {
                    type: token,
                    text: match,
                    remainder: remainder
                };
            }
        }

        var msg = "ERROR: In parsing: [" + text + "], expected one of: ";
        for (i = 0; i < len; i++) {
            token = tokens[i];
            msg += "\n    " + token + ": " + patterns[token];
        }

        throw new Error(msg);
    }

    function tokenize(text) {
        var results = [];
        var token, expect = ["NOT", "GEOMETRY", "SPATIAL", "PROPERTY", "LPAREN"];

        do {
            token = nextToken(text, expect);
            text = token.remainder;
            expect = follows[token.type];
            if (token.type !== "END" && !expect) {
                throw new Error("No follows list for " + token.type);
            }
            results.push(token);
        } while (token.type !== "END");

        return results;
    }

    function buildAst(tokens) {
        var operatorStack = [],
            postfix = [];

        while (tokens.length) {
            var tok = tokens.shift();
            switch (tok.type) {
                case "PROPERTY":
                case "GEOMETRY":
                case "VALUE":
                case "TIME":
                case "TIME_PERIOD":
                    postfix.push(tok);
                    break;
                case "COMPARISON":
                case "BETWEEN":
                case "IS_NULL":
                case "LOGICAL":
                case "BEFORE":
                case "AFTER":
                case "DURING":
                    var p = precedence[tok.type];

                    while (operatorStack.length > 0 &&
                    (precedence[operatorStack[operatorStack.length - 1].type] <= p)
                        ) {
                        postfix.push(operatorStack.pop());
                    }

                    operatorStack.push(tok);
                    break;
                case "SPATIAL":
                case "NOT":
                case "LPAREN":
                    operatorStack.push(tok);
                    break;
                case "RPAREN":
                    while (operatorStack.length > 0 &&
                    (operatorStack[operatorStack.length - 1].type !== "LPAREN")
                        ) {
                        postfix.push(operatorStack.pop());
                    }
                    operatorStack.pop(); // toss out the LPAREN

                    if (operatorStack.length > 0 &&
                        operatorStack[operatorStack.length - 1].type === "SPATIAL") {
                        postfix.push(operatorStack.pop());
                    }
                    break;
                case "COMMA":
                case "END":
                    break;
                default:
                    throw new Error("Unknown token type " + tok.type);
            }
        }

        while (operatorStack.length > 0) {
            postfix.push(operatorStack.pop());
        }

        function buildTree() {
            var value,
                property,
                tok = postfix.pop();
            switch (tok.type) {
                case "LOGICAL":
                    var rhs = buildTree(),
                        lhs = buildTree();
                    return {
                        filters: [lhs, rhs],
                        type: tok.text.toUpperCase()
                    };
                case "NOT":
                    var operand = buildTree();
                    return {
                        filters: [operand],
                        type: tok.type
                    };
                case "BETWEEN":
                    var min, max;
                    postfix.pop(); // unneeded AND token here
                    max = buildTree();
                    min = buildTree();
                    property = buildTree();
                    return {
                        property: property,
                        lowerBoundary: min,
                        upperBoundary: max,
                        type: tok.type
                    };
                case "BEFORE":
                case "AFTER":
                    value = buildTree();
                    property = buildTree();
                    return {
                        property: property,
                        value: new Date(value),
                        type: tok.text.toUpperCase()
                    };
                case "DURING":
                    var dates = buildTree().split('/');
                    property = buildTree();
                    return {
                        property: property,
                        from: dates[0],
                        to: dates[1],
                        type: tok.text.toUpperCase()
                    };
                case "COMPARISON":
                    value = buildTree();
                    property = buildTree();
                    return {
                        property: property,
                        value: value,
                        type: tok.text.toUpperCase()
                    };
                case "IS_NULL":
                    property = buildTree();
                    return {
                        property: property,
                        type: tok.text.toUpperCase()
                    };
                case "VALUE":
                    var match = tok.text.match(/^'(.*)'$/);
                    if (match) {
                        return match[1].replace(/''/g, "'");
                    } else {
                        return Number(tok.text);
                    }
                    break;
                case "SPATIAL":
                    switch (tok.text.toUpperCase()) {
                        case "BBOX":
                            var maxy = buildTree(),
                                maxx = buildTree(),
                                miny = buildTree(),
                                minx = buildTree(),
                                prop = buildTree();

                            return {
                                type: tok.text.toUpperCase(),
                                property: prop,
                                value: [minx, miny, maxx, maxy]
                            };
                        case "INTERSECTS":
                            value = buildTree();
                            property = buildTree();
                            return {
                                type: tok.text.toUpperCase(),
                                property: property,
                                value: value
                            };
                        case "WITHIN":
                            value = buildTree();
                            property = buildTree();
                            return {
                                type: tok.text.toUpperCase(),
                                property: property,
                                value: value
                            };
                        case "CONTAINS":
                            value = buildTree();
                            property = buildTree();
                            return {
                                type: tok.text.toUpperCase(),
                                property: property,
                                value: value
                            };
                        case "DWITHIN":
                            var distance = buildTree();
                            value = buildTree();
                            property = buildTree();
                            return {
                                type: tok.text.toUpperCase(),
                                value: value,
                                property: property,
                                distance: Number(distance)
                            };
                    }
                    break;
                case "GEOMETRY":
                    return {
                        type: tok.type,
                        value: tok.text
                    };
                default:
                    return tok.text;
            }
        }

        var result = buildTree();
        if (postfix.length > 0) {
            var msg = "Remaining tokens after building AST: \n";
            for (var i = postfix.length - 1; i >= 0; i--) {
                msg += postfix[i].type + ": " + postfix[i].text + "\n";
            }
            throw new Error(msg);
        }

        return result;
    }

    function write(filter) {
        switch (classes[filter.type]) {
            case spatialClass:
                switch (filter.type) {
                    case "BBOX":
                        var xmin = filter.value[0],
                            ymin = filter.value[1],
                            xmax = filter.value[2],
                            ymax = filter.value[3];
                        return "BBOX(" +
                            filter.property + "," +
                            xmin + "," + ymin + "," + xmax + "," + ymax +
                            ")";
                    case "DWITHIN":
                        return "DWITHIN(" +
                            filter.property + ", " +
                            write(filter.value) + ", " +
                            filter.distance + ", meters)";
                    case "WITHIN":
                        return "WITHIN(" +
                            filter.property + ", " +
                            write(filter.value) + ")";
                    case "INTERSECTS":
                        return "INTERSECTS(" +
                            filter.property + ", " +
                            write(filter.value) + ")";
                    case "CONTAINS":
                        return "CONTAINS(" +
                            filter.property + ", " +
                            write(filter.value) + ")";
                    case "GEOMETRY":
                        return filter.value;
                    default:
                        throw new Error("Unknown spatial filter type: " + filter.type);
                }
                break;
            case logicalClass:
                if (filter.type === "NOT") {
                    // TODO: deal with precedence of logical operators to
                    // avoid extra parentheses (not urgent)
                    return "NOT (" + write(filter.filters[0]) + ")";
                } else {
                    var res = "(";
                    var first = true;
                    for (var i = 0; i < filter.filters.length; i++) {
                        if (first) {
                            first = false;
                        } else {
                            res += ") " + filter.type + " (";
                        }
                        res += write(filter.filters[i]);
                    }
                    return res + ")";
                }
                break;
            case comparisonClass:
                if (filter.type === "BETWEEN") {
                    return filter.property + " BETWEEN " +
                        write(filter.lowerBoundary) + " AND " +
                        write(filter.upperBoundary);
                } else {
                    return (filter.value !== null) ? filter.property +
                    " " + filter.type + " " +
                    write(filter.value) : filter.property +
                    " " + filter.type;
                }
                break;
            case temporalClass:
                switch (filter.type) {
                    case "BEFORE":
                    case "AFTER":
                        return filter.property + " " + filter.type + " " + filter.value.toString(dateTimeFormat);
                    case "DURING":
                        return filter.property + " " + filter.type + " " +
                            filter.from.toString(dateTimeFormat) + "/" +
                            filter.to.toString(dateTimeFormat);
                }
                break;
            case undefined:
                if (typeof filter === "string") {
                    return "'" + filter.replace(/'/g, "''") + "'";
                } else if (typeof filter === "number") {
                    return String(filter);
                }
                break;
            default:
                throw new Error("Can't encode: " + filter.type + " " + filter);
        }
    }

    function simplifyFilters(cqlAst){
        for (var i = 0; i < cqlAst.filters.length; i++){
            if (simplifyAst(cqlAst.filters[i], cqlAst)){
                var filtersToMerge = cqlAst.filters.splice(i, 1)[0];
                filtersToMerge.filters.forEach(function(filter){
                    cqlAst.filters.push(filter);
                });
            }
        }
    }

    function simplifyAst(cqlAst, parentNode){
        if (!cqlAst.filters && parentNode){
            return false;
        } else if (!parentNode){
            if (cqlAst.filters){
                simplifyFilters(cqlAst);
            }
            return cqlAst;
        } else {
            simplifyFilters(cqlAst);
            if (cqlAst.type === parentNode.type){
                return true;
            } else {
                return false;
            }
        }
    }

    return {
        read: function (cql) {
            return buildAst(tokenize(cql));
        },
        write: function (filter) {
            return write(filter);
        },
        simplify: function(cqlAst){
            return simplifyAst(cqlAst);
        }
    };
});