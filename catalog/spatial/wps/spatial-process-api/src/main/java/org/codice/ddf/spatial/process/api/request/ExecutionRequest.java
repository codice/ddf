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
package org.codice.ddf.spatial.process.api.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** This class is Experimental and subject to change */
public class ExecutionRequest {

  private final boolean rawResponse;

  private final List<Data> inputData;

  private final List<OutputDefinition> outputDefinitions;

  public ExecutionRequest(
      List<Data> inputData, List<OutputDefinition> outputDefinitions, boolean rawResponse) {
    this.inputData = new ArrayList<>(inputData);
    this.outputDefinitions = new ArrayList<>(outputDefinitions);
    this.rawResponse = rawResponse;
  }

  public List<Data> getInputData() {
    return Collections.unmodifiableList(inputData);
  }

  public List<OutputDefinition> getOutputDefinitions() {
    return Collections.unmodifiableList(outputDefinitions);
  }

  public boolean isRawResponse() {
    return rawResponse;
  }

  public ProcessResultBuilder getRequestResultBuilder() {
    return new ProcessResultBuilder(outputDefinitions, rawResponse);
  }
}
