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
package ddf.mime.mapper;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.mime.MimeTypeToTransformerMapper;
import java.util.Arrays;
import java.util.List;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.ws.rs.core.MediaType;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class MimeTypeToTransformerMapperImplTest {

  /**
   * We expect an empty services list to be returned when no bundleContext is provided
   *
   * @throws MimeTypeParseException
   */
  @Test
  public void testNullBundleContext() throws MimeTypeParseException {

    // given
    final BundleContext context = null;

    // when
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches = matcher.findMatches(Object.class, null);

    // then
    assertThat(matches.isEmpty(), is(true));
  }

  /**
   * We expect an empty services list to be returned when no services have been registered in the
   * service registry
   *
   * @throws MimeTypeParseException
   * @throws InvalidSyntaxException
   */
  @Test
  public void testEmptyServiceList() throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);
    ServiceReference[] refs = {};

    // when
    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches =
        matcher.findMatches(Object.class, new MimeType(MediaType.APPLICATION_ATOM_XML));

    // then
    assertThat(matches.isEmpty(), is(true));
  }

  /**
   * We expect to receive all the services to be returned when the user does not provide any mime
   * types
   *
   * @throws MimeTypeParseException
   * @throws InvalidSyntaxException
   */
  @Test
  public void testNullMimeType() throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);

    ServiceReference ref1 = mock(ServiceReference.class);
    ServiceReference ref2 = mock(ServiceReference.class);
    ServiceReference ref3 = mock(ServiceReference.class);

    when(ref1.getProperty(Constants.SERVICE_RANKING)).thenReturn(1);
    when(ref2.getProperty(Constants.SERVICE_RANKING)).thenReturn(2);
    when(ref3.getProperty(Constants.SERVICE_RANKING)).thenReturn(3);

    when(ref1.compareTo(ref2)).thenReturn(-1);
    when(ref1.compareTo(ref3)).thenReturn(-1);
    when(ref2.compareTo(ref1)).thenReturn(1);
    when(ref2.compareTo(ref3)).thenReturn(-1);
    when(ref3.compareTo(ref1)).thenReturn(1);
    when(ref3.compareTo(ref2)).thenReturn(1);

    /*
     * Add the three references out of order.
     */
    ServiceReference[] refs = {ref2, ref3, ref1};

    Object simpleTransformer1 = new Object();
    Object simpleTransformer2 = new Object();
    Object simpleTransformer3 = new Object();

    // when
    when(context.getService(ref1)).thenReturn(simpleTransformer1);
    when(context.getService(ref2)).thenReturn(simpleTransformer2);
    when(context.getService(ref3)).thenReturn(simpleTransformer3);

    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches = matcher.findMatches(Object.class, null);

    // then
    assertThat(matches.size(), is(3));
    /*
     * Test the sorted order of the references.
     */
    assertThat(matches.get(0), is(simpleTransformer3));
    assertThat(matches.get(1), is(simpleTransformer2));
    assertThat(matches.get(2), is(simpleTransformer1));
  }

  /**
   * We expect to receive all the services to be returned which is in this case an empty list.
   *
   * @throws MimeTypeParseException
   * @throws InvalidSyntaxException
   */
  @Test
  public void testNullMimeTypeWithEmptyServiceList()
      throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);
    ServiceReference[] refs = {};

    // when
    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches = matcher.findMatches(Object.class, null);

    // then
    assertThat(matches.isEmpty(), is(true));
  }

  /**
   * Testing a negative case where the mimetypes don't match.
   *
   * <p>Tests if
   *
   * <p>InputTransformer Registered: <br>
   * {BaseType1, BaseType2}. <br>
   * <br>
   * User MimeType Provided: <br>
   * {BasetType3} <br>
   * <br>
   * Empty Set should be returned.
   *
   * @throws MimeTypeParseException
   * @throws InvalidSyntaxException
   */
  @Test
  public void testNoMatch() throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);
    ServiceReference ref1 =
        createMockReference(1, Arrays.asList(MediaType.APPLICATION_ATOM_XML), null);
    ServiceReference ref2 = createMockReference(2, Arrays.asList(MediaType.APPLICATION_JSON), null);

    ServiceReference[] refs = {ref1, ref2};

    Object simpleTransformer1 = new Object();
    Object simpleTransformer2 = new Object();

    // when
    when(context.getService(ref1)).thenReturn(simpleTransformer1);
    when(context.getService(ref2)).thenReturn(simpleTransformer2);

    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches =
        matcher.findMatches(Object.class, new MimeType(MediaType.APPLICATION_XML));

    // then
    assertThat(matches.size(), is(0));
  }

  /**
   * Testing a negative case where the mimetypes don't match.
   *
   * <p>Tests if
   *
   * <p>InputTransformer Registered: <br>
   * {BaseType1 + Id1}. <br>
   * <br>
   * User MimeType Provided: <br>
   * {BaseType2} <br>
   * <br>
   * Empty Set should be returned.
   *
   * @throws MimeTypeParseException
   * @throws InvalidSyntaxException
   */
  @Test
  public void testNoMatchInputTransformerBaseTypeAndId()
      throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);

    ServiceReference ref1 =
        createMockReference(1, Arrays.asList(MediaType.APPLICATION_ATOM_XML), "a1");
    ServiceReference[] refs = {ref1};

    Object simpleTransformer1 = new Object();

    // when
    when(context.getService(ref1)).thenReturn(simpleTransformer1);

    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches =
        matcher.findMatches(Object.class, new MimeType(MediaType.APPLICATION_XML));

    // then
    assertThat(matches.size(), is(0));
  }

  /**
   * Testing a negative case where the mimetype ids don't match.
   *
   * <p>Tests if
   *
   * <p>InputTransformer Registered: <br>
   * {BaseType1+id1}. <br>
   * <br>
   * User MimeType Provided: <br>
   * {BaseType1} <br>
   * <br>
   * Empty Set should be returned.
   *
   * @throws MimeTypeParseException
   * @throws InvalidSyntaxException
   */
  @Test
  public void testBaseTypeMatchIdMismatch() throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);

    ServiceReference ref1 =
        createMockReference(1, Arrays.asList(MediaType.APPLICATION_ATOM_XML), "a1");
    ServiceReference[] refs = {ref1};

    Object simpleTransformer1 = new Object();

    // when
    when(context.getService(ref1)).thenReturn(simpleTransformer1);

    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches =
        matcher.findMatches(Object.class, new MimeType(MediaType.APPLICATION_ATOM_XML + "; id=a2"));

    // then
    assertThat(matches.size(), is(0));
  }

  /**
   * Tests if a basetype matches that the list will return the correct sorted list.
   *
   * @throws MimeTypeParseException
   * @throws InvalidSyntaxException
   */
  @Test
  public void testOnlyBaseTypeMatch() throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);

    ServiceReference ref1 =
        createMockReference(1, Arrays.asList(MediaType.APPLICATION_ATOM_XML), "a1");
    ServiceReference ref2 = createMockReference(2, Arrays.asList(MediaType.APPLICATION_JSON), "a2");
    ServiceReference ref3 = createMockReference(3, null, null);
    ServiceReference[] refs = {ref2, ref3, ref1};

    Object simpleTransformer1 = new Object();
    Object simpleTransformer2 = new Object();
    Object simpleTransformer3 = new Object();

    // when
    when(context.getService(ref1)).thenReturn(simpleTransformer1);
    when(context.getService(ref2)).thenReturn(simpleTransformer2);
    when(context.getService(ref3)).thenReturn(simpleTransformer3);

    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches =
        matcher.findMatches(Object.class, new MimeType(MediaType.APPLICATION_JSON));

    // then
    assertThat(matches.size(), is(1));
    assertThat(matches.get(0), is(simpleTransformer2));
  }

  /**
   * Tests if a basetype matches that the list will return the correct sorted list.
   *
   * @throws MimeTypeParseException
   * @throws InvalidSyntaxException
   */
  @Test
  public void testOnlyBaseTypeMatch2() throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);

    ServiceReference ref1 =
        createMockReference(1, Arrays.asList(MediaType.APPLICATION_ATOM_XML), "a1");
    ServiceReference ref2 = createMockReference(2, Arrays.asList(MediaType.APPLICATION_JSON), "a2");
    ServiceReference ref3 = createMockReference(3, Arrays.asList(MediaType.APPLICATION_JSON), "a3");
    ServiceReference[] refs = {ref2, ref3, ref1};

    Object simpleTransformer1 = new Object();
    Object simpleTransformer2 = new Object();
    Object simpleTransformer3 = new Object();

    // when
    when(context.getService(ref1)).thenReturn(simpleTransformer1);
    when(context.getService(ref2)).thenReturn(simpleTransformer2);
    when(context.getService(ref3)).thenReturn(simpleTransformer3);
    when(ref1.compareTo(ref2)).thenReturn(-1);
    when(ref1.compareTo(ref3)).thenReturn(-1);
    when(ref2.compareTo(ref1)).thenReturn(1);
    when(ref2.compareTo(ref3)).thenReturn(-1);
    when(ref3.compareTo(ref1)).thenReturn(1);
    when(ref3.compareTo(ref2)).thenReturn(1);

    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches =
        matcher.findMatches(Object.class, new MimeType(MediaType.APPLICATION_JSON));

    // then
    assertThat(matches.size(), is(2));
    assertThat(matches.get(0), is(simpleTransformer3));
    assertThat(matches.get(1), is(simpleTransformer2));
  }

  /**
   * Tests if a single id match will return only one item
   *
   * @throws MimeTypeParseException
   * @throws InvalidSyntaxException
   */
  @Test
  public void testSingleIdMatch() throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);

    ServiceReference ref1 = createMockReference(1, Arrays.asList(MediaType.APPLICATION_JSON), "");
    ServiceReference ref2 =
        createMockReference(
            2, Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON), "a1");
    ServiceReference ref3 = createMockReference(3, null, null);
    ServiceReference[] refs = {ref2, ref3, ref1};

    Object simpleTransformer1 = new Object();
    Object simpleTransformer2 = new Object();
    Object simpleTransformer3 = new Object();

    // when
    when(context.getService(ref1)).thenReturn(simpleTransformer1);
    when(context.getService(ref2)).thenReturn(simpleTransformer2);
    when(context.getService(ref3)).thenReturn(simpleTransformer3);

    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches =
        matcher.findMatches(Object.class, new MimeType(MediaType.APPLICATION_JSON + "; id=a1"));

    // then
    assertThat(matches.size(), is(1));
    assertThat(matches.get(0), is(simpleTransformer2));
  }

  @Test
  public void testInvalidMimeTypeServiceProperty()
      throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);

    ServiceReference ref1 = createMockReference(1, Arrays.asList("!INVALID@!"), null);
    ServiceReference[] refs = {ref1};

    Object simpleTransformer1 = new Object();

    // when
    when(context.getService(ref1)).thenReturn(simpleTransformer1);
    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches =
        matcher.findMatches(Object.class, new MimeType(MediaType.APPLICATION_JSON + "; id=a1"));

    // then
    assertThat(matches.size(), is(0));
  }

  /**
   * Tests the case where the ServiceReference Properties does not have a list of MimeTypes, instead
   * it provides a single String for the MimeType
   *
   * @throws MimeTypeParseException
   * @throws InvalidSyntaxException
   */
  @Test
  public void testSingleMimeTypeServiceProperty()
      throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);

    ServiceReference ref = mock(ServiceReference.class);
    ServiceReference[] refs = {ref};

    when(ref.getProperty(Constants.SERVICE_RANKING)).thenReturn(0);
    when(ref.getProperty(MimeTypeToTransformerMapper.MIME_TYPE_KEY))
        .thenReturn(MediaType.APPLICATION_JSON);

    Object simpleTransformer1 = new Object();

    // when
    when(context.getService(ref)).thenReturn(simpleTransformer1);

    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches =
        matcher.findMatches(Object.class, new MimeType(MediaType.APPLICATION_JSON));

    // then
    assertThat(matches.size(), is(1));
  }

  @Test
  public void testInvalidMimeTypeServicePropertyWithMatch()
      throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);

    ServiceReference ref1 = createMockReference(1, Arrays.asList("!INVALID!"), null);
    ServiceReference ref2 =
        createMockReference(
            2, Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON), "a1");
    ServiceReference[] refs = {ref2, ref1};

    Object simpleTransformer1 = new Object();
    Object simpleTransformer2 = new Object();

    // when
    when(context.getService(ref1)).thenReturn(simpleTransformer1);
    when(context.getService(ref2)).thenReturn(simpleTransformer2);

    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches =
        matcher.findMatches(Object.class, new MimeType(MediaType.APPLICATION_JSON + "; id=a1"));

    // then
    assertThat(matches.size(), is(1));
    assertThat(matches.get(0), is(simpleTransformer2));
  }

  /**
   * Tests if a multiple id match will return only one item
   *
   * @throws MimeTypeParseException
   * @throws InvalidSyntaxException
   */
  @Test
  public void testMultiIdMatchExtraParameters()
      throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);

    ServiceReference ref1 = createMockReference(1, Arrays.asList(MediaType.APPLICATION_JSON), "a1");
    ServiceReference ref2 =
        createMockReference(
            2, Arrays.asList(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON), "a1");
    ServiceReference ref3 = createMockReference(3, null, null);
    ServiceReference[] refs = {ref3, ref2, ref1};

    //        InputTransformer simpleTransformer1 = getSimpleTransformer("1");
    //        InputTransformer simpleTransformer2 = getSimpleTransformer("2");
    //        InputTransformer simpleTransformer3 = getSimpleTransformer("3");
    Object simpleTransformer1 = new Object();
    Object simpleTransformer2 = new Object();
    Object simpleTransformer3 = new Object();

    // when
    when(context.getService(ref1)).thenReturn(simpleTransformer1);
    when(context.getService(ref2)).thenReturn(simpleTransformer2);
    when(context.getService(ref3)).thenReturn(simpleTransformer3);
    when(ref1.compareTo(ref3)).thenReturn(-1);
    when(ref2.compareTo(ref1)).thenReturn(1);
    when(ref2.compareTo(ref3)).thenReturn(-1);
    when(ref3.compareTo(ref1)).thenReturn(1);
    when(ref3.compareTo(ref2)).thenReturn(1);

    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches =
        matcher.findMatches(
            Object.class, new MimeType(MediaType.APPLICATION_JSON + "; id=a1;charset=UTF-8"));

    // then
    assertThat(matches.size(), is(2));
    assertThat(matches.get(0), is(simpleTransformer2));
    assertThat(matches.get(1), is(simpleTransformer1));
  }

  /**
   * We expect a null services list to be returned when no services have been registered in the
   * service registry
   *
   * @throws MimeTypeParseException
   * @throws InvalidSyntaxException
   */
  @Test
  public void testNullServiceList() throws MimeTypeParseException, InvalidSyntaxException {

    // given
    final BundleContext context = mock(BundleContext.class);
    ServiceReference[] refs = null;

    // when
    when(context.getServiceReferences(isA(String.class), isNull(String.class))).thenReturn(refs);
    MimeTypeToTransformerMapper matcher =
        new MimeTypeToTransformerMapperImpl() {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };
    List<Object> matches =
        matcher.findMatches(Object.class, new MimeType(MediaType.APPLICATION_ATOM_XML));

    // then
    assertThat(matches.isEmpty(), is(true));
  }

  private ServiceReference createMockReference(int i, List<String> mimeTypesSupported, String id) {

    ServiceReference ref = mock(ServiceReference.class);

    when(ref.getProperty(Constants.SERVICE_RANKING)).thenReturn(i);
    when(ref.getProperty(MimeTypeToTransformerMapper.MIME_TYPE_KEY)).thenReturn(mimeTypesSupported);
    when(ref.getProperty(MimeTypeToTransformerMapper.ID_KEY)).thenReturn(id);

    return ref;
  }
}
