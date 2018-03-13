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
package org.codice.ddf.condpermadmin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.sourceforge.prograde.policy.SecurityActions;
import net.sourceforge.prograde.policyparser.ParsedPermission;
import net.sourceforge.prograde.policyparser.ParsedPolicy;
import net.sourceforge.prograde.policyparser.ParsedPolicyEntry;
import net.sourceforge.prograde.policyparser.ParsedPrincipal;
import net.sourceforge.prograde.policyparser.Parser;
import net.sourceforge.prograde.type.Priority;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.osgi.util.tracker.ServiceTracker;

/** Initializes the CondPermAdmin permission table from all policy files in ddf_home/security */
public class PermissionActivator implements BundleActivator {

  private static final String BUNDLE_NAME_CONDITION =
      "org.codice.ddf.condition.BundleNameCondition";

  private static final String PRINCIPAL_CONDITION = "org.codice.ddf.condition.PrincipalCondition";

  private static final String SIGNER_CONDITION = "org.codice.ddf.condition.SignerCondition";

  private static final String OSGI_CODEBASE = "file:/";

  private static final String PERM_ADMIN_SERVICE_NAME =
      "org.osgi.service.condpermadmin.ConditionalPermissionAdmin";

  private ServiceTracker<ConditionalPermissionAdmin, ConditionalPermissionAdmin> permAdminTracker;

  @Override
  public void start(BundleContext bundleContext) throws Exception {
    System.setProperty("/", File.separator);
    permAdminTracker = new ServiceTracker<>(bundleContext, PERM_ADMIN_SERVICE_NAME, null);
    permAdminTracker.open();

    ConditionalPermissionAdmin conditionalPermissionAdmin = permAdminTracker.getService();
    String policyDir = SecurityActions.getSystemProperty("ddf.home") + File.separator + "security";
    if (policyDir.startsWith("=")) {
      policyDir = policyDir.substring(1);
    }
    File policyDirFile = new File(policyDir);
    List<ParsedPolicy> parsedPolicies = new ArrayList<>();
    for (File file : Objects.requireNonNull(policyDirFile.listFiles())) {
      parsedPolicies.add(new Parser(false).parse(file));
    }
    ConditionalPermissionUpdate conditionalPermissionUpdate =
        conditionalPermissionAdmin.newConditionalPermissionUpdate();
    conditionalPermissionUpdate.getConditionalPermissionInfos().clear();
    Priority priorityResult = null;
    List<ConditionalPermissionInfo> allGrantInfos = new ArrayList<>();
    List<ConditionalPermissionInfo> allDenyInfos = new ArrayList<>();
    for (ParsedPolicy parsedPolicy : parsedPolicies) {
      List<ParsedPolicyEntry> grantEntries = parsedPolicy.getGrantEntries();
      List<ParsedPolicyEntry> denyEntries = parsedPolicy.getDenyEntries();

      buildConditionalPermissionInfo(
          conditionalPermissionAdmin, grantEntries, allGrantInfos, ConditionalPermissionInfo.ALLOW);
      buildConditionalPermissionInfo(
          conditionalPermissionAdmin, denyEntries, allDenyInfos, ConditionalPermissionInfo.DENY);

      Priority priority = parsedPolicy.getPriority();
      if (priorityResult == null) {
        priorityResult = priority;
      } else if (priority != priorityResult) {
        // if they don't match, then we can't make a determination on the priority, so we'll
        // default to deny
        priorityResult = Priority.DENY;
      }
    }

    if (priorityResult == Priority.GRANT) {
      conditionalPermissionUpdate.getConditionalPermissionInfos().addAll(allGrantInfos);
      conditionalPermissionUpdate.getConditionalPermissionInfos().addAll(allDenyInfos);
      conditionalPermissionUpdate
          .getConditionalPermissionInfos()
          .add(getAllPermission(conditionalPermissionAdmin, ConditionalPermissionInfo.ALLOW));
    } else if (priorityResult == Priority.DENY) {
      conditionalPermissionUpdate.getConditionalPermissionInfos().addAll(allDenyInfos);
      conditionalPermissionUpdate.getConditionalPermissionInfos().addAll(allGrantInfos);
      conditionalPermissionUpdate
          .getConditionalPermissionInfos()
          .add(getAllPermission(conditionalPermissionAdmin, ConditionalPermissionInfo.DENY));
    }

    conditionalPermissionUpdate.commit();
  }

