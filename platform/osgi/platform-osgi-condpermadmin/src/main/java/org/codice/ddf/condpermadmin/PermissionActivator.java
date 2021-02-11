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

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.regex.Pattern;
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

  private static final Pattern REGEX = Pattern.compile("/");

  private ServiceTracker<ConditionalPermissionAdmin, ConditionalPermissionAdmin> permAdminTracker;

  private volatile ConditionalPermissionAdmin conditionalPermissionAdmin = null;

  private volatile Priority priorityResult = null;

  @SuppressWarnings("squid:S1149" /* required by osgi's API */)
  @Override
  public void start(BundleContext bundleContext) throws Exception {
    System.setProperty("/", File.separator);
    this.conditionalPermissionAdmin = getConditionalPermissionAdmin(bundleContext);
    String policyDir = SecurityActions.getSystemProperty("ddf.home") + File.separator + "security";
    if (policyDir.startsWith("=")) {
      policyDir = policyDir.substring(1);
    }
    File policyDirFile = new File(policyDir);
    List<ParsedPolicy> parsedPolicies = new ArrayList<>();
    for (File file : Objects.requireNonNull(policyDirFile.listFiles())) {
      ParsedPolicy parse = null;
      try {
        parse = new Parser(false).parse(file);
      } catch (Exception e) {
        systemExit(file);
      }

      parsedPolicies.add(parse);
    }
    ConditionalPermissionUpdate conditionalPermissionUpdate =
        conditionalPermissionAdmin.newConditionalPermissionUpdate();
    conditionalPermissionUpdate.getConditionalPermissionInfos().clear();
    this.priorityResult = null;
    List<ConditionalPermissionInfo> allGrantInfos = new ArrayList<>();
    List<ConditionalPermissionInfo> allDenyInfos = new ArrayList<>();
    for (ParsedPolicy parsedPolicy : parsedPolicies) {
      List<ParsedPolicyEntry> grantEntries = parsedPolicy.getGrantEntries();
      List<ParsedPolicyEntry> denyEntries = parsedPolicy.getDenyEntries();

      buildConditionalPermissionInfo(grantEntries, allGrantInfos, ConditionalPermissionInfo.ALLOW);
      buildConditionalPermissionInfo(denyEntries, allDenyInfos, ConditionalPermissionInfo.DENY);

      Priority priority = parsedPolicy.getPriority();
      if (priorityResult == null) {
        this.priorityResult = priority;
      } else if (priority != priorityResult) {
        // if they don't match, then we can't make a determination on the priority, so we'll
        // default to deny
        this.priorityResult = Priority.DENY;
      }
    }

    if (priorityResult == null && !allGrantInfos.isEmpty() && !allDenyInfos.isEmpty()) {
      this.priorityResult = Priority.GRANT;
    }

    if (priorityResult == Priority.GRANT) {
      conditionalPermissionUpdate.getConditionalPermissionInfos().addAll(allGrantInfos);
      conditionalPermissionUpdate.getConditionalPermissionInfos().addAll(allDenyInfos);
      conditionalPermissionUpdate
          .getConditionalPermissionInfos()
          .add(getAllPermission(ConditionalPermissionInfo.ALLOW));
    } else if (priorityResult == Priority.DENY) {
      conditionalPermissionUpdate.getConditionalPermissionInfos().addAll(allDenyInfos);
      conditionalPermissionUpdate.getConditionalPermissionInfos().addAll(allGrantInfos);
      conditionalPermissionUpdate
          .getConditionalPermissionInfos()
          .add(getAllPermission(ConditionalPermissionInfo.DENY));
    }

    conditionalPermissionUpdate.commit();
  }

  public void grantPermission(String bundle, String permission) throws Exception {
    synchronized (this) {
      // use the parsed policy to make it easier to parse the permission string
      final ParsedPolicy parsedPolicy =
          new Parser(false)
              .parse(
                  new StringReader(
                      String.format(
                          "grant codebase \"file:/%s\" { permission %s; }", bundle, permission)));
      final List<ParsedPolicyEntry> grantEntries = parsedPolicy.getGrantEntries();
      final List<ConditionalPermissionInfo> allGrantInfos = new ArrayList<>();
      final ConditionalPermissionUpdate conditionalPermissionUpdate =
          conditionalPermissionAdmin.newConditionalPermissionUpdate();

      buildConditionalPermissionInfo(grantEntries, allGrantInfos, ConditionalPermissionInfo.ALLOW);
      final ConditionalPermissionInfo grantInfo = allGrantInfos.get(0);
      final List<ConditionalPermissionInfo> conditionalInfos =
          conditionalPermissionUpdate.getConditionalPermissionInfos();
      boolean added = false;

      // see if we can find one conditional permission for the exact same permission
      // if we do, let's just add this new bundle to the list as opposed to adding a
      // brand new conditional permission
      for (final ListIterator<ConditionalPermissionInfo> i = conditionalInfos.listIterator();
          i.hasNext(); ) {
        final ConditionalPermissionInfo permInfo = i.next();

        if (Objects.equals(grantInfo.getAccessDecision(), permInfo.getAccessDecision())
            && Arrays.equals(grantInfo.getPermissionInfos(), permInfo.getPermissionInfos())) {
          final ConditionInfo[] conditions = permInfo.getConditionInfos();

          if ((conditions != null)
              && (conditions.length == 1)
              && BUNDLE_NAME_CONDITION.equals(conditions[0].getType())) {
            final String[] bundles = conditions[0].getArgs();
            final String[] newBundles = new String[bundles.length + 1];

            System.arraycopy(bundles, 0, newBundles, 0, bundles.length);
            newBundles[bundles.length] = bundle;
            final ConditionalPermissionInfo newPermInfo =
                conditionalPermissionAdmin.newConditionalPermissionInfo(
                    permInfo.getName(),
                    new ConditionInfo[] {new ConditionInfo(BUNDLE_NAME_CONDITION, newBundles)},
                    permInfo.getPermissionInfos(),
                    permInfo.getAccessDecision());

            i.set(newPermInfo);
            added = true;
            break;
          }
        }
      }
      if (!added) {
        // if priority is to grant then insert at the top, otherwise insert before
        // the last entry which always reference an all-permission to deny
        final int index = (priorityResult == Priority.GRANT) ? 0 : conditionalInfos.size() - 1;

        conditionalInfos.add(index, grantInfo);
      }
      conditionalPermissionUpdate.commit();
    }
  }

  @VisibleForTesting
  @SuppressWarnings("squid:S106" /* Logging subsystem not yet initialized */)
  void systemExit(File file) {
    System.out.printf("%nUnable to parse policy file, %s. Please fix and try again.%n", file);
    System.exit(1);
  }

  ConditionalPermissionAdmin getConditionalPermissionAdmin(BundleContext bundleContext) {
    permAdminTracker = new ServiceTracker<>(bundleContext, PERM_ADMIN_SERVICE_NAME, null);
    permAdminTracker.open();

    return permAdminTracker.getService();
  }

  private ConditionalPermissionInfo getAllPermission(String type) {
    return conditionalPermissionAdmin.newConditionalPermissionInfo(
        null,
        null,
        new PermissionInfo[] {new PermissionInfo("java.security.AllPermission", "", "")},
        type);
  }

  /**
   * This method will allow policy entries with no permissions for the case where there are
   * pre-defined policy entries for administrators to add configuration specific permissions.
   */
  private void buildConditionalPermissionInfo(
      List<ParsedPolicyEntry> entries, List<ConditionalPermissionInfo> infos, String type) {
    for (ParsedPolicyEntry parsedPolicyEntry : entries) {
      List<ParsedPermission> permissions = parsedPolicyEntry.getPermissions();
      if (permissions.isEmpty()) {
        // Allow policy entries with no permissions.
        continue;
      }

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
              REGEX.split(replaceSystemProperties(getBundleName(codebase)))));
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
