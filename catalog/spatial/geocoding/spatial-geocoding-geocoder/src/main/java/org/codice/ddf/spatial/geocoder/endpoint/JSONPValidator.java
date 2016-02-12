/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package org.codice.ddf.spatial.geocoder.endpoint;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSONPValidator {

    private static final Pattern JSONP_VALID_PATTERN;

    static {
        JSONP_VALID_PATTERN = Pattern.compile(
                "^[a-zA-Z_$][0-9a-zA-Z_$]*(?:\\[(?:\".+\"|'.+'|\\d+)\\])*?$");
    }

    private static final Set<String> RESERVED_WORDS =
            Collections.unmodifiableSet(new HashSet<String>() {
                {
                    add("abstract");
                    add("boolean");
                    add("break");
                    add("byte");
                    add("case");
                    add("catch");
                    add("char");
                    add("class");
                    add("const");
                    add("continue");
                    add("debugger");
                    add("default");
                    add("delete");
                    add("do");
                    add("double");
                    add("else");
                    add("enum");
                    add("export");
                    add("extends");
                    add("false");
                    add("final");
                    add("finally");
                    add("float");
                    add("for");
                    add("function");
                    add("goto");
                    add("if");
                    add("implements");
                    add("import");
                    add("in");
                    add("instanceof");
                    add("int");
                    add("interface");
                    add("long");
                    add("native");
                    add("new");
                    add("null");
                    add("package");
                    add("private");
                    add("protected");
                    add("public");
                    add("return");
                    add("short");
                    add("static");
                    add("super");
                    add("switch");
                    add("synchronized");
                    add("this");
                    add("throw");
                    add("throws");
                    add("transient");
                    add("true");
                    add("try");
                    add("typeof");
                    add("var");
                    add("void");
                    add("volatile");
                    add("while");
                    add("with");
                }
            });

    public static boolean isValidJSONP(String jsonp) {
        String[] jsonpPortions = jsonp.split("\\.");
        if (jsonpPortions.length == 0) {
            return false;
        }
        for (String portion : jsonpPortions) {
            Matcher matcher = JSONP_VALID_PATTERN.matcher(portion);
            if (!matcher.matches() || RESERVED_WORDS.contains(portion)) {
                return false;
            }
        }
        return true;
    }
}
