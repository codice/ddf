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

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.codice.ddf.spatial.process.api.ProcessException;

/** This class is Experimental and subject to change */
public class ProcessResultBuilder {

  private final Map<String, OutputDefinition> outputDefinitions;

  private final List<Data> outputData = new ArrayList<>();

  private boolean raw;

  public ProcessResultBuilder(List<OutputDefinition> outputDefinitions, boolean raw) {
    this.outputDefinitions =
        outputDefinitions
            .stream()
            .collect(Collectors.toMap(OutputDefinition::getId, Function.identity()));
    this.raw = raw;
  }

  public ProcessResultBuilder add(String id, String value) {
    OutputDefinition outputDefinition = getOutputDefinition(id);
    outputData.add(outputDefinition.createOutputData(value));
    return this;
  }

  public ProcessResultBuilder add(String id, InputStream inputStream) {
    OutputDefinition outputDefinition = getOutputDefinition(id);
    outputData.add(outputDefinition.createOutputData(inputStream));
    return this;
  }

  public ProcessResultBuilder add(String id, URI uri) {
    OutputDefinition outputDefinition = getOutputDefinition(id);
    outputData.add(outputDefinition.createOutputData(uri));
    return this;
  }

  /**
   * @param id
   * @return OutputDefinition
   * @throws ProcessException
   */
  public OutputDefinition getOutputDefinition(String id) {
    if (!outputDefinitions.containsKey(id)) {
      throw new ProcessException(
          Collections.singleton(id), "OutputDefinition not found for output");
    }
    return outputDefinitions.get(id);
  }

  public ProcessResult build() {
    return new ProcessResult(outputData, raw);
  }
}
