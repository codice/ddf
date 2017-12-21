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
package org.codice.ddf.spatial.ogc.wps.process.endpoint;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.opengis.ows.v_2_0.BasicIdentificationType;
import net.opengis.ows.v_2_0.CodeType;
import net.opengis.wps.v_2_0.ProcessOffering;
import net.opengis.wps.v_2_0.ProcessOfferings;
import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.spatial.ogc.wps.process.api.WpsException;
import org.codice.ddf.spatial.process.api.description.BoundingBoxDataDescription;
import org.codice.ddf.spatial.process.api.description.ComplexDataDescription;
import org.codice.ddf.spatial.process.api.description.DataDescription;
import org.codice.ddf.spatial.process.api.description.DataDescriptionGroup;
import org.codice.ddf.spatial.process.api.description.ExecutionDescription;
import org.codice.ddf.spatial.process.api.description.LiteralDataDescription;
import org.codice.ddf.spatial.process.api.description.TransmissionMode;
import org.codice.ddf.spatial.process.api.request.Data;
import org.codice.ddf.spatial.process.api.request.DataFormat;
import org.codice.ddf.spatial.process.api.request.DataGroup;
import org.codice.ddf.spatial.process.api.request.ExecutionRequest;
import org.codice.ddf.spatial.process.api.request.OutputDefinition;

public class Validator {

  public static final String WPS_VERSION = "2.0.0";

  private Validator() {}

  /**
   * @param versions
   * @throws WpsException
   */
  public static void validateAcceptedVersions(List<String> versions) {

    if (!CollectionUtils.isEmpty(versions) && !versions.contains(WPS_VERSION)) {
      throw new WpsException(
          "List of versions in AcceptVersions parameter value, in GetCapabilities operation request, did not include any version supported by this server",
          "VersionNegotiationFailed",
          null);
    }
  }

  /**
   * @param processIds
   * @param processOfferings
   * @throws WpsException
   */
  public static void validateDescribeRequest(
      Set<String> processIds, ProcessOfferings processOfferings) {
    Set<String> foundProcessIds =
        processOfferings
            .getProcessOffering()
            .stream()
            .map(ProcessOffering::getProcess)
            .map(BasicIdentificationType::getIdentifier)
            .map(CodeType::getValue)
            .collect(Collectors.toSet());

    // add ALL to list of found processe Id's since it is reserved by WPS
    foundProcessIds.add("ALL");

    // verify that processId's for all of the requested processes are found otherwise throw an
    // exception for any process which the system didn't find an offering for.
    Set<String> missingProcessIds = new HashSet<>(processIds);
    missingProcessIds.removeAll(foundProcessIds);

    if (!missingProcessIds.isEmpty()) {
      throw new WpsException(
          "One of the identifiers passed does not match with any of the processes offered by this server.",
          "NoSuchProcess",
          String.join(",", missingProcessIds));
    }
  }

  /**
   * @param inputDatas
   * @param inputDescriptions
   * @throws WpsException
   */
  public static void validateProcessInputs(
      List<Data> inputDatas, List<DataDescription> inputDescriptions) {
    // bin the inputs by id
    Map<String, List<Data>> inputs =
        inputDatas.stream().collect(Collectors.groupingBy(Data::getId));

    // verify no unexpected inputs are being passed
    Map<String, DataDescription> inputDesc =
        inputDescriptions
            .stream()
            .collect(Collectors.toMap(DataDescription::getId, Function.identity()));
    inputs
        .keySet()
        .forEach(
            key -> {
              if (!inputDesc.containsKey(key)) {
                throw new WpsException(
                    "One or more of the input identifiers passed does not match with any of the input identifiers of this process.",
                    "NoSuchInput",
                    key);
              }
            });

    inputDesc.forEach((key, value) -> validateProcessInputsMinMaxOccurs(inputs.get(key), value));
  }

  /**
   * @param inputs
   * @param inputDescription
   * @throws WpsException
   */
  public static void validateProcessInputsMinMaxOccurs(
      List<Data> inputs, DataDescription inputDescription) {

    if (CollectionUtils.isEmpty(inputs)) {
      if (BigInteger.ZERO.equals(inputDescription.getMinOccurs())) {
        return;
      } else {
        throw new WpsException(
            "Too few input items have been specified.", "TooFewInputs", inputDescription.getId());
      }
    }

    BigInteger maxOccurs =
        inputDescription.getMaxOccurs() == null ? BigInteger.ONE : inputDescription.getMaxOccurs();

    if (inputs.size() > maxOccurs.intValue()) {
      throw new WpsException(
          "Too many input items have been specified.", "TooManyInputs", inputDescription.getId());
    }
    BigInteger minOccurs =
        inputDescription.getMinOccurs() == null ? BigInteger.ONE : inputDescription.getMinOccurs();
    if (inputs.size() < minOccurs.intValue()) {
      throw new WpsException(
          "Too few input items have been specified.", "TooFewInputs", inputDescription.getId());
    }
    inputs.forEach(input -> validateProcessInputData(input, inputDescription));
  }

