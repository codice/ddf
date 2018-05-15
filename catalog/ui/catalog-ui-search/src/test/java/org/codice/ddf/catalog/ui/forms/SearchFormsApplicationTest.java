package org.codice.ddf.catalog.ui.forms;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;
import static spark.Spark.awaitInitialization;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.security.SubjectUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.catalog.ui.forms.model.TemplateTransformer;
import org.codice.ddf.catalog.ui.security.ShareableMetacardImpl;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import spark.Request;
import spark.Spark;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class, SubjectUtils.class, SearchFormsApplication.class})
@PowerMockIgnore("javax.net.ssl.*")
public class SearchFormsApplicationTest {

  private static final String TEST_HOST = "http://localhost";

  private static final int TEST_PORT = 4567;

  // Allotted time to kill the Spark Servlet (milliseconds)
  private static final int TIME_TO_KILL_SPARK = 100;

  private static final String SPARK_TEST_SERVER = TEST_HOST + ":" + TEST_PORT;

  private static final String TEST_EMAIL_1 = "dummyUser@gmail.com";

  private static final String TEST_EMAIL_2 = "dummyUser2@gmail.com";

  private static final String TEST_EMAIL_3 = "dummyUser3@gmail.com";

  private static final String DELETE_REQ = "delete";

  private static final String POST_REQ = "post";

  private static final String GET_REQ = "get";

  CatalogFramework catalogFramework;

  TemplateTransformer templateTransformer;

  EndpointUtil endpointUtil;

  SearchFormsApplication sfa;

  CloseableHttpClient httpClient;

  List<String> testRoles = new ArrayList<>();

  DeleteResponse deleteResponse = mock(DeleteResponse.class, Mockito.RETURNS_DEEP_STUBS);

  Set<ProcessingDetails> mockedSet = mock(HashSet.class);

  HashSet<String> mockedRoleSet = new HashSet<>();

  HashSet<String> mockedIndividualSet = new HashSet<>();

  @org.mockito.Mock private Subject currentSubject;

  @org.mockito.Mock PrincipalCollection principalCollection;

  @org.mockito.Mock HttpServletRequest servletRequest;

  @org.mockito.Mock HttpSession httpSession;

  @org.mockito.Mock Request request;

  @org.mockito.Mock ShareableMetacardImpl shareableMetacard;

  @Before
  public void setup() throws Exception {

    // Mock classes
    catalogFramework = mock(CatalogFramework.class);
    templateTransformer = mock(TemplateTransformer.class);
    endpointUtil = mock(EndpointUtil.class);
    httpClient = HttpClients.custom().build();

    // Mock static classes/methods
    mockStatic(SecurityUtils.class);
    mockStatic(SubjectUtils.class);

    // Mock some sample roles
    testRoles.add("admin");
    testRoles.add("viewer");
    testRoles.add("guest");

    // Mock requests
    when(catalogFramework.delete(any(DeleteRequest.class))).thenReturn(deleteResponse);
    when(deleteResponse.getProcessingErrors()).thenReturn(mockedSet);

    // Initialize the Spark Server inside SFA controller
    sfa = spy(new SearchFormsApplication(catalogFramework, templateTransformer, endpointUtil));
    sfa.init();
    awaitInitialization();
  }

  @After
  public void tearDown() throws Exception {
    Spark.stop();
    Thread.sleep(TIME_TO_KILL_SPARK);
  }

  @Test
  public void testSuccessfulTemplateDeletion() throws Exception {
    when(sfa.getRequesterEmail()).thenReturn(TEST_EMAIL_1);
    doReturn(
            new ImmutableMap.Builder<String, Object>()
                .put(Core.METACARD_OWNER, TEST_EMAIL_1)
                .build())
        .when(sfa, "getOriginalMetacardOwner", any(Request.class));
    when(deleteResponse.getProcessingErrors().isEmpty()).thenReturn(true);
    ImmutableMap<String, Object> responseData = makeRequestUtility(DELETE_REQ, "/forms/5");
    assertEquals(new Integer(200), (Integer) responseData.get("status"));
    assertThat(
        responseData.get("response").toString(), containsString("Message=Successfully deleted."));
  }

