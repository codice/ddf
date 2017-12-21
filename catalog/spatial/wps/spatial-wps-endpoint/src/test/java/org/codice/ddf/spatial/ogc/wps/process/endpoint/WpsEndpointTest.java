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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import net.opengis.ows.v_2_0.CodeType;
import net.opengis.wps.v_2_0.Data;
import net.opengis.wps.v_2_0.DataInputType;
import net.opengis.wps.v_2_0.DataOutputType;
import net.opengis.wps.v_2_0.DataTransmissionModeType;
import net.opengis.wps.v_2_0.DescribeProcess;
import net.opengis.wps.v_2_0.Dismiss;
import net.opengis.wps.v_2_0.ExecuteRequestType;
import net.opengis.wps.v_2_0.GetResult;
import net.opengis.wps.v_2_0.GetStatus;
import net.opengis.wps.v_2_0.OutputDefinitionType;
import net.opengis.wps.v_2_0.ProcessOffering;
import net.opengis.wps.v_2_0.ProcessOfferings;
import net.opengis.wps.v_2_0.Result;
import net.opengis.wps.v_2_0.StatusInfo;
import net.opengis.wps.v_2_0.WPSCapabilitiesType;
import org.codice.ddf.spatial.ogc.wps.process.api.WpsException;
import org.codice.ddf.spatial.process.api.Operation;
import org.codice.ddf.spatial.process.api.Process;
import org.codice.ddf.spatial.process.api.ProcessMonitor;
import org.codice.ddf.spatial.process.api.ProcessRepository;
import org.codice.ddf.spatial.process.api.description.BoundingBoxDataDescription;
import org.codice.ddf.spatial.process.api.description.ComplexDataDescription;
import org.codice.ddf.spatial.process.api.description.DataDescription;
import org.codice.ddf.spatial.process.api.description.DataDescriptionGroup;
import org.codice.ddf.spatial.process.api.description.DataFormatDefinition;
import org.codice.ddf.spatial.process.api.description.DataType;
import org.codice.ddf.spatial.process.api.description.ExecutionDescription;
import org.codice.ddf.spatial.process.api.description.LiteralDataDescription;
import org.codice.ddf.spatial.process.api.description.Metadata;
import org.codice.ddf.spatial.process.api.description.TransmissionMode;
import org.codice.ddf.spatial.process.api.request.DataFormat;
import org.codice.ddf.spatial.process.api.request.DataGroup;
import org.codice.ddf.spatial.process.api.request.ExecutionRequest;
import org.codice.ddf.spatial.process.api.request.Literal;
import org.codice.ddf.spatial.process.api.request.OutputDefinition;
import org.codice.ddf.spatial.process.api.request.ProcessResult;
import org.codice.ddf.spatial.process.api.request.ProcessStatus;
import org.codice.ddf.spatial.process.api.request.Reference;
import org.codice.ddf.spatial.process.api.request.Status;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WpsEndpointTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  private WpsEndpoint wpsEndpoint;

  @Mock private ProcessRepository processRepository;

  @Mock private Process simpleProcess;

  @Mock private ProcessStatus processStatus;

  @Mock private ProcessResult processResult;

  @Mock private Literal literal;

  @Mock private Reference reference;

  @Mock private DataFormat dataformat;

  @Mock private ProcessMonitor processMonitor;

  @Mock private UriInfo uriInfo;

  @Mock private LiteralDataDescription literalDataDesc;

  @Mock private DataDescription anyDataDesc;

  @Mock private BoundingBoxDataDescription boxDataDesc;

  @Mock private ComplexDataDescription complexDataDesc;

  private ComplexProcess complexProcess;

  @Before
  public void setUp() throws Exception {
    complexProcess = new ComplexProcess();
    wpsEndpoint = new WpsEndpoint();
    wpsEndpoint.setProcessRepositories(Collections.singletonList(processRepository));
    wpsEndpoint.setProcessMonitors(Collections.singletonList(processMonitor));
    wpsEndpoint.setUri(uriInfo);

    when(uriInfo.getBaseUri()).thenReturn(new URI("this:is:a:test"));

    when(processRepository.getProcesses()).thenReturn(Arrays.asList(simpleProcess, complexProcess));
    when(processRepository.getProcess(anyString())).thenReturn(Optional.of(simpleProcess));
    when(simpleProcess.getDescription()).thenReturn("Algo desc");
    when(simpleProcess.getId()).thenReturn("test");
    Set<Operation> operationSet = new HashSet<>();
    operationSet.add(Operation.ASYNC_EXEC);
    operationSet.add(Operation.SYNC_EXEC);
    operationSet.add(Operation.DISMISS);
    when(simpleProcess.getOperations()).thenReturn(operationSet);
    when(simpleProcess.getTitle()).thenReturn("Algo title");
    when(simpleProcess.getVersion()).thenReturn("1.0.2a-32");
    when(simpleProcess.asyncExecute(any(ExecutionRequest.class))).thenReturn(processStatus);
    when(simpleProcess.syncExecute(any(ExecutionRequest.class))).thenReturn(processResult);

    when(simpleProcess.getExecutionDescription())
        .thenReturn(
            new ExecutionDescription(
                Arrays.asList(literalDataDesc, boxDataDesc),
                Arrays.asList(complexDataDesc, anyDataDesc),
                new TreeSet<>(Collections.singleton(TransmissionMode.REFERENCE))));

    DataFormatDefinition dataformatdef = new DataFormatDefinition();
    dataformatdef.setEncoding("UTF-8");
    dataformatdef.setMimeType("text/plain");
    dataformatdef.setSchema("test-schema.txt");

    when(literalDataDesc.getName()).thenReturn("input desc 1");
    when(literalDataDesc.getId()).thenReturn("literalDataDesc");
    when(literalDataDesc.getMaxOccurs()).thenReturn(BigInteger.ONE);
    when(literalDataDesc.getMinOccurs()).thenReturn(BigInteger.ONE);
    when(literalDataDesc.getDescription()).thenReturn("a description for input 1");
    when(literalDataDesc.getDataFormats()).thenReturn(Collections.singletonList(dataformatdef));

    doReturn(DataType.STRING).when(literalDataDesc).getType();

    when(boxDataDesc.getName()).thenReturn("input desc 2");
    when(boxDataDesc.getId()).thenReturn("boxDataDesc");
    when(boxDataDesc.getMaxOccurs()).thenReturn(BigInteger.ONE);
    when(boxDataDesc.getMinOccurs()).thenReturn(BigInteger.ZERO);
    when(boxDataDesc.getDescription()).thenReturn("a description for input 2");
    when(boxDataDesc.getDataFormats()).thenReturn(Collections.singletonList(dataformatdef));

    when(complexDataDesc.getName()).thenReturn("output desc 1");
    when(complexDataDesc.getId()).thenReturn("complexDataDesc");
    when(complexDataDesc.getDescription()).thenReturn("a description for output 1");
    when(complexDataDesc.getDataFormats()).thenReturn(Collections.singletonList(dataformatdef));

    when(anyDataDesc.getName()).thenReturn("output desc 2");
    when(anyDataDesc.getId()).thenReturn("anyDataDesc");
    when(anyDataDesc.getDescription()).thenReturn("a description for output 2");
    when(anyDataDesc.getDataFormats()).thenReturn(Collections.singletonList(dataformatdef));

    when(processStatus.getStatus()).thenReturn(Status.ACCEPTED);

    List<org.codice.ddf.spatial.process.api.request.Data> result = new ArrayList<>();
    result.add(literal);
    result.add(reference);
    when(reference.getFormat()).thenReturn(dataformat);
    when(reference.getUri()).thenReturn(new URI("file://path/to/product.nitf"));
    when(processResult.getOutput()).thenReturn(result);

    when(processMonitor.dismissRequest(anyString())).thenReturn(Optional.of(processStatus));
    when(processMonitor.getRequestStatus(anyString())).thenReturn(Optional.of(processStatus));
    when(processMonitor.getRequestResult(anyString())).thenReturn(Optional.of(processResult));
  }

  @Test
  public void getCapabilities() throws Exception {
    WPSCapabilitiesType capabilities =
        wpsEndpoint.getCapabilities(Collections.emptyList(), Collections.emptyList());
    assertThat(
        capabilities
            .getOperationsMetadata()
            .getOperation()
            .stream()
            .map(net.opengis.ows.v_2_0.Operation::getName)
            .collect(Collectors.toList()),
        containsInAnyOrder(
            "GetCapabilities", "DescribeProcess", "Execute", "GetStatus", "GetResult", "Dismiss"));
    assertThat(capabilities.getContents().getProcessSummary(), Matchers.iterableWithSize(2));
  }

  @Test
  public void describeProcess() throws Exception {

    DescribeProcess dp = new DescribeProcess();
    CodeType ct = new CodeType();
    ct.setValue("ALL");
    dp.setIdentifier(Collections.singletonList(ct));
    ProcessOfferings processOfferings = wpsEndpoint.describeProcess(dp);
    assertThat(processOfferings, is(not(nullValue())));
    assertThat(processOfferings.getProcessOffering(), is(not(empty())));
    ProcessOffering processOffering = processOfferings.getProcessOffering().get(0);
    assertThat(
        processOffering.getJobControlOptions(),
        containsInAnyOrder("async-execute", "sync-execute", "dismiss"));
  }

  @Test
  public void asyncExecute() throws Exception {
    ExecuteRequestType executeRequestType = new ExecuteRequestType();
    CodeType ct = new CodeType();
    ct.setValue("test");
    executeRequestType.setIdentifier(ct);
    executeRequestType.setResponse("document");
    executeRequestType.setMode("async");
    DataInputType dataInputType = new DataInputType();
    Data data = new Data();
    data.setContent(Collections.singletonList("argument"));
    data.setEncoding("UTF-8");
    data.setMimeType("text/plain");
    data.setSchema("test-schema.txt");
    dataInputType.setData(data);
    dataInputType.setId(literalDataDesc.getId());
    executeRequestType.setInput(Collections.singletonList(dataInputType));
    OutputDefinitionType dataOutputType = new OutputDefinitionType();
    dataOutputType.setId(complexDataDesc.getId());
    dataOutputType.setEncoding("UTF-8");
    dataOutputType.setMimeType("text/plain");
    dataOutputType.setSchema("test-schema.txt");
    dataOutputType.setTransmission(DataTransmissionModeType.REFERENCE);
    executeRequestType.setOutput(Collections.singletonList(dataOutputType));

    Response response = wpsEndpoint.execute(executeRequestType);
    verify(simpleProcess).asyncExecute(any(ExecutionRequest.class));
    assertThat(response.getEntity(), is(not(nullValue())));
  }

  @Test
  public void syncExecute() throws Exception {
    ExecuteRequestType executeRequestType = new ExecuteRequestType();
    CodeType ct = new CodeType();
    ct.setValue("test");
    executeRequestType.setIdentifier(ct);
    executeRequestType.setResponse("document");
    executeRequestType.setMode("sync");
    DataInputType dataInputType = new DataInputType();
    Data data = new Data();
    data.setContent(Collections.singletonList("argument"));
    data.setEncoding("UTF-8");
    data.setMimeType("text/plain");
    data.setSchema("test-schema.txt");
    dataInputType.setData(data);
    dataInputType.setId(literalDataDesc.getId());
    executeRequestType.setInput(Collections.singletonList(dataInputType));
    OutputDefinitionType dataOutputType = new OutputDefinitionType();
    dataOutputType.setEncoding("UTF-8");
    dataOutputType.setId(complexDataDesc.getId());
    dataOutputType.setMimeType("text/plain");
    dataOutputType.setSchema("test-schema.txt");
    dataOutputType.setTransmission(DataTransmissionModeType.REFERENCE);
    executeRequestType.setOutput(Collections.singletonList(dataOutputType));

    Response response = wpsEndpoint.execute(executeRequestType);
    verify(simpleProcess).syncExecute(any(ExecutionRequest.class));
    assertThat(response.getEntity(), is(not(nullValue())));
  }

  @Test
  public void getStatus() throws Exception {
    GetStatus getStatus = new GetStatus();
    getStatus.setJobID("1234");
    StatusInfo statusInfo = wpsEndpoint.getStatus(getStatus);
    assertThat(statusInfo, is(not(nullValue())));
  }

  @Test
  public void getResult() throws Exception {
    GetResult getresult = new GetResult();
    getresult.setJobID("1234");
    Result result = wpsEndpoint.getResult(getresult).readEntity(Result.class);
    assertThat(result, is(not(nullValue())));
  }

  @Test
  public void dismiss() throws Exception {
    Dismiss dismiss = new Dismiss();
    dismiss.setJobID("1234");
    StatusInfo statusInfo = wpsEndpoint.dismiss(dismiss);
    assertThat(statusInfo, is(not(nullValue())));
  }

  @Test
  public void asyncExecuteMissingArgsException() throws Exception {
    thrown.expect(WpsException.class);
    thrown.expectMessage("Too few input items have been specified.");
    ExecuteRequestType executeRequestType = new ExecuteRequestType();
    CodeType ct = new CodeType();
    ct.setValue("test");
    executeRequestType.setIdentifier(ct);
    executeRequestType.setResponse("document");
    executeRequestType.setMode("async");
    executeRequestType.setInput(Collections.emptyList());
    OutputDefinitionType dataOutputType = new OutputDefinitionType();
    dataOutputType.setId(complexDataDesc.getId());
    dataOutputType.setEncoding("UTF-8");
    dataOutputType.setMimeType("text/plain");
    dataOutputType.setSchema("test-schema.txt");
    dataOutputType.setTransmission(DataTransmissionModeType.REFERENCE);
    executeRequestType.setOutput(Collections.singletonList(dataOutputType));

    wpsEndpoint.execute(executeRequestType);
  }

  @Test
  public void asyncExecuteExtraArgsException() throws Exception {
    thrown.expect(WpsException.class);
    thrown.expectMessage(
        "One or more of the input identifiers passed does not match with any of the input identifiers of this process.");
    ExecuteRequestType executeRequestType = new ExecuteRequestType();
    CodeType ct = new CodeType();
    ct.setValue("test");
    executeRequestType.setIdentifier(ct);
    executeRequestType.setResponse("document");
    executeRequestType.setMode("async");
    DataInputType dataInputType = new DataInputType();
    Data data = new Data();
    data.setContent(Collections.singletonList("argument"));
    data.setEncoding("UTF-8");
    data.setMimeType("text/plain");
    data.setSchema("test-schema.txt");
    dataInputType.setData(data);
    dataInputType.setId(literalDataDesc.getId());
    DataInputType extraDataInputType = new DataInputType();
    Data extraData = new Data();
    extraData.setContent(Collections.singletonList("argument"));
    extraData.setEncoding("UTF-8");
    extraData.setMimeType("text/plain");
    extraData.setSchema("test-schema.txt");
    extraDataInputType.setData(data);
    extraDataInputType.setId("extrainput");
    executeRequestType.getInput().add(dataInputType);
    executeRequestType.getInput().add(extraDataInputType);
    OutputDefinitionType dataOutputType = new OutputDefinitionType();
    dataOutputType.setId(complexDataDesc.getId());
    dataOutputType.setEncoding("UTF-8");
    dataOutputType.setMimeType("text/plain");
    dataOutputType.setSchema("test-schema.txt");
    dataOutputType.setTransmission(DataTransmissionModeType.REFERENCE);
    executeRequestType.setOutput(Collections.singletonList(dataOutputType));

    wpsEndpoint.execute(executeRequestType);
  }

  @Test
  public void complexSyncExecuteUsingDefaults() throws Exception {
    when(processRepository.getProcess(anyString())).thenReturn(Optional.of(complexProcess));
    ExecuteRequestType executeRequestType = new ExecuteRequestType();
    CodeType ct = new CodeType();
    ct.setValue("complex");
    executeRequestType.setIdentifier(ct);
    executeRequestType.setResponse("document");
    executeRequestType.setMode("sync");
    DataInputType literalInputType = new DataInputType();
    Data inLiteral = new Data();
    literalInputType.setData(inLiteral);
    literalInputType.setId("inLiteral");
    Data inComplex = new Data();
    DataInputType complexInputType = new DataInputType();
    String inComplexData = "{key:\"value\"}";
    inComplex.setContent(Collections.singletonList(inComplexData));
    complexInputType.setData(inComplex);
    complexInputType.setId("inComplex");
    DataInputType groupInputType = new DataInputType();
    groupInputType.setId("inGroup");
    groupInputType.setInput(Arrays.asList(complexInputType, literalInputType));
    executeRequestType.setInput(Arrays.asList(groupInputType, groupInputType, groupInputType));

    Response response = wpsEndpoint.execute(executeRequestType);
    ExecutionRequest executionRequest = complexProcess.executionRequest;
    DataGroup dataGroup = (DataGroup) executionRequest.getInputData().get(0);

    assertThat(dataGroup.getData(), Matchers.iterableWithSize(2));
    // input order should be preserved
    org.codice.ddf.spatial.process.api.request.Data complexArg = dataGroup.getData().get(0);
    org.codice.ddf.spatial.process.api.request.Data literalArg = dataGroup.getData().get(1);
    assertThat(literalArg, is(notNullValue()));
    assertThat(complexArg, is(notNullValue()));
    assertThat(
        complexProcess.inLiteral.getDataFormats().contains(literalArg.getFormat()), is(true));
    assertThat(((Literal) literalArg).getValue(), is(true));
    assertThat(
        complexProcess.inComplex.getDataFormats().contains(complexArg.getFormat()), is(true));
    assertThat(((Literal) complexArg).getValue(), is(inComplexData));

    List<OutputDefinition> outputDefinitions = executionRequest.getOutputDefinitions();
    assertThat(outputDefinitions, Matchers.iterableWithSize(2));
    OutputDefinition complexOutDef = outputDefinitions.get(0);
    assertThat(complexOutDef.getTransmissionMode(), is(TransmissionMode.VALUE));
    assertThat(complexOutDef.getId(), is("outComplex"));
    assertThat(
        complexProcess.outComplex.getDataFormats().contains(complexOutDef.getFormat()), is(true));
    OutputDefinition literalOutDef = outputDefinitions.get(1);
    assertThat(literalOutDef.getTransmissionMode(), is(TransmissionMode.VALUE));
    assertThat(literalOutDef.getId(), is("outLiteral"));
    assertThat(
        complexProcess.outLiteral.getDataFormats().contains(literalOutDef.getFormat()), is(true));

    List<DataOutputType> output = ((Result) response.getEntity()).getOutput();
    assertThat(output, Matchers.iterableWithSize(2));
  }

  public class ComplexProcess implements Process, ProcessMonitor {

    protected ExecutionRequest executionRequest;

    protected DataDescription inLiteral =
        new LiteralDataDescription(
            "inLiteral", "Literal Data", "This is a test literal input", DataType.BOOLEAN, "true");

    protected DataDescription inComplex =
        new ComplexDataDescription(
                "inComplex",
                "Complex Data",
                "This is a test complex input",
                Collections.singletonList(new DataFormatDefinition("application/json", "UTF-8")))
            .minOccurs(BigInteger.ZERO)
            .maxOccurs(BigInteger.valueOf(2));

    protected DataDescription inGroup =
        new DataDescriptionGroup(
                "inGroup",
                "Input Group",
                "Test Group of Inputs",
                Arrays.asList(inLiteral, inComplex))
            .maxOccurs(BigInteger.TEN);

    protected DataDescription outComplex =
        new LiteralDataDescription(
            "outComplex", "Output Literal", "This is a literal on output", DataType.BASE_64_BINARY);

    protected DataDescription outLiteral =
        new ComplexDataDescription(
            "outLiteral",
            "Output Complex",
            "This is a complex output",
            Collections.singletonList(new DataFormatDefinition("application/json", "UTF-8")));

    protected ExecutionDescription executionDescription =
        new ExecutionDescription(
            Arrays.asList(inGroup),
            Arrays.asList(outComplex, outLiteral),
            new TreeSet<>(Arrays.asList(TransmissionMode.VALUE, TransmissionMode.REFERENCE)));

    protected ProcessResult processResult;

    @Override
    public String getId() {
      return "complex";
    }

    @Override
    public String getDescription() {
      return "Enter description here";
    }

    @Override
    public String getVersion() {
      return "1.0.1-a1";
    }

    @Override
    public String getTitle() {
      return "Complex Process";
    }

    @Override
    public Metadata getMetadata() {

      return new Metadata()
          .description("Metadata for process discovery, not used currently")
          .keywords(Arrays.asList("test", "not-used"));
    }

    @Override
    public Set<Operation> getOperations() {
      return EnumSet.of(
          Operation.ASYNC_EXEC, Operation.SYNC_EXEC, Operation.DISMISS, Operation.STATUS);
    }

    @Override
    public ExecutionDescription getExecutionDescription() {
      return executionDescription;
    }

    @Override
    public ProcessStatus asyncExecute(ExecutionRequest executionRequest) {
      this.executionRequest = executionRequest;
      processResult =
          executionRequest
              .getRequestResultBuilder()
              .add("outLiteral", "{ body:\"test\"}")
              .add("outComplex", new ByteArrayInputStream("this is some bytes".getBytes()))
              .build();
      return new ProcessStatus("test-uuid", Status.COMPLETED);
    }

    /**
     * @param executionRequest
     * @return
     * @throws ProcessException
     */
    @Override
    public ProcessResult syncExecute(ExecutionRequest executionRequest) {
      this.executionRequest = executionRequest;
      processResult =
          executionRequest
              .getRequestResultBuilder()
              .add("outLiteral", "{ body:\"test\"}")
              .add("outComplex", new ByteArrayInputStream("this is some bytes".getBytes()))
              .build();
      return processResult;
    }

    @Override
    public Optional<ProcessMonitor> getProcessMonitor() {
      return Optional.of(this);
    }

    @Override
    public Optional<ProcessStatus> getRequestStatus(String requestId) {
      return Optional.of(new ProcessStatus(requestId, Status.COMPLETED));
    }

    @Override
    public Optional<ProcessStatus> dismissRequest(String requestId) {
      return Optional.of(new ProcessStatus(requestId, Status.CANCELED));
    }

    @Override
    public Optional<ProcessResult> getRequestResult(String requestId) {
      return Optional.of(processResult);
    }
  }
}
