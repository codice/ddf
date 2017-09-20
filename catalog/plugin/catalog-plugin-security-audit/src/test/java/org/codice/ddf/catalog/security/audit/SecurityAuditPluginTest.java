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
package org.codice.ddf.catalog.security.audit;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.common.audit.SecurityLogger;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codice.ddf.catalog.plugin.security.audit.SecurityAuditPlugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({SecurityLogger.class})
public class SecurityAuditPluginTest {

  @Rule public PowerMockRule rule = new PowerMockRule();

  private SecurityAuditPlugin securityAuditPlugin;

  private String metacardKey = "metacardKey";

  private String auditMessageFormat =
      "Attribute %s on metacard %s with value(s) %s was updated to value(s) %s";

  @Before
  public void setup() {
    List<String> auditAttributes = new ArrayList<>();
    auditAttributes.add(Metacard.TITLE);
    securityAuditPlugin = new SecurityAuditPlugin();
    securityAuditPlugin.setAuditAttributes(auditAttributes);
  }

  @Test
  public void testChangedAttributeInAuditConfigIsAudited() throws StopProcessingException {
    PowerMockito.mockStatic(SecurityLogger.class);

    MetacardImpl existingMetacard = new MetacardImpl();
    MetacardImpl updateMetacard = new MetacardImpl();

    existingMetacard.setAttribute(Metacard.TITLE, "A");
    updateMetacard.setAttribute(Metacard.TITLE, "1");
    existingMetacard.setAttribute(Metacard.ID, "test");
    updateMetacard.setAttribute(Metacard.ID, "test");

    List<Map.Entry<Serializable, Metacard>> updateMetacards = new ArrayList<>();
    updateMetacards.add(
        new AbstractMap.SimpleEntry<Serializable, Metacard>(metacardKey, updateMetacard));

    Map<String, Metacard> existingMetacards = new HashMap<>();
    existingMetacards.put(metacardKey, existingMetacard);

    UpdateRequestImpl updateRequest = new UpdateRequestImpl(updateMetacards, Metacard.TITLE, null);
    securityAuditPlugin.processPreUpdate(updateRequest, existingMetacards);

    PowerMockito.verifyStatic(times(1));
    SecurityLogger.audit(String.format(auditMessageFormat, Metacard.TITLE, "test", "A", "1"));
  }

  @Test
  public void testUnchangedAttributeInAuditConfigIsNotAudited() throws StopProcessingException {
    PowerMockito.mockStatic(SecurityLogger.class);

    MetacardImpl existingMetacard = new MetacardImpl();
    MetacardImpl updateMetacard = new MetacardImpl();

    existingMetacard.setAttribute(Metacard.TITLE, "B");
    updateMetacard.setAttribute(Metacard.TITLE, "B");
    existingMetacard.setAttribute(Metacard.ID, "test");
    updateMetacard.setAttribute(Metacard.ID, "test");

    List<Map.Entry<Serializable, Metacard>> updateMetacards = new ArrayList<>();
    updateMetacards.add(
        new AbstractMap.SimpleEntry<Serializable, Metacard>(metacardKey, updateMetacard));

    Map<String, Metacard> existingMetacards = new HashMap<>();
    existingMetacards.put(metacardKey, existingMetacard);

    UpdateRequestImpl updateRequest = new UpdateRequestImpl(updateMetacards, Metacard.TITLE, null);
    securityAuditPlugin.processPreUpdate(updateRequest, existingMetacards);

    PowerMockito.verifyStatic(never());
    SecurityLogger.audit(String.format(auditMessageFormat, Metacard.TITLE, "test", "B", "B"));
  }

  @Test
  public void testChangedAttributeNotInAuditConfigIsNotAudited() throws StopProcessingException {
    PowerMockito.mockStatic(SecurityLogger.class);

    MetacardImpl existingMetacard = new MetacardImpl();
    MetacardImpl updateMetacard = new MetacardImpl();

    existingMetacard.setAttribute(Metacard.METADATA, "A test string");
    updateMetacard.setAttribute(Metacard.METADATA, "A different test string");
    existingMetacard.setAttribute(Metacard.ID, "test");
    updateMetacard.setAttribute(Metacard.ID, "test");

    List<Map.Entry<Serializable, Metacard>> updateMetacards = new ArrayList<>();
    updateMetacards.add(
        new AbstractMap.SimpleEntry<Serializable, Metacard>(metacardKey, updateMetacard));

    Map<String, Metacard> existingMetacards = new HashMap<>();
    existingMetacards.put(metacardKey, existingMetacard);

    UpdateRequestImpl updateRequest =
        new UpdateRequestImpl(updateMetacards, Metacard.METADATA, null);
    securityAuditPlugin.processPreUpdate(updateRequest, existingMetacards);

    PowerMockito.verifyStatic(never());
    SecurityLogger.audit(
        String.format(
            auditMessageFormat,
            Metacard.METADATA,
            "test",
            "A test string",
            "A different test string"));
  }

  @Test
  public void testUnchangedAttributeNotInAuditConfigIsNotAudited() throws StopProcessingException {
    PowerMockito.mockStatic(SecurityLogger.class);

    MetacardImpl existingMetacard = new MetacardImpl();
    MetacardImpl updateMetacard = new MetacardImpl();

    existingMetacard.setAttribute(Metacard.METADATA, "A test string");
    updateMetacard.setAttribute(Metacard.METADATA, "A test string");
    existingMetacard.setAttribute(Metacard.ID, "test");
    updateMetacard.setAttribute(Metacard.ID, "test");

    List<Map.Entry<Serializable, Metacard>> updateMetacards = new ArrayList<>();
    updateMetacards.add(
        new AbstractMap.SimpleEntry<Serializable, Metacard>(metacardKey, updateMetacard));

    Map<String, Metacard> existingMetacards = new HashMap<>();
    existingMetacards.put(metacardKey, existingMetacard);

    UpdateRequestImpl updateRequest =
        new UpdateRequestImpl(updateMetacards, Metacard.METADATA, null);
    securityAuditPlugin.processPreUpdate(updateRequest, existingMetacards);

    PowerMockito.verifyStatic(never());
    SecurityLogger.audit(
        String.format(
            auditMessageFormat, Metacard.METADATA, "test", "A test string", "A test string"));
  }

