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
package org.codice.ddf.registry.rest.endpoint.report;

import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectListWebConverter.ORGANIZATION_KEY;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Contact;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.rest.endpoint.HandlebarsHelper;
import org.codice.ddf.registry.schemabindings.converter.web.OrganizationWebConverter;
import org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectListWebConverter;
import org.codice.ddf.registry.schemabindings.helper.RegistryPackageTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

/**
 * Generates a specific report as a String of html which is then compiled and shown through the
 * RegistryRestEndpoint.
 *
 * <p>More specifically within this class, maps are generated through the classes contained in
 * registry-schema-bindings.The details contained in the maps populate a specific handlebar template
 * and result in a String html that is relayed to RegistryRestEndpoint.
 */
public class RegistryReportBuilder {

  public static final String REPORT = "report";

  public static final String SUMMARY = "summary";

  public static final String ORGANIZATIONS = "organizations";

  public static final String ERROR = "error";

  private ClassPathTemplateLoader templateLoader;

  private Handlebars handlebars;

  private WebMapHelper webMapHelper = new WebMapHelper();

  public void setup() {
    templateLoader = new ClassPathTemplateLoader();
    templateLoader.setPrefix("/templates");
    templateLoader.setSuffix(".hbt");
    handlebars = new Handlebars(templateLoader);
    handlebars.registerHelpers(new HandlebarsHelper());
    handlebars.registerHelpers(StringHelpers.class);
  }

  public String getSummaryHtmlFromMetacard(Metacard metacard) throws IOException {

    Map<String, Object> reportMap = getSummaryMap(metacard);
    Template template = handlebars.compile(SUMMARY);
    String html = template.apply(reportMap);
    html = html.replaceAll("\n", "");
    return html;
  }

  public String getHtmlFromRegistryPackage(
      RegistryPackageType registryPackage, String handlebarTemplate) throws IOException {

    Map<String, Object> reportMap = new HashMap<>();
    if (handlebarTemplate.equals(ORGANIZATIONS)) {
      reportMap = getOrganizationMap(registryPackage);
    } else if (handlebarTemplate.equals(REPORT)) {
      reportMap = getFullRegistryMap(registryPackage);
    }
    Template template = handlebars.compile(handlebarTemplate);
    String html = template.apply(reportMap);
    html = html.replaceAll("\n", "");
    return html;
  }

  public String getErrorHtml(String errorMessage) throws IOException {
    Template template = handlebars.compile(ERROR);
    String html = template.apply(errorMessage);
    return html;
  }

  private Map<String, Object> getSummaryMap(Metacard metacard) {
    Map<String, Object> summaryMap = new HashMap<>();

    Map<String, String> summaryAttributes =
        ImmutableMap.of(
            Contact.POINT_OF_CONTACT_NAME,
            "Organization Name",
            Metacard.POINT_OF_CONTACT,
            "Point of Contact",
            Contact.POINT_OF_CONTACT_ADDRESS,
            "Address",
            Contact.POINT_OF_CONTACT_EMAIL,
            "Email Addresses",
            Contact.POINT_OF_CONTACT_PHONE,
            "Phone Numbers");

    summaryAttributes
        .keySet()
        .stream()
        .forEach(
            key ->
                webMapHelper.putIfNotEmpty(
                    summaryMap,
                    summaryAttributes.get(key),
                    RegistryUtility.getListOfStringAttribute(metacard, key)));
    return summaryMap;
  }

  private Map<String, Object> getFullRegistryMap(RegistryPackageType registryPackage) {
    RegistryObjectListWebConverter registryObjectListWebConverter =
        new RegistryObjectListWebConverter();
    return registryObjectListWebConverter.convert(registryPackage.getRegistryObjectList());
  }

  private Map<String, Object> getOrganizationMap(RegistryPackageType registryPackage) {
    Map<String, Object> organizationMap = new HashMap<>();

    List<Map<String, Object>> organizations = new ArrayList<>();
    RegistryPackageTypeHelper rpt = new RegistryPackageTypeHelper(registryPackage);
    OrganizationWebConverter owc = new OrganizationWebConverter();
    organizations.addAll(
        rpt.getOrganizations().stream().map(owc::convert).collect(Collectors.toList()));
    webMapHelper.putIfNotEmpty(organizationMap, ORGANIZATION_KEY, organizations);

    return organizationMap;
  }
}