  private ConditionalPermissionInfo getAllPermission(
      ConditionalPermissionAdmin conditionalPermissionAdmin, String type) {
    return conditionalPermissionAdmin.newConditionalPermissionInfo(
        null,
        null,
        new PermissionInfo[] {new PermissionInfo("java.security.AllPermission", "", "")},
        type);
  }

  private void buildConditionalPermissionInfo(
      ConditionalPermissionAdmin conditionalPermissionAdmin,
      List<ParsedPolicyEntry> entries,
      List<ConditionalPermissionInfo> infos,
      String type) {
    for (ParsedPolicyEntry parsedPolicyEntry : entries) {
      List<ParsedPermission> permissions = parsedPolicyEntry.getPermissions();
      PermissionInfo[] permissionInfos = new PermissionInfo[permissions.size()];
      int index = 0;
      for (ParsedPermission parsedPermission : permissions) {
        permissionInfos[index++] =
            new PermissionInfo(
                parsedPermission.getPermissionType(),
                replaceSystemProperties(parsedPermission.getPermissionName()),
                parsedPermission.getActions());
      }
      List<ConditionInfo> conditionInfos = new ArrayList<>();
      addCodebase(parsedPolicyEntry, conditionInfos);
      addSignedBy(parsedPolicyEntry, conditionInfos);
      addPrincipals(parsedPolicyEntry, conditionInfos);
      infos.add(
          conditionalPermissionAdmin.newConditionalPermissionInfo(
              null,
              (conditionInfos.isEmpty())
                  ? null
                  : conditionInfos.toArray(new ConditionInfo[conditionInfos.size()]),
              permissionInfos,
              type));
    }
  }

  private void addPrincipals(
      ParsedPolicyEntry parsedPolicyEntry, List<ConditionInfo> conditionInfos) {
    List<ParsedPrincipal> parsedPrincipals = parsedPolicyEntry.getPrincipals();
    List<String> principals = new ArrayList<>();
    for (ParsedPrincipal parsedPrincipal : parsedPrincipals) {
      if (parsedPrincipal.hasAlias()) {
        principals.add(parsedPrincipal.getAlias());
      } else {
        principals.add(
            parsedPrincipal.getPrincipalClass() + "/" + parsedPrincipal.getPrincipalName());
      }
    }

    if (!principals.isEmpty()) {
      conditionInfos.add(
          new ConditionInfo(
              PRINCIPAL_CONDITION, principals.toArray(new String[principals.size()])));
    }
  }

  private void addSignedBy(
      ParsedPolicyEntry parsedPolicyEntry, List<ConditionInfo> conditionInfos) {
    String signedBy = parsedPolicyEntry.getSignedBy();
    if (signedBy != null) {
      conditionInfos.add(new ConditionInfo(SIGNER_CONDITION, new String[] {signedBy}));
    }
  }

  private void addCodebase(
      ParsedPolicyEntry parsedPolicyEntry, List<ConditionInfo> conditionInfos) {
    String codebase = parsedPolicyEntry.getCodebase();
    if (codebase != null) {
      conditionInfos.add(
          new ConditionInfo(
              BUNDLE_NAME_CONDITION,
              new String[] {replaceSystemProperties(getBundleName(codebase))}));
    } else {
      conditionInfos.add(new ConditionInfo(BUNDLE_NAME_CONDITION, new String[] {""}));
    }
  }

  private String getBundleName(String osgiCodebase) {
    return osgiCodebase.replace(OSGI_CODEBASE, "");
  }

  private String replaceSystemProperties(String string) {
    return StrSubstitutor.replaceSystemProperties(string);
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    permAdminTracker.close();
  }
}
