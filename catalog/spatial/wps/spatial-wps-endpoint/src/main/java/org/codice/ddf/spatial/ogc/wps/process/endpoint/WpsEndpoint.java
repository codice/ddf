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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import net.opengis.ows.v_2_0.CodeType;
import net.opengis.ows.v_2_0.DCP;
import net.opengis.ows.v_2_0.DescriptionType;
import net.opengis.ows.v_2_0.HTTP;
import net.opengis.ows.v_2_0.KeywordsType;
import net.opengis.ows.v_2_0.LanguageStringType;
import net.opengis.ows.v_2_0.ObjectFactory;
import net.opengis.ows.v_2_0.OperationsMetadata;
import net.opengis.ows.v_2_0.RequestMethodType;
import net.opengis.ows.v_2_0.ServiceIdentification;
import net.opengis.ows.v_2_0.ServiceProvider;
import net.opengis.wps.v_2_0.Contents;
import net.opengis.wps.v_2_0.DescribeProcess;
import net.opengis.wps.v_2_0.Dismiss;
import net.opengis.wps.v_2_0.ExecuteRequestType;
import net.opengis.wps.v_2_0.GetCapabilitiesType;
import net.opengis.wps.v_2_0.GetResult;
import net.opengis.wps.v_2_0.GetStatus;
import net.opengis.wps.v_2_0.ProcessOfferings;
import net.opengis.wps.v_2_0.ProcessSummaryType;
import net.opengis.wps.v_2_0.StatusInfo;
import net.opengis.wps.v_2_0.WPSCapabilitiesType;
import org.codice.ddf.spatial.ogc.wps.process.api.Wps;
import org.codice.ddf.spatial.ogc.wps.process.api.WpsConstants;
import org.codice.ddf.spatial.ogc.wps.process.api.WpsException;
import org.codice.ddf.spatial.process.api.Operation;
import org.codice.ddf.spatial.process.api.Process;
import org.codice.ddf.spatial.process.api.ProcessException;
import org.codice.ddf.spatial.process.api.ProcessMonitor;
import org.codice.ddf.spatial.process.api.ProcessRepository;
import org.codice.ddf.spatial.process.api.description.ExecutionDescription;
import org.codice.ddf.spatial.process.api.request.ExecutionRequest;
import org.codice.ddf.spatial.process.api.request.Literal;
import org.codice.ddf.spatial.process.api.request.ProcessResult;
import org.codice.ddf.spatial.process.api.request.ProcessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of WPS 2.0 specification, Limitations of this implementation: Does not support
 * multiple choice inputs, outputs that are nested, no support for returning result expiration and
 * only limited support for inputs passed by reference (if the underlying process supports it).
 */
public class WpsEndpoint implements Wps {

  private static final Logger LOGGER = LoggerFactory.getLogger(WpsEndpoint.class);

  private static final Set<String> ALL = Collections.singleton("ALL");
  private static final String NO_SUCH_JOB_MSG =
      "The JobID from the request does not match any of the Jobs running on this server";
  private static final String NO_SUCH_JOB = "NoSuchJob";

  private final ObjectFactory owsObjectFactory = new ObjectFactory();

  private final net.opengis.wps.v_2_0.ObjectFactory wpsObjectFactory =
      new net.opengis.wps.v_2_0.ObjectFactory();

  private final WpsObjectConverter wpsObjectConverter = new WpsObjectConverter();

  private List<ProcessRepository> processRepositories = new ArrayList<>();

  private List<ProcessMonitor> processMonitors = Collections.emptyList();

  @Context private UriInfo uri;

  @Override
  public WPSCapabilitiesType getCapabilities(GetCapabilitiesType getCapabilitiesType)
      throws WpsException {
    List<String> versions = Collections.emptyList();
    if (getCapabilitiesType.getAcceptVersions() != null
        && getCapabilitiesType.getAcceptVersions().getVersion() != null) {
      versions = getCapabilitiesType.getAcceptVersions().getVersion();
    }
    List<String> sections = Collections.emptyList();
    if (getCapabilitiesType.getSections() != null
        && getCapabilitiesType.getSections().getSection() != null) {
      sections = getCapabilitiesType.getSections().getSection();
    }
    return getCapabilities(versions, sections);
  }

  /**
   * @param acceptVersions
   * @param sections
   * @param updateSequence
   * @param acceptFormats
   * @return
   * @throws WpsException
   */
  @Override
  public WPSCapabilitiesType getCapabilities(
      @Nullable String acceptVersions,
      @Nullable String sections,
      @Nullable String updateSequence,
      @Nullable String acceptFormats) {
    List<String> versions = Collections.emptyList();
    if (acceptVersions != null) {
      versions =
          Arrays.asList(acceptVersions.split(","))
              .stream()
              .map(String::trim)
              .collect(Collectors.toList());
    }
    List<String> sectionsList = Collections.emptyList();
    if (sections != null) {
      sectionsList =
          Arrays.asList(sections.split(","))
              .stream()
              .map(String::trim)
              .collect(Collectors.toList());
    }
    return getCapabilities(versions, sectionsList);
  }

