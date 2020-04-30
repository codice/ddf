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
package ddf.catalog.operation.impl;

import ddf.catalog.operation.ResultAttributeHighlight;
import ddf.catalog.operation.ResultHighlight;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultHighlightImpl implements ResultHighlight {
  private String resultId;

  private List<ResultAttributeHighlight> attributeHighlights;

  /**
   * Instantiates a ResultHighlightImpl representing the highlight information for a particular
   * query result. The attribute highlight data is available to add to or retrieve via the <code>
   * getAttributeHighlights</code> method. This constructor creates an empty attribute highlight
   * list, indicating that there are no highlights.
   *
   * @param resultId The ID of the result for which the contained highlights are for
   */
  public ResultHighlightImpl(String resultId) {
    this.resultId = resultId;
    attributeHighlights = new ArrayList<>();
  }

  /**
   * Instantiates a ResultHighlightImpl representing the highlight information for a particular
   * query result. Any number of {@link ResultAttributeHighlight}s may be provided to indicate that
   * more than one attribute was matched in a query. Additional highlight data may be added to the
   * list by first requesting the list of highlights from the <code>getAttributeHighlights</code>
   * method.
   *
   * @param resultId The ID of the result for which the contained highlights are for
   * @param attributeHighlights The list of attribute highlights that apply to the result
   */
  public ResultHighlightImpl(String resultId, List<ResultAttributeHighlight> attributeHighlights) {
    this(resultId);
    this.attributeHighlights.addAll(attributeHighlights);
  }

  @Override
  public String getResultId() {
    return resultId;
  }

  @Override
  public List<ResultAttributeHighlight> getAttributeHighlights() {
    return Collections.unmodifiableList(attributeHighlights);
  }
}
