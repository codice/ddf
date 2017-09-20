/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.confluence.source;

public class ConfluenceQueryParameter {

  public static final String PROPERTY_FORMAT = "%s %s \"%s\"";

  public static final String WILD_CARD = "*";

  private boolean likeValid = false;

  private boolean equalValid = false;

  private boolean greaterLessThanValid = false;

  private boolean wildCardValid = false;

  private boolean translateLike = false;

  private String paramterName;

  public ConfluenceQueryParameter(
      String paramterName,
      boolean likeValid,
      boolean equalValid,
      boolean greaterLessThanValid,
      boolean wildCardValid,
      boolean translateLike) {
    this.paramterName = paramterName;
    this.likeValid = likeValid;
    this.equalValid = equalValid;
    this.greaterLessThanValid = greaterLessThanValid;
    this.wildCardValid = wildCardValid;
    this.translateLike = translateLike;
  }

  public String getLikeExpression(String literal) {
    String operator = "~";
    if (!likeValid) {
      if (translateLike) {
        operator = "=";
      } else {
        return null;
      }
    }
    if (invalidWildCard(literal)) {
      return null;
    }
    return String.format(PROPERTY_FORMAT, paramterName, operator, literal);
  }

  public String getEqualExpression(String literal) {
    if (!equalValid || invalidWildCard(literal)) {
      return null;
    }
    return String.format(PROPERTY_FORMAT, paramterName, "=", literal);
  }

  public String getGreaterThanExpression(String literal) {
    if (!greaterLessThanValid || invalidWildCard(literal)) {
      return null;
    }
    return String.format(PROPERTY_FORMAT, paramterName, ">", literal);
  }

  public String getLessThanExpression(String literal) {
    if (!greaterLessThanValid || invalidWildCard(literal)) {
      return null;
    }
    return String.format(PROPERTY_FORMAT, paramterName, "<", literal);
  }

  public String getParamterName() {
    return paramterName;
  }

  private boolean invalidWildCard(String literal) {
    return (!wildCardValid && literal.indexOf(WILD_CARD) >= 0) || WILD_CARD.equals(literal);
  }
}