  /**
   * @param acceptVersions
   * @param sections
   * @return
   * @throws WpsException
   */
  public WPSCapabilitiesType getCapabilities(
      List<String> acceptVersions, @Nullable List<String> sections) {
    Validator.validateAcceptedVersions(acceptVersions);

    if (sections == null || sections.isEmpty()) {
      return createCapabilities(WpsConstants.GET_CAPABILITIES_SECTIONS);
    }

    return createCapabilities(sections);
  }

  private WPSCapabilitiesType createCapabilities(List<String> sections) {
    List<ProcessSummaryType> processSummary = new ArrayList<>();
    for (ProcessRepository pr : processRepositories) {
      try {
        processSummary.addAll(wpsObjectConverter.createProcessSummary(pr));
      } catch (RuntimeException e) {
        LOGGER.debug("Error getting process summary from processRepository.", e);
      }
    }

    WPSCapabilitiesType wpsCapabilitiesType = wpsObjectFactory.createWPSCapabilitiesType();
    wpsCapabilitiesType.setVersion("2.0.0");

    if (sections.contains(WpsConstants.CAPABILITY_CONTENTS)) {
      wpsCapabilitiesType.setContents(getContents(processSummary));
    }

    if (sections.contains(WpsConstants.CAPABILITY_OPS_METADATA)) {
      wpsCapabilitiesType.setOperationsMetadata(getOperationsMetadata());
    }

    if (sections.contains(WpsConstants.CAPABILITY_SERVICE_ID)) {
      wpsCapabilitiesType.setServiceIdentification(getServiceId(processSummary));
    }

    if (sections.contains(WpsConstants.CAPABILITY_SERVICE_PROVIDER)) {
      wpsCapabilitiesType.setServiceProvider(getServiceProvider());
    }
    return wpsCapabilitiesType;
  }

  private Contents getContents(List<ProcessSummaryType> processSummary) {
    Contents contents = wpsObjectFactory.createContents();
    contents.setProcessSummary(processSummary);
    return contents;
  }

  private ServiceProvider getServiceProvider() {
    ServiceProvider serviceProvider = owsObjectFactory.createServiceProvider();
    serviceProvider.setProviderName("DDF");
    serviceProvider.setProviderSite(owsObjectFactory.createOnlineResourceType());
    serviceProvider.setServiceContact(owsObjectFactory.createResponsiblePartySubsetType());
    return serviceProvider;
  }

  private ServiceIdentification getServiceId(List<ProcessSummaryType> processSummary) {
    ServiceIdentification serviceIdentification = owsObjectFactory.createServiceIdentification();

    serviceIdentification.setFees("NONE");
    serviceIdentification.setAccessConstraints(Collections.singletonList("NONE"));
    serviceIdentification.setServiceType(wpsObjectConverter.createCodeType("WPS"));
    Set<String> keywords =
        processSummary
            .stream()
            .map(DescriptionType::getKeywords)
            .flatMap(List::stream)
            .map(KeywordsType::getKeyword)
            .flatMap(List::stream)
            .map(LanguageStringType::getValue)
            .collect(Collectors.toSet());
    if (!keywords.isEmpty()) {
      serviceIdentification.setKeywords(wpsObjectConverter.createKeywords(keywords));
    }
    serviceIdentification.setTitle(
        wpsObjectConverter.createLanguageStrings("Web Processing Service"));
    serviceIdentification.setAbstract(wpsObjectConverter.createLanguageStrings("DDF WPS Endpoint"));
    return serviceIdentification;
  }

