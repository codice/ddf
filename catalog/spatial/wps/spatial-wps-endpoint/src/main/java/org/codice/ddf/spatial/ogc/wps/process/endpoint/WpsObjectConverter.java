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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import net.opengis.ows.v_2_0.AllowedValues;
import net.opengis.ows.v_2_0.CodeType;
import net.opengis.ows.v_2_0.DomainMetadataType;
import net.opengis.ows.v_2_0.KeywordsType;
import net.opengis.ows.v_2_0.LanguageStringType;
import net.opengis.ows.v_2_0.MetadataType;
import net.opengis.ows.v_2_0.ObjectFactory;
import net.opengis.ows.v_2_0.RangeType;
import net.opengis.ows.v_2_0.ValuesReference;
import net.opengis.wps.v_2_0.BoundingBoxData;
import net.opengis.wps.v_2_0.ComplexDataType;
import net.opengis.wps.v_2_0.Data;
import net.opengis.wps.v_2_0.DataDescriptionType;
import net.opengis.wps.v_2_0.DataInputType;
import net.opengis.wps.v_2_0.DataOutputType;
import net.opengis.wps.v_2_0.DataTransmissionModeType;
import net.opengis.wps.v_2_0.ExecuteRequestType;
import net.opengis.wps.v_2_0.Format;
import net.opengis.wps.v_2_0.InputDescriptionType;
import net.opengis.wps.v_2_0.LiteralDataType;
import net.opengis.wps.v_2_0.LiteralDataType.LiteralDataDomain;
import net.opengis.wps.v_2_0.OutputDefinitionType;
import net.opengis.wps.v_2_0.OutputDescriptionType;
import net.opengis.wps.v_2_0.ProcessDescriptionType;
import net.opengis.wps.v_2_0.ProcessOffering;
import net.opengis.wps.v_2_0.ProcessSummaryType;
import net.opengis.wps.v_2_0.ReferenceType;
import net.opengis.wps.v_2_0.Result;
import net.opengis.wps.v_2_0.StatusInfo;
import net.opengis.wps.v_2_0.SupportedCRS;
import org.codice.ddf.spatial.ogc.wps.process.api.LiteralValue;
import org.codice.ddf.spatial.ogc.wps.process.api.WpsException;
import org.codice.ddf.spatial.process.api.Operation;
import org.codice.ddf.spatial.process.api.Process;
import org.codice.ddf.spatial.process.api.ProcessRepository;
import org.codice.ddf.spatial.process.api.description.BoundingBoxDataDescription;
import org.codice.ddf.spatial.process.api.description.DataDescription;
import org.codice.ddf.spatial.process.api.description.DataDescriptionGroup;
import org.codice.ddf.spatial.process.api.description.DataFormatDefinition;
import org.codice.ddf.spatial.process.api.description.DataType;
import org.codice.ddf.spatial.process.api.description.ExecutionDescription;
import org.codice.ddf.spatial.process.api.description.LiteralDataDescription;
import org.codice.ddf.spatial.process.api.description.Metadata;
import org.codice.ddf.spatial.process.api.description.Range;
import org.codice.ddf.spatial.process.api.description.TransmissionMode;
import org.codice.ddf.spatial.process.api.request.DataFormat;
import org.codice.ddf.spatial.process.api.request.DataGroup;
import org.codice.ddf.spatial.process.api.request.ExecutionRequest;
import org.codice.ddf.spatial.process.api.request.Literal;
import org.codice.ddf.spatial.process.api.request.OutputDefinition;
import org.codice.ddf.spatial.process.api.request.ProcessResult;
import org.codice.ddf.spatial.process.api.request.ProcessStatus;
import org.codice.ddf.spatial.process.api.request.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpsObjectConverter {

  private static final Logger LOGGER = LoggerFactory.getLogger(WpsObjectConverter.class);

  private static final DatatypeFactory DATATYPE_FACTORY;
  public static final String NO_SUCH_MODE_MSG =
      "The process does not permit the desired execution mode.";
  public static final String NO_SUCH_MODE = "NoSuchMode";

  static {
    try {
      DATATYPE_FACTORY = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final ObjectFactory owsObjectFactory = new ObjectFactory();

  private final net.opengis.wps.v_2_0.ObjectFactory wpsObjectFactory =
      new net.opengis.wps.v_2_0.ObjectFactory();

  public List<DataTransmissionModeType> createDataTransmissionType(
      @Nullable Set<TransmissionMode> transmissionModes) {
    List<DataTransmissionModeType> dataTransmissionModeTypes = new ArrayList<>();
    if (transmissionModes != null) {
      for (TransmissionMode mode : transmissionModes) {
        if (mode == TransmissionMode.VALUE) {
          dataTransmissionModeTypes.add(DataTransmissionModeType.VALUE);

        } else if (mode == TransmissionMode.REFERENCE) {
          dataTransmissionModeTypes.add(DataTransmissionModeType.REFERENCE);
        }
      }
    }
    return dataTransmissionModeTypes;
  }

  public List<OutputDescriptionType> createOutputDescriptions(
      List<DataDescription> outputDescriptions) {
    List<OutputDescriptionType> outputDescriptionTypes = new ArrayList<>();
    for (DataDescription dataDesc : outputDescriptions) {
      OutputDescriptionType outputDescriptionType = wpsObjectFactory.createOutputDescriptionType();
      outputDescriptionType.setAbstract(createLanguageStrings(dataDesc.getDescription()));
      outputDescriptionType.setIdentifier(createCodeType(dataDesc.getId()));
      outputDescriptionType.setTitle(createLanguageStrings(dataDesc.getName()));
      if (dataDesc.getMetadata() != null) {
        outputDescriptionType.setMetadata(createMetadata(dataDesc.getMetadata()));
        outputDescriptionType.setKeywords(createKeywords(dataDesc.getMetadata().getKeywords()));
      }
      if (dataDesc instanceof DataDescriptionGroup
          && !((DataDescriptionGroup) dataDesc).getDataDescriptions().isEmpty()) {
        outputDescriptionType.setOutput(
            createOutputDescriptions(((DataDescriptionGroup) dataDesc).getDataDescriptions()));
      } else {
        outputDescriptionType.setDataDescription(createDataDescriptionType(dataDesc));
      }
      outputDescriptionTypes.add(outputDescriptionType);
    }

    return outputDescriptionTypes;
  }

  public List<InputDescriptionType> createInputDescriptions(
      List<DataDescription> dataDescriptions) {
    List<InputDescriptionType> inputDescriptionTypes = new ArrayList<>();
    for (DataDescription dataDesc : dataDescriptions) {
      InputDescriptionType inputDescriptionType = wpsObjectFactory.createInputDescriptionType();
      inputDescriptionType.setMaxOccurs(String.valueOf(dataDesc.getMaxOccurs()));
      inputDescriptionType.setMinOccurs(dataDesc.getMinOccurs());
      inputDescriptionType.setAbstract(createLanguageStrings(dataDesc.getDescription()));
      inputDescriptionType.setIdentifier(createCodeType(dataDesc.getId()));
      inputDescriptionType.setTitle(createLanguageStrings(dataDesc.getName()));
      if (dataDesc.getMetadata() != null) {
        inputDescriptionType.setMetadata(createMetadata(dataDesc.getMetadata()));
        inputDescriptionType.setKeywords(createKeywords(dataDesc.getMetadata().getKeywords()));
      }
      if (dataDesc instanceof DataDescriptionGroup
          && !((DataDescriptionGroup) dataDesc).getDataDescriptions().isEmpty()) {
        inputDescriptionType.setInput(
            createInputDescriptions(((DataDescriptionGroup) dataDesc).getDataDescriptions()));
      } else {
        inputDescriptionType.setDataDescription(createDataDescriptionType(dataDesc));
      }
      inputDescriptionTypes.add(inputDescriptionType);
    }
    return inputDescriptionTypes;
  }

  public List<ProcessSummaryType> createProcessSummary(ProcessRepository processesRepository) {
    List<ProcessSummaryType> processSummaryTypes = new ArrayList<>();
    for (Process process : processesRepository.getProcesses()) {
      try {

        ProcessSummaryType processSummaryType = wpsObjectFactory.createProcessSummaryType();
        processSummaryType.setJobControlOptions(getJobControlOptions(process.getOperations()));
        processSummaryType.setProcessVersion(process.getVersion());
        processSummaryType.setIdentifier(createCodeType(process.getId()));
        processSummaryType.setTitle(createLanguageStrings(process.getTitle()));
        processSummaryType.setAbstract(createLanguageStrings(process.getDescription()));
        processSummaryType.setMetadata(createMetadata(process.getMetadata()));
        if (process.getMetadata() != null) {
          processSummaryType.setKeywords(createKeywords(process.getMetadata().getKeywords()));
        }
        processSummaryType.setOutputTransmission(
            createDataTransmissionType(process.getExecutionDescription().getTransmissionModes()));
        processSummaryTypes.add(processSummaryType);
      } catch (RuntimeException e) {
        LOGGER.debug("Error getting process summary from process: {}", process.getId(), e);
      }
    }
    return processSummaryTypes;
  }

  public List<ProcessOffering> createProcessOfferings(ProcessRepository processRepository) {
    List<ProcessOffering> processOfferings = new ArrayList<>();

    for (Process process : processRepository.getProcesses()) {
      try {
        processOfferings.add(createProcessOffering(process));
      } catch (RuntimeException e) {
        LOGGER.debug("Error creating process offering", e);
      }
    }
    return processOfferings;
  }

  public List<ProcessOffering> createProcessOfferings(
      ProcessRepository processRepository, Set<String> processIds) {
    return processIds
        .stream()
        .map(pid -> createProcessOffering(processRepository, pid))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  public Optional<ProcessOffering> createProcessOffering(
      ProcessRepository processRepository, String processId) {
    try {
      Optional<Process> process = processRepository.getProcess(processId);
      return process.map(this::createProcessOffering);
    } catch (RuntimeException e) {
      LOGGER.debug("Error getting algorithm {}", processId, e);
    }
    return Optional.empty();
  }

  public ProcessOffering createProcessOffering(Process process) {
    ProcessOffering processOffering = wpsObjectFactory.createProcessOffering();
    ExecutionDescription executionDescription = process.getExecutionDescription();
    processOffering.setJobControlOptions(getJobControlOptions(process.getOperations()));
    processOffering.setProcessVersion(process.getVersion());
    ProcessDescriptionType processDescriptionType = wpsObjectFactory.createProcessDescriptionType();
    processDescriptionType.setIdentifier(createCodeType(process.getId()));
    processDescriptionType.setTitle(createLanguageStrings(process.getTitle()));
    processDescriptionType.setAbstract(createLanguageStrings(process.getDescription()));
    processDescriptionType.setMetadata(createMetadata(process.getMetadata()));
    if (process.getMetadata() != null) {
      processDescriptionType.setKeywords(createKeywords(process.getMetadata().getKeywords()));
    }
    processOffering.setOutputTransmission(
        createDataTransmissionType(process.getExecutionDescription().getTransmissionModes()));
    processDescriptionType.setInput(
        createInputDescriptions(executionDescription.getInputDescriptions()));
    processDescriptionType.setOutput(
        createOutputDescriptions(executionDescription.getOutputDescriptions()));
    processOffering.setProcess(processDescriptionType);

    return processOffering;
  }

  public JAXBElement<BoundingBoxData> createDataDescriptionType(
      BoundingBoxDataDescription boundingBoxDataDescription) {
    List<Format> formats = createFormats(boundingBoxDataDescription);
    BoundingBoxData boundingBoxData = wpsObjectFactory.createBoundingBoxData();
    List<SupportedCRS> supportedCRSs =
        boundingBoxDataDescription
            .getSupportedCoordRefSys()
            .stream()
            .map(
                crs -> {
                  SupportedCRS scrs = wpsObjectFactory.createSupportedCRS();
                  scrs.setValue(crs);
                  return scrs;
                })
            .collect(Collectors.toList());
    if (!supportedCRSs.isEmpty()) {
      supportedCRSs.get(0).setDefault(true);
    }
    boundingBoxData.setSupportedCRS(supportedCRSs);
    boundingBoxData.setFormat(formats);
    return wpsObjectFactory.createBoundingBoxData(boundingBoxData);
  }

  public JAXBElement<LiteralDataType> createDataDescriptionType(
      LiteralDataDescription literalDataDescription) {
    List<Format> formats = createFormats(literalDataDescription);
    LiteralDataType literalDataType = wpsObjectFactory.createLiteralDataType();
    LiteralDataDomain literalDataDomain = wpsObjectFactory.createLiteralDataTypeLiteralDataDomain();
    // todo (RWY) - support multiple choices for the value type
    literalDataDomain.setDefault(true);

    if (literalDataDescription.getDefaultValue() != null) {
      literalDataDomain.setDefaultValue(
          createValue(
              literalDataDescription.getDefaultValue(),
              literalDataDescription.getType(),
              literalDataDescription.getUnitOfMeasure()));
    }

    if (literalDataDescription.getType() != null) {
      literalDataDomain.setDataType(createDataType(literalDataDescription.getType()));
    }
    if (literalDataDescription.getEnumeratedValues() != null
        && !literalDataDescription.getEnumeratedValues().isEmpty()) {
      literalDataDomain.setAllowedValues(getAllowedValuesEnumeration(literalDataDescription));
    } else if (literalDataDescription.getRange() != null) {
      literalDataDomain.setAllowedValues(getAllowedValuesRange(literalDataDescription));
    } else if (literalDataDescription.getReference() != null) {
      ValuesReference valuesReference = getValuesReference(literalDataDescription);
      literalDataDomain.setValuesReference(valuesReference);
    } else {
      literalDataDomain.setAnyValue(owsObjectFactory.createAnyValue());
    }
    literalDataType.setFormat(formats);
    literalDataType.setLiteralDataDomain(Collections.singletonList(literalDataDomain));
    return wpsObjectFactory.createLiteralData(literalDataType);
  }

  private ValuesReference getValuesReference(LiteralDataDescription literalDataDescription) {
    ValuesReference valuesReference = owsObjectFactory.createValuesReference();
    valuesReference.setReference(String.valueOf(literalDataDescription.getReference().getUrl()));
    valuesReference.setValue(literalDataDescription.getReference().getDescription());
    return valuesReference;
  }

  private AllowedValues getAllowedValuesRange(LiteralDataDescription literalDataDescription) {
    Range range = literalDataDescription.getRange();
    AllowedValues allowedValues = owsObjectFactory.createAllowedValues();
    RangeType rangeType = owsObjectFactory.createRangeType();
    rangeType.setMaximumValue(
        createValue(
            range.getMaximumValue(),
            literalDataDescription.getType(),
            literalDataDescription.getUnitOfMeasure()));
    rangeType.setMinimumValue(
        createValue(
            range.getMinimumValue(),
            literalDataDescription.getType(),
            literalDataDescription.getUnitOfMeasure()));
    rangeType.setSpacing(
        createValue(
            range.getSpacing(),
            literalDataDescription.getType(),
            literalDataDescription.getUnitOfMeasure()));
    rangeType.setRangeClosure(Collections.singletonList(range.getClosure().value()));
    allowedValues.setValueOrRange(Collections.singletonList(rangeType));
    return allowedValues;
  }

  private AllowedValues getAllowedValuesEnumeration(LiteralDataDescription literalDataDescription) {
    AllowedValues allowedValues = owsObjectFactory.createAllowedValues();
    List<Object> valueTypes =
        literalDataDescription
            .getEnumeratedValues()
            .stream()
            .map(
                ev ->
                    createValue(
                        ev,
                        literalDataDescription.getType(),
                        literalDataDescription.getUnitOfMeasure()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    allowedValues.setValueOrRange(valueTypes);
    return allowedValues;
  }

  @SuppressWarnings("squid:S1452" /*Intentionally returning generic to avoid casting*/)
  public JAXBElement<? extends DataDescriptionType> createDataDescriptionType(
      DataDescription dataDescription) {

    if (dataDescription instanceof BoundingBoxDataDescription) {
      return createDataDescriptionType((BoundingBoxDataDescription) dataDescription);
    } else if (dataDescription instanceof LiteralDataDescription) {
      return createDataDescriptionType((LiteralDataDescription) dataDescription);
    } else {
      List<Format> formats = createFormats(dataDescription);
      ComplexDataType complexDataType = wpsObjectFactory.createComplexDataType();
      complexDataType.setFormat(formats);
      return wpsObjectFactory.createComplexData(complexDataType);
    }
  }

  private DomainMetadataType createDataType(DataType type) {
    DomainMetadataType domainMetadataType = owsObjectFactory.createDomainMetadataType();
    domainMetadataType.setReference(type.reference());
    domainMetadataType.setValue(type.value());
    return domainMetadataType;
  }

  @Nullable
  public LiteralValue createValue(
      @Nullable Serializable value, @Nullable DataType type, @Nullable String uom) {
    if (value == null) {
      return null;
    }
    LiteralValue literalValue = new LiteralValue();
    literalValue.setValue(value.toString());
    if (type != null) {
      literalValue.setDataType(type.reference());
    }
    literalValue.setUom(uom);
    return literalValue;
  }

  public List<Format> createFormats(DataDescription dataDescription) {
    List<Format> formats =
        dataDescription
            .getDataFormats()
            .stream()
            .map(this::createFormat)
            .collect(Collectors.toList());
    if (!formats.isEmpty()) {
      formats.get(0).setDefault(true);
    }
    return formats;
  }

  public Format createFormat(DataFormatDefinition dataFormat) {
    Format format = wpsObjectFactory.createFormat();
    format.setEncoding(dataFormat.getEncoding());
    format.setSchema(dataFormat.getSchema());
    format.setMimeType(dataFormat.getMimeType());
    format.setMaximumMegabytes(dataFormat.getMaximumMegabytes());
    return format;
  }

  public List<LanguageStringType> createLanguageStrings(String str) {
    LanguageStringType lst = owsObjectFactory.createLanguageStringType();
    lst.setValue(str);
    return Collections.singletonList(lst);
  }

  public LanguageStringType createLanguageString(String str) {
    LanguageStringType lst = owsObjectFactory.createLanguageStringType();
    lst.setValue(str);
    return lst;
  }

  public CodeType createCodeType(String str) {
    CodeType ct = owsObjectFactory.createCodeType();
    ct.setValue(str);
    return ct;
  }

  public List<KeywordsType> createKeywords(@Nullable Collection<String> keywords) {
    if (keywords == null || keywords.isEmpty()) {
      return Collections.emptyList();
    }
    KeywordsType keywordsType = owsObjectFactory.createKeywordsType();
    keywordsType.setKeyword(
        keywords.stream().map(this::createLanguageString).collect(Collectors.toList()));
    return Collections.singletonList(keywordsType);
  }

  @SuppressWarnings("squid:S1452" /*Intentionally returning generic to avoid casting*/)
  public List<JAXBElement<? extends MetadataType>> createMetadata(@Nullable Metadata metadata) {
    if (metadata == null) {
      return Collections.emptyList();
    }
    MetadataType metadataType = owsObjectFactory.createMetadataType();
    metadataType.setAbout(metadata.getDescription());
    metadataType.setRole(metadata.getRole());
    metadataType.setHref(metadata.getUrl());
    return Collections.singletonList(owsObjectFactory.createMetadata(metadataType));
  }

  public List<String> getJobControlOptions(@Nullable Set<Operation> operations) {
    if (operations == null) {
      return Collections.emptyList();
    }
    return operations.stream().map(Operation::value).collect(Collectors.toList());
  }

  public List<OutputDefinition> getOutputDefinitions(
      ExecutionDescription executionDescription,
      @Nullable List<OutputDefinitionType> outputDefinitionTypes) {
    List<DataDescription> outputDescriptions = executionDescription.getOutputDescriptions();
    TransmissionMode defaultTransmissionMode = executionDescription.getTransmissionModes().first();
    // if no output definitions then use the descriptions to generate the default outputDefinition
    if (outputDefinitionTypes == null || outputDefinitionTypes.isEmpty()) {
      return outputDescriptions
          .stream()
          .map(od -> getOutputDefinition(od, defaultTransmissionMode, null))
          .collect(Collectors.toList());
    }

    Map<String, DataDescription> outputDescriptionsMap =
        outputDescriptions
            .stream()
            .collect(Collectors.toMap(DataDescription::getId, Function.identity()));

    List<String> extraneousOutputDefinitions =
        outputDefinitionTypes
            .stream()
            .filter(odt -> !outputDescriptionsMap.containsKey(odt.getId()))
            .map(OutputDefinitionType::getId)
            .collect(Collectors.toList());
    if (!extraneousOutputDefinitions.isEmpty()) {
      throw new WpsException(
          "One or more of the input or output formats specified in the request did not match with any of the formats defined for that particular input or output.",
          "NoSuchFormat",
          extraneousOutputDefinitions.stream().collect(Collectors.joining(",")));
    }
    return outputDefinitionTypes
        .stream()
        .map(
            odt ->
                getOutputDefinition(
                    outputDescriptionsMap.get(odt.getId()), defaultTransmissionMode, odt))
        .collect(Collectors.toList());
  }

  public OutputDefinition getOutputDefinition(
      DataDescription dataDescriptionDefault,
      @Nullable TransmissionMode defaultTransmissionMode,
      @Nullable OutputDefinitionType odt) {
    OutputDefinition outputDefinition = new OutputDefinition(dataDescriptionDefault.getId());
    if (odt != null) {
      outputDefinition.setFormat(
          getDataFormatWithDefaults(
              dataDescriptionDefault, odt.getEncoding(), odt.getMimeType(), odt.getSchema()));
      DataTransmissionModeType transmission = odt.getTransmission();
      if (transmission != null) {
        if (transmission == DataTransmissionModeType.REFERENCE) {
          outputDefinition.setTransmissionMode(TransmissionMode.REFERENCE);

        } else if (transmission == DataTransmissionModeType.VALUE) {
          outputDefinition.setTransmissionMode(TransmissionMode.VALUE);
        }
      }
    } else {
      outputDefinition.setFormat(
          getDataFormatWithDefaults(dataDescriptionDefault, null, null, null));
    }
    if (outputDefinition.getTransmissionMode() == null) {
      outputDefinition.setTransmissionMode(defaultTransmissionMode);
    }

    return outputDefinition;
  }

  public ExecutionRequest getExecutionRequest(
      ExecutionDescription executionDescription, ExecuteRequestType executeRequestType) {
    List<org.codice.ddf.spatial.process.api.request.Data> inputDatas =
        getInputData(executionDescription.getInputDescriptions(), executeRequestType.getInput());

    List<OutputDefinition> outputDefinitions =
        getOutputDefinitions(executionDescription, executeRequestType.getOutput());
    return new ExecutionRequest(
        inputDatas, outputDefinitions, Objects.equals("raw", executeRequestType.getResponse()));
  }

  public StatusInfo createStatusInfo(ProcessStatus processStatus) {
    StatusInfo statusInfo = wpsObjectFactory.createStatusInfo();
    statusInfo.setStatus(createStatusInfoStatus(processStatus));
    statusInfo.setJobID(processStatus.getRequestId());
    statusInfo.setPercentCompleted(processStatus.getPercentComplete());
    if (processStatus.getEstimatedCompletionTime() != null) {
      statusInfo.setEstimatedCompletion(
          DATATYPE_FACTORY.newXMLGregorianCalendar(
              GregorianCalendar.from(
                  processStatus.getEstimatedCompletionTime().atZone(ZoneId.systemDefault()))));
    }
    return statusInfo;
  }

  /**
   * @param inputDescriptionsList
   * @param dits
   * @return
   * @throws WpsException
   */
  public List<org.codice.ddf.spatial.process.api.request.Data> getInputData(
      List<DataDescription> inputDescriptionsList, List<DataInputType> dits) {
    Map<String, DataDescription> inputDescriptions =
        inputDescriptionsList
            .stream()
            .collect(Collectors.toMap(DataDescription::getId, Function.identity()));
    List<org.codice.ddf.spatial.process.api.request.Data> inputData = new ArrayList<>();

    for (DataInputType dit : dits) {
      DataDescription dataDescription = inputDescriptions.get(dit.getId());
      if (dit.getData() != null) {
        Literal literal = getLiteral(dataDescription, dit);
        inputData.add(literal);
      } else if (dit.getInput() != null && !dit.getInput().isEmpty()) {
        if (!(inputDescriptions.get(dit.getId()) instanceof DataDescriptionGroup)) {
          throw new WpsException(
              "One or more of the input or output formats specified in the request did not match with any of the formats defined for that particular input or output.",
              "NoSuchFormat",
              dit.getId());
        }
        DataDescriptionGroup dataDescriptionGroup = (DataDescriptionGroup) dataDescription;
        inputData.add(
            new DataGroup(
                dit.getId(),
                getInputData(dataDescriptionGroup.getDataDescriptions(), dit.getInput())));

      } else if (dit.getReference() != null) {
        // TODO (RWY) - currently just pass along the reference but maybe we should handle initially
        // fetching it here?
        inputData.add(getReference(dataDescription, dit.getReference(), dit.getId()));
      }
    }

    return inputData;
  }

  public Literal getLiteral(DataDescription dataDescription, DataInputType dit) {
    Literal literal = new Literal(dit.getId());
    DataFormat dataFormat;

    if (dit.getData() != null) {
      dataFormat =
          getDataFormatWithDefaults(
              dataDescription,
              dit.getData().getEncoding(),
              dit.getData().getMimeType(),
              dit.getData().getSchema());
    } else {
      dataFormat = getDataFormatWithDefaults(dataDescription, null, null, null);
    }
    literal.setFormat(dataFormat);
    if (dataDescription instanceof LiteralDataDescription) {
      LiteralDataDescription literalDataDescription = (LiteralDataDescription) dataDescription;
      DataType type = literalDataDescription.getType();
      Serializable rawValue =
          dit.getData()
              .getContent()
              .stream()
              .findFirst()
              .orElse(literalDataDescription.getDefaultValue());

      if (rawValue instanceof String) {
        literal.setValue(type.parseFromString((String) rawValue));
      } else {
        literal.setValue(rawValue);
      }
    } else {
      // Complex data that is passed by value i.e a blob of xml/json or something base64 encoded
      literal.setValue(dit.getData().getContent().stream().findFirst().orElse(null));
    }
    return literal;
  }

  /**
   * @param referenceType
   * @param id
   * @return
   * @throws WpsException
   */
  public Reference getReference(
      DataDescription dataDescription, ReferenceType referenceType, String id) {
    Reference reference = new Reference(id);
    DataFormat dataFormat =
        getDataFormatWithDefaults(
            dataDescription,
            referenceType.getEncoding(),
            referenceType.getMimeType(),
            referenceType.getSchema());
    reference.setFormat(dataFormat);
    try {
      reference.setUri(new URI(referenceType.getHref()));
    } catch (URISyntaxException e) {
      LOGGER.debug("Invalid URI:{}", referenceType.getHref(), e);
      throw new WpsException(
          "One of the referenced input data sets was inaccessible.", "DataNotAccessible", id);
    }
    return reference;
  }

  public DataFormat getDataFormatWithDefaults(
      @Nullable DataDescription dataDescription,
      @Nullable String encoding,
      @Nullable String mimeType,
      @Nullable String schema) {
    if (dataDescription == null
        || dataDescription.getDataFormats() == null
        || dataDescription.getDataFormats().isEmpty()) {
      return new DataFormat();
    }
    DataFormatDefinition defaultDataFormat = dataDescription.getDataFormats().get(0);
    DataFormat dataFormat = new DataFormat();
    if (encoding != null) {
      dataFormat.setEncoding(encoding);
    } else {
      dataFormat.setEncoding(defaultDataFormat.getEncoding());
    }
    if (mimeType != null) {
      dataFormat.setMimeType(mimeType);
    } else {
      dataFormat.setMimeType(defaultDataFormat.getMimeType());
    }
    if (schema != null) {
      dataFormat.setSchema(schema);
    } else {
      dataFormat.setSchema(defaultDataFormat.getSchema());
    }
    return dataFormat;
  }

  /**
   * @param algo
   * @param mode
   * @return
   * @throws WpsException
   */
  public Operation getOperation(Process algo, String mode) {

    switch (mode) {
      case "sync":
        if (!algo.getOperations().contains(Operation.SYNC_EXEC)) {
          throw new WpsException(NO_SUCH_MODE_MSG, NO_SUCH_MODE, mode);
        }
        return Operation.SYNC_EXEC;
      case "async":
        if (!algo.getOperations().contains(Operation.ASYNC_EXEC)) {
          throw new WpsException(NO_SUCH_MODE_MSG, NO_SUCH_MODE, mode);
        }
        return Operation.ASYNC_EXEC;
      case "auto":
        return algo.getOperations()
            .stream()
            .filter(o -> o == Operation.ASYNC_EXEC || o == Operation.SYNC_EXEC)
            .findFirst()
            .orElseThrow(() -> new WpsException(NO_SUCH_MODE_MSG, NO_SUCH_MODE, mode));
      default:
        throw new WpsException(NO_SUCH_MODE_MSG, NO_SUCH_MODE, mode);
    }
  }

  public Result createResult(@Nullable ProcessResult processResult) {

    Result result = wpsObjectFactory.createResult();
    if (processResult == null) {
      return result;
    }
    result.setJobID(processResult.getRequestId());
    List<org.codice.ddf.spatial.process.api.request.Data> outputs = processResult.getOutput();
    List<DataOutputType> dataOutputTypes =
        outputs.stream().map(this::getDataOutputType).collect(Collectors.toList());
    result.setOutput(dataOutputTypes);
    return result;
  }

  public DataOutputType getDataOutputType(
      org.codice.ddf.spatial.process.api.request.Data outputData) {
    DataOutputType dataOutputType = wpsObjectFactory.createDataOutputType();
    dataOutputType.setId(outputData.getId());
    if (outputData instanceof Reference) {
      dataOutputType.setReference(createReferenceType((Reference) outputData));
    } else if (outputData instanceof Literal) {
      dataOutputType.setData(createData((Literal) outputData));
    } else if (outputData instanceof DataGroup) {
      dataOutputType.setOutput(wpsObjectFactory.createDataOutputType());
      // WPS doesn't support nesting of multiple outputs
      dataOutputType
          .getOutput()
          .setOutput(
              getDataOutputType(
                  ((DataGroup) outputData).getData().stream().findFirst().orElse(null)));
    }
    return dataOutputType;
  }

  @Nullable
  public Data createData(@Nullable Literal outputLiteral) {
    if (outputLiteral == null) {
      return null;
    }
    Data data = wpsObjectFactory.createData();
    DataFormat df = outputLiteral.getFormat();
    if (df != null) {
      data.setSchema(df.getSchema());
      data.setMimeType(df.getMimeType());
      data.setEncoding(df.getEncoding());
    }
    if (outputLiteral.getValue() != null) {
      data.setContent(Collections.singletonList(outputLiteral.getValue()));
    }
    return data;
  }

  @Nullable
  public ReferenceType createReferenceType(@Nullable Reference reference) {
    if (reference == null) {
      return null;
    }
    ReferenceType referenceType = wpsObjectFactory.createReferenceType();
    referenceType.setHref(reference.getUri().toASCIIString());
    if (reference.getFormat() != null) {
      DataFormat df = reference.getFormat();
      referenceType.setEncoding(df.getEncoding());
      referenceType.setMimeType(df.getMimeType());
      referenceType.setSchema(df.getSchema());
    }
    return referenceType;
  }

  public String createStatusInfoStatus(ProcessStatus status) {
    switch (status.getStatus()) {
      case RECEIVED:
      case ACCEPTED:
        return "Accepted";
      case IN_PROGRESS:
        return "Running";
      case COMPLETED:
        return "Succeeded";
      case CANCELED:
        return "Dismissed"; // schema doesn't support this but the value is specified in the spec
      case REFUSED:
      case FAILED:
      default:
        return "Failed";
    }
  }
}