  @Test
  public void testFailedTemplateDeletion() throws Exception {
    when(sfa.getRequesterEmail()).thenReturn(TEST_EMAIL_1);
    doReturn(
            new ImmutableMap.Builder<String, Object>()
                .put(Core.METACARD_OWNER, TEST_EMAIL_1)
                .build())
        .when(sfa, "getOriginalMetacardOwner", any(Request.class));
    when(deleteResponse.getProcessingErrors().isEmpty()).thenReturn(false);
    ImmutableMap<String, Object> responseData = makeRequestUtility(DELETE_REQ, "/forms/5");
    assertEquals(new Integer(500), (Integer) responseData.get("status"));
    assertThat(
        responseData.get("response").toString(), containsString("Message=Failed to delete."));
  }

  @Test
  public void testGetRequesterGroupsOnSubject() {
    stub(method(SubjectUtils.class, "getAttribute", Subject.class, String.class))
        .toReturn(testRoles);
    assertEquals(testRoles, sfa.getRequesterGroups());
  }

  @Test
  public void testRetrievalOfOriginalMetacardOwner() throws IOException {
    String jsonMap = "{\"metacard.owner\":" + TEST_EMAIL_2 + "}";
    doReturn(jsonMap).when(endpointUtil).safeGetBody(request);
    assertEquals(TEST_EMAIL_2, sfa.getOriginalMetacardOwner(request).get(Core.METACARD_OWNER));
  }

  @Test
  public void testRetrievalOfRequesterEmail() {
    when(SecurityUtils.getSubject()).thenReturn(currentSubject);
    stub(method(SubjectUtils.class, "getEmailAddress", Subject.class)).toReturn(TEST_EMAIL_3);
    assertEquals(TEST_EMAIL_3, sfa.getRequesterEmail());
  }

  @Test
  public void testListIntersectionByRoleIsEmpty() {
    HashSet<String> testRoleSet = new HashSet<>();
    testRoleSet.add("viewer");
    when(sfa.getRequesterGroups()).thenReturn(testRoles);
    when(shareableMetacard.getAccessGroups()).thenReturn(testRoleSet);
    assertTrue(sfa.sharedByGroup().test(shareableMetacard));
  }

  @Test
  public void testListIntersectionByRoleIsNotEmpty() {
    when(sfa.getRequesterGroups()).thenReturn(testRoles);
    when(shareableMetacard.getAccessGroups()).thenReturn(mockedRoleSet);
    assertFalse(sfa.sharedByGroup().test(shareableMetacard));
  }

  @Test
  public void testSubjectExistsWithinMetacardAccessIndividuals() {
    HashSet<String> testIndividualSet = new HashSet<>();
    testIndividualSet.add(TEST_EMAIL_2);
    when(sfa.getRequesterEmail()).thenReturn(TEST_EMAIL_2);
    when(shareableMetacard.getAccessIndividuals()).thenReturn(testIndividualSet);
    assertTrue(sfa.sharedByIndividual().test(shareableMetacard));
  }

  @Test
  public void testSubjectDoesNotExistsWithinMetacardAccessIndividuals() {
    when(sfa.getRequesterEmail()).thenReturn(TEST_EMAIL_2);
    when(shareableMetacard.getAccessIndividuals()).thenReturn(mockedIndividualSet);
    assertFalse(sfa.sharedByIndividual().test(shareableMetacard));
  }

  /** Request utility helper method */
  private ImmutableMap<String, Object> makeRequestUtility(String type, String endpoint)
      throws IOException {

    HttpRequestBase req;
    endpoint = SPARK_TEST_SERVER + endpoint;

    switch (type) {
      case GET_REQ:
        req = new HttpGet(endpoint);
        break;
      case POST_REQ:
        req = new HttpPost(endpoint);
        break;
      case DELETE_REQ:
        req = new HttpDelete(endpoint);
        break;
      default:
        throw new UnsupportedOperationException("Request Method Unsupported!");
    }

    CloseableHttpResponse response = httpClient.execute(req);

    int statusCode = response.getStatusLine().getStatusCode();
    BufferedReader rd =
        new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

    StringBuffer result = new StringBuffer();
    String line = "";
    while ((line = rd.readLine()) != null) {
      result.append(line);
    }

    return new ImmutableMap.Builder<String, Object>()
        .put("response", result)
        .put("status", statusCode)
        .build();
  }
}