  private OperationsMetadata getOperationsMetadata() {
    OperationsMetadata operationsMetadata = owsObjectFactory.createOperationsMetadata();
    List<net.opengis.ows.v_2_0.Operation> operations = operationsMetadata.getOperation();

    RequestMethodType requestMethodType = owsObjectFactory.createRequestMethodType();
    requestMethodType.setHref(uri.getBaseUri().toASCIIString());
    HTTP httpPost = owsObjectFactory.createHTTP();
    HTTP httpGetPost = owsObjectFactory.createHTTP();
    httpPost.setGetOrPost(
        Collections.singletonList(owsObjectFactory.createHTTPPost(requestMethodType)));
    httpGetPost.setGetOrPost(
        Arrays.asList(
            owsObjectFactory.createHTTPGet(requestMethodType),
            owsObjectFactory.createHTTPPost(requestMethodType)));

    DCP post = owsObjectFactory.createDCP();
    post.setHTTP(httpPost);
    DCP postget = owsObjectFactory.createDCP();
    postget.setHTTP(httpGetPost);

    net.opengis.ows.v_2_0.Operation getCapabilities = new net.opengis.ows.v_2_0.Operation();
    getCapabilities.setDCP(Collections.singletonList(postget));
    getCapabilities.setName("GetCapabilities");
    operations.add(getCapabilities);

    net.opengis.ows.v_2_0.Operation describeProcess = new net.opengis.ows.v_2_0.Operation();
    describeProcess.setDCP(Collections.singletonList(postget));
    describeProcess.setName("DescribeProcess");
    operations.add(describeProcess);

    net.opengis.ows.v_2_0.Operation execute = new net.opengis.ows.v_2_0.Operation();
    execute.setDCP(Collections.singletonList(post));
    execute.setName("Execute");
    operations.add(execute);

    net.opengis.ows.v_2_0.Operation getStatus = new net.opengis.ows.v_2_0.Operation();
    getStatus.setDCP(Collections.singletonList(postget));
    getStatus.setName("GetStatus");
    operations.add(getStatus);

    net.opengis.ows.v_2_0.Operation getResult = new net.opengis.ows.v_2_0.Operation();
    getResult.setDCP(Collections.singletonList(postget));
    getResult.setName("GetResult");
    operations.add(getResult);

    net.opengis.ows.v_2_0.Operation dismiss = new net.opengis.ows.v_2_0.Operation();
    dismiss.setDCP(Collections.singletonList(postget));
    dismiss.setName("Dismiss");
    operations.add(dismiss);
    return operationsMetadata;
  }

  /**
   * @param identifiers
   * @param lang
   * @return
   * @throws WpsException
   */
  @Override
  public ProcessOfferings describeProcess(@Nullable String identifiers, @Nullable String lang) {
    Set<String> processIds = ALL;
    if (identifiers != null) {
      processIds = Arrays.stream(identifiers.split(",")).collect(Collectors.toSet());
    }
    return describeProcess(processIds);
  }

  /**
   * @param processIds
   * @return
   * @throws WpsException
   */
  public ProcessOfferings describeProcess(Set<String> processIds) {
    ProcessOfferings processOfferings = wpsObjectFactory.createProcessOfferings();

    if (processIds.isEmpty() || processIds.contains("ALL")) {
      for (ProcessRepository pr : processRepositories) {
        try {
          processOfferings
              .getProcessOffering()
              .addAll(wpsObjectConverter.createProcessOfferings(pr));
        } catch (RuntimeException e) {
          LOGGER.debug("Error getting process offering from processRepository.", e);
        }
      }
    } else {

      for (ProcessRepository pr : processRepositories) {
        try {
          processOfferings
              .getProcessOffering()
              .addAll(wpsObjectConverter.createProcessOfferings(pr, processIds));
        } catch (RuntimeException e) {
          LOGGER.debug("Error getting process offering from processRepository.", e);
        }
      }
    }
    Validator.validateDescribeRequest(processIds, processOfferings);
    return processOfferings;
  }

  /**
   * @param describeProcess
   * @return
   * @throws WpsException
   */
  @Override
  public ProcessOfferings describeProcess(DescribeProcess describeProcess) {
    Set<String> processIds =
        describeProcess
            .getIdentifier()
            .stream()
            .map(CodeType::getValue)
            .collect(Collectors.toSet());
    if (processIds.isEmpty()) {
      processIds = ALL;
    }
    return describeProcess(processIds);
  }

  // todo - (RWY) add some sort of permissions check, may need to bubble it down into the algo's and
  // add to describe

  /**
   * @param executeRequestType
   * @return
   * @throws WpsException
   */
  @Override
  public Response execute(ExecuteRequestType executeRequestType) {
    String processId = executeRequestType.getIdentifier().getValue();
    Process process = getProcess(processId);

    ExecutionDescription executionDescription = process.getExecutionDescription();
    ExecutionRequest executionRequest =
        wpsObjectConverter.getExecutionRequest(executionDescription, executeRequestType);
    Validator.validate(executionDescription, executionRequest);
    Operation operation = wpsObjectConverter.getOperation(process, executeRequestType.getMode());
    if (operation == Operation.SYNC_EXEC) {
      ProcessResult processResult = process.syncExecute(executionRequest);
      return getResponse(processResult);
    } else {
      ProcessStatus processStatus = process.asyncExecute(executionRequest);
      return Response.ok(wpsObjectConverter.createStatusInfo(processStatus)).build();
    }
  }