  public static void validateProcessInputData(Data inputs, DataDescription inputDescription) {
    if (!CollectionUtils.isEmpty(inputDescription.getDataFormats())) {
      validateDataFormats(inputDescription, inputs.getFormat());
    }

    if (inputDescription instanceof DataDescriptionGroup) {
      if (inputs instanceof DataGroup) {
        validateProcessInputs(
            ((DataGroup) inputs).getData(),
            ((DataDescriptionGroup) inputDescription).getDataDescriptions());
      } else {
        throw new WpsException(
            "One or more of inputs for which the service was able to retrieve the data but could not read it.",
            "WrongInputData",
            inputDescription.getId());
      }
      // todo (RWY) - implement extended validation for LiteralDataDescription
      // ComplexDataDescription and BoundingBoxDataDescription when we understand the use cases
      // better
    } else if (inputDescription instanceof LiteralDataDescription) {
      // add additional validation here
    } else if (inputDescription instanceof ComplexDataDescription) {
      // add additional validation here
    } else if (inputDescription instanceof BoundingBoxDataDescription) {
      // add additional validation here
    }
  }

  /**
   * @param dataDescription
   * @param dataFormat
   * @throws WpsException
   */
  public static void validateDataFormats(
      DataDescription dataDescription, @Nullable DataFormat dataFormat) {
    if (!CollectionUtils.isEmpty(
            dataDescription
                .getDataFormats()) // if there is no format specified don't worry about validating
        && dataFormat != null
        // if the given data format is null, don't validate we should just use the default.
        && !dataDescription.getDataFormats().contains(dataFormat)) {
      throw new WpsException(
          "One or more of the input or output formats specified in the request did not match with any of the formats defined for that particular input or output",
          "NoSuchFormat",
          dataDescription.getId());
    }
  }

  /**
   * @param requestedOutputs
   * @param outputDescriptions
   * @throws WpsException
   */
  public static void validateProcessOutputs(
      List<OutputDefinition> requestedOutputs, List<DataDescription> outputDescriptions) {
    Map<String, DataDescription> outDesc =
        outputDescriptions
            .stream()
            .collect(Collectors.toMap(DataDescription::getId, Function.identity()));
    for (OutputDefinition requestedOutput : requestedOutputs) {
      DataDescription dataDescription = outDesc.get(requestedOutput.getId());
      if (dataDescription == null) {
        throw new WpsException(
            "One or more of the output identifiers passed does not match with any of the output identifiers of this process.",
            "NoSuchOutput",
            requestedOutput.getId());
      }
      validateProcessOutput(requestedOutput, dataDescription);
    }
  }

  public static void validateProcessOutput(
      OutputDefinition requestedOutput, DataDescription dataDescription) throws WpsException {
    validateDataFormats(dataDescription, requestedOutput.getFormat());
  }

  public static void validateTransmissionModes(
      @Nullable List<OutputDefinition> requestedOutputs,
      @Nullable Set<TransmissionMode> transmissionModes)
      throws WpsException {
    // if the process doesn't specify a transmission mode lets ignore it
    if (CollectionUtils.isEmpty(transmissionModes) || CollectionUtils.isEmpty(requestedOutputs)) {
      return;
    }

    for (OutputDefinition requestedOutput : requestedOutputs) {
      if (!transmissionModes.contains(requestedOutput.getTransmissionMode())) {
        // this is fabricated the spec doesn't have a specific exception for not supporting `value`
        // transmission mode.
        throw new WpsException(
            "Execute operation requests unsupported transmission mode for output data",
            "NoSuchMode",
            requestedOutput.getId());
      }
    }
  }

  /**
   * @param executionDescription
   * @param executionRequest
   * @throws WpsException
   */
  public static void validate(
      ExecutionDescription executionDescription, ExecutionRequest executionRequest) {

    validateProcessInputs(
        executionRequest.getInputData(), executionDescription.getInputDescriptions());
    validateProcessOutputs(
        executionRequest.getOutputDefinitions(), executionDescription.getOutputDescriptions());

    validateTransmissionModes(
        executionRequest.getOutputDefinitions(), executionDescription.getTransmissionModes());
  }
}