  @Test
  public void testDeletedAttributeInAuditConfigIsAudited() throws StopProcessingException {
    PowerMockito.mockStatic(SecurityLogger.class);

    MetacardImpl existingMetacard = new MetacardImpl();
    MetacardImpl updateMetacard = new MetacardImpl();

    existingMetacard.setAttribute(Metacard.TITLE, "A test string");
    existingMetacard.setAttribute(Metacard.ID, "test");
    updateMetacard.setAttribute(Metacard.ID, "test");

    List<Map.Entry<Serializable, Metacard>> updateMetacards = new ArrayList<>();
    updateMetacards.add(
        new AbstractMap.SimpleEntry<Serializable, Metacard>(metacardKey, updateMetacard));

    Map<String, Metacard> existingMetacards = new HashMap<>();
    existingMetacards.put(metacardKey, existingMetacard);

    UpdateRequestImpl updateRequest = new UpdateRequestImpl(updateMetacards, Metacard.TITLE, null);
    securityAuditPlugin.processPreUpdate(updateRequest, existingMetacards);

    PowerMockito.verifyStatic(times(1));
    SecurityLogger.audit(
        String.format(auditMessageFormat, Metacard.TITLE, "test", "A test string", "[NO VALUE]"));
  }

  @Test
  public void testAddedAttributeInAuditConfigIsAudited() throws StopProcessingException {
    PowerMockito.mockStatic(SecurityLogger.class);

    MetacardImpl existingMetacard = new MetacardImpl();
    MetacardImpl updateMetacard = new MetacardImpl();

    updateMetacard.setAttribute(Metacard.TITLE, "A test string");
    existingMetacard.setAttribute(Metacard.ID, "test");
    updateMetacard.setAttribute(Metacard.ID, "test");

    List<Map.Entry<Serializable, Metacard>> updateMetacards = new ArrayList<>();
    updateMetacards.add(
        new AbstractMap.SimpleEntry<Serializable, Metacard>(metacardKey, updateMetacard));

    Map<String, Metacard> existingMetacards = new HashMap<>();
    existingMetacards.put(metacardKey, existingMetacard);

    UpdateRequestImpl updateRequest = new UpdateRequestImpl(updateMetacards, Metacard.TITLE, null);
    securityAuditPlugin.processPreUpdate(updateRequest, existingMetacards);

    PowerMockito.verifyStatic(times(1));
    SecurityLogger.audit(
        String.format(auditMessageFormat, Metacard.TITLE, "test", "[NO VALUE]", "A test string"));
  }

  @Test
  public void testNullAttributeInAuditConfigIsNotAudited() throws StopProcessingException {
    PowerMockito.mockStatic(SecurityLogger.class);

    MetacardImpl existingMetacard = new MetacardImpl();
    MetacardImpl updateMetacard = new MetacardImpl();

    existingMetacard.setAttribute(Metacard.ID, "test");
    updateMetacard.setAttribute(Metacard.ID, "test");

    List<Map.Entry<Serializable, Metacard>> updateMetacards = new ArrayList<>();
    updateMetacards.add(
        new AbstractMap.SimpleEntry<Serializable, Metacard>(metacardKey, updateMetacard));

    Map<String, Metacard> existingMetacards = new HashMap<>();
    existingMetacards.put(metacardKey, existingMetacard);

    UpdateRequestImpl updateRequest = new UpdateRequestImpl(updateMetacards, Metacard.TITLE, null);
    securityAuditPlugin.processPreUpdate(updateRequest, existingMetacards);

    PowerMockito.verifyStatic(never());
    SecurityLogger.audit(anyString());
  }

  @Test
  public void testNullupdateRequest() throws StopProcessingException {
    PowerMockito.mockStatic(SecurityLogger.class);

    securityAuditPlugin.processPreUpdate(null, null);

    PowerMockito.verifyStatic(never());
    SecurityLogger.audit(anyString());
  }

  @Test
  public void testNotLocalRequest() throws StopProcessingException {
    PowerMockito.mockStatic(SecurityLogger.class);

    MetacardImpl existingMetacard = new MetacardImpl();
    MetacardImpl updateMetacard = new MetacardImpl();

    existingMetacard.setAttribute(Metacard.ID, "test");
    updateMetacard.setAttribute(Metacard.ID, "test");

    List<Map.Entry<Serializable, Metacard>> updateMetacards = new ArrayList<>();
    updateMetacards.add(
        new AbstractMap.SimpleEntry<Serializable, Metacard>(metacardKey, updateMetacard));

    Map<String, Metacard> existingMetacards = new HashMap<>();
    existingMetacards.put(metacardKey, existingMetacard);

    Map<String, Serializable> props = new HashMap<>();
    props.put(Constants.LOCAL_DESTINATION_KEY, false);

    UpdateRequestImpl updateRequest = new UpdateRequestImpl(updateMetacards, Metacard.TITLE, props);

    securityAuditPlugin.processPreUpdate(updateRequest, existingMetacards);

    PowerMockito.verifyStatic(never());
    SecurityLogger.audit(anyString());
  }
}