  private Response getResponse(ProcessResult processResult) {
    if (processResult.isRaw()) {
      Optional<Literal> op =
          processResult
              .getOutput()
              .stream()
              .filter(Literal.class::isInstance)
              .map(Literal.class::cast)
              .findFirst();
      if (!op.isPresent()) {
        return Response.noContent().build();
      }
      Literal literal = op.get();
      return Response.ok(literal.getValue())
          .encoding(literal.getFormat().getEncoding())
          .type(literal.getFormat().getMimeType())
          .build();
    }
    return Response.ok(wpsObjectConverter.createResult(processResult))
        .encoding(StandardCharsets.UTF_8.name())
        .type(MediaType.APPLICATION_XML)
        .build();
  }

  /**
   * @param jobId
   * @return
   * @throws WpsException
   */
  @Override
  @SuppressWarnings("squid:S3655" /*Optional::isPresent is checked prior to get*/)
  public StatusInfo getStatus(String jobId) {
    ProcessStatus status =
        processMonitors
            .stream()
            .map(
                am -> {
                  try {
                    return am.getRequestStatus(jobId);
                  } catch (ProcessException e) {
                    throw e;
                  } catch (RuntimeException e) {
                    LOGGER.debug("Error monitoring requests", e);
                    return Optional.<ProcessStatus>empty();
                  }
                })
            .filter(Optional::isPresent)
            .findFirst()
            .orElseThrow(() -> new WpsException(NO_SUCH_JOB_MSG, NO_SUCH_JOB, jobId))
            .get();
    return wpsObjectConverter.createStatusInfo(status);
  }

  /**
   * @param getStatus
   * @return
   * @throws WpsException
   */
  @Override
  public StatusInfo getStatus(GetStatus getStatus) {
    return getStatus(getStatus.getJobID());
  }

  /**
   * @param jobId
   * @return
   * @throws WpsException
   */
  @Override
  @SuppressWarnings("squid:S3655" /*Optional::isPresent is checked prior to get*/)
  public Response getResult(String jobId) {
    ProcessResult processResult =
        processMonitors
            .stream()
            .map(
                am -> {
                  try {
                    return am.getRequestResult(jobId);
                  } catch (ProcessException e) {
                    throw e;
                  } catch (RuntimeException e) {
                    LOGGER.debug("Error getting results", e);
                    return Optional.<ProcessResult>empty();
                  }
                })
            .filter(Optional::isPresent)
            .findFirst()
            .orElseThrow(() -> new WpsException(NO_SUCH_JOB_MSG, NO_SUCH_JOB, jobId))
            .get();
    return getResponse(processResult);
  }

  /**
   * @param getResult
   * @return
   * @throws WpsException
   */
  @Override
  public Response getResult(GetResult getResult) {
    return getResult(getResult.getJobID());
  }

  /**
   * @param jobId
   * @return
   * @throws WpsException
   */
  @Override
  @SuppressWarnings("squid:S3655" /*Optional::isPresent is checked prior to get*/)
  public StatusInfo dismiss(String jobId) {
    ProcessStatus status =
        processMonitors
            .stream()
            .map(
                am -> {
                  try {
                    return am.dismissRequest(jobId);
                  } catch (ProcessException e) {
                    throw e;
                  } catch (RuntimeException e) {
                    LOGGER.debug("Error dismissing requests", e);
                    return Optional.<ProcessStatus>empty();
                  }
                })
            .filter(Optional::isPresent)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> new WpsException(NO_SUCH_JOB_MSG, NO_SUCH_JOB, jobId))
            .get();
    return wpsObjectConverter.createStatusInfo(status);
  }

  /**
   * @param dismiss
   * @return
   * @throws WpsException
   */
  @Override
  public StatusInfo dismiss(Dismiss dismiss) {
    return dismiss(dismiss.getJobID());
  }

  /**
   * @param processId
   * @return
   * @throws WpsException
   */
  private Process getProcess(String processId) {
    for (ProcessRepository processRepository : processRepositories) {
      try {
        Optional<Process> process = processRepository.getProcess(processId);
        if (process.isPresent()) {
          return process.get();
        }
      } catch (RuntimeException e) {
        LOGGER.debug("Error retrieving {} process from repository", processId, e);
      }
    }
    throw new WpsException(
        "One of the identifiers passed does not match with any of the processes offered by this server.",
        "NoSuchProcess",
        processId);
  }

  public List<ProcessRepository> getProcessRepositories() {
    return Collections.unmodifiableList(processRepositories);
  }

  public void setProcessRepositories(List<ProcessRepository> processRepositories) {
    this.processRepositories = processRepositories;
  }

  public List<ProcessMonitor> getProcessMonitors() {
    return Collections.unmodifiableList(processMonitors);
  }

  public void setProcessMonitors(List<ProcessMonitor> processMonitors) {
    this.processMonitors = processMonitors;
  }

  public void setUri(UriInfo uri) {
    this.uri = uri;
  }
}
