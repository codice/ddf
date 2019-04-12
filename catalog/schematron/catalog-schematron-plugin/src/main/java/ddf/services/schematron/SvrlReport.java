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
package ddf.services.schematron;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.codice.ddf.platform.util.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Schematron Validation Report Language (SVRL) formatted report of output from Schematron
 * validation.
 *
 * @author rodgersh
 */
public class SvrlReport implements SchematronReport {
  /** SVRL report tag for assertion that failed during Schematron validation */
  private static final String ASSERT_FAIL_TAG = "svrl:failed-assert";

  /** SVRL report tag for report that failed during Schematron validation */
  private static final String REPORT_FAIL_TAG = "svrl:failed-report";

  /**
   * SVRL report tag for flag attribute in a svrl:failed-assert element that indicates if the
   * failure is an error or warning
   */
  private static final String FLAG_ATTR = "flag";

  /**
   * Value for svrl:failed-assert tag's flag attribute for warnings.
   *
   * <p>Example: <svrl:failed-assert test="if(invalid) then 1 else not($hasInvalids)"
   * flag="warning"> ... </svrl:failed-assert>
   */
  private static final String WARNING_FLAG_ATTR_TEXT = "warning";

  /**
   * Value for svrl:failed-assert tag's flag attribute for errors
   *
   * <p>Example: <svrl:failed-assert test="if(invalid) then 1 else not($hasInvalids)" flag="error">
   * ... </svrl:failed-assert>
   */
  private static final String ERROR_FLAG_ATTR_TEXT = "error";

  private static final Logger LOGGER = LoggerFactory.getLogger(SvrlReport.class);

  /** Schematron report in DOM format */
  private DOMResult report;

  /** The root element of the report's DOM tree. */
  private Element root = null;

  /** @param result DOM-formatted results from Schematron validation */
  public SvrlReport(DOMResult result) {
    this.report = result;
    this.root = (Element) report.getNode().getFirstChild();
  }

  public SvrlReport() {}

  /**
   * Returns true if Schematron report is valid, false otherwise. The input document is considered
   * to be valid if it has no failed assertions for errors and no failed reports for errors. If the
   * suppressWarnings argument is true, then Schematron warnings are also included in the document's
   * validity assessment.
   *
   * @param suppressWarnings do not include Schematron warnings in determining validity
   * @return true if no assert or report error messages found in SVRL report, false otherwise
   */
  @Override
  public boolean isValid(boolean suppressWarnings) {
    List<Node> errorAssertions = getAllAssertMessages(ERROR_FLAG_ATTR_TEXT);
    List<Node> errorReports = getAllReportMessages(ERROR_FLAG_ATTR_TEXT);

    if (errorAssertions.size() != 0 || errorReports.size() != 0) {
      return false;
    }

    if (!suppressWarnings) {
      List<Node> warningAssertions = getAllAssertMessages(WARNING_FLAG_ATTR_TEXT);
      List<Node> warningReports = getAllReportMessages(WARNING_FLAG_ATTR_TEXT);

      if (warningAssertions.size() != 0 || warningReports.size() != 0) {
        return false;
      }
    }

    return true;
  }

  /**
   * Retrieve all assertion messages, warnings and errors, from the SVRL report.
   *
   * @return list of XML Nodes for all assert nodes
   */
  @Override
  public NodeList getAllAssertMessages() {
    return root.getElementsByTagName(ASSERT_FAIL_TAG);
  }

  /**
   * Retrieve only the specified type of assertion messages (warnings or errors) from the SVRL
   * report.
   *
   * @return list of XML Nodes for all assert nodes of specified type
   * @parameter type the type of assert message to search for in SVRL report, "warning" or "error"
   */
  public List<Node> getAllAssertMessages(String type) {
    List<Node> assertions = new ArrayList<>();
    if (isEmpty()) {
      return assertions;
    }

    NodeList assertFailures = getAllAssertMessages();
    for (int i = 0; i < assertFailures.getLength(); i++) {
      Node assertion = assertFailures.item(i);
      NamedNodeMap attributes = assertion.getAttributes();
      Node flagNode = attributes.getNamedItem(FLAG_ATTR);
      if (flagNode != null && flagNode.getNodeValue().equals(type)) {
        assertions.add(assertion);
      }
    }

    return assertions;
  }

  /**
   * Retrieve all report messages, warnings and errors, from the SVRL report.
   *
   * @return list of XML Nodes for all report nodes
   */
  @Override
  public NodeList getAllReportMessages() {
    return root.getElementsByTagName(REPORT_FAIL_TAG);
  }

  /**
   * Retrieve only the specified type of report messages (warnings or errors) from the SVRL report.
   *
   * @return list of XML Nodes for all report nodes
   * @parameter type the type of report message to search for in SVRL report, "warning" or "error"
   */
  public List<Node> getAllReportMessages(String type) {
    List<Node> reports = new ArrayList<Node>();

    NodeList reportFailures = getAllReportMessages();
    for (int i = 0; i < reportFailures.getLength(); i++) {
      Node report = reportFailures.item(i);
      NamedNodeMap attributes = report.getAttributes();
      Node flagNode = attributes.getNamedItem(FLAG_ATTR);
      if (flagNode != null && flagNode.getNodeValue().equals(type)) {
        reports.add(report);
      }
    }

    return reports;
  }

  /**
   * Get a list of all of the assertion and report error messages from the SVRL report.
   *
   * @return list of error strings
   */
  @Override
  public List<String> getErrors() {
    List<String> errors = new ArrayList<>();

    if (isEmpty()) {
      return errors;
    }

    List<Node> errorAssertions = getAllAssertMessages(ERROR_FLAG_ATTR_TEXT);
    for (Node error : errorAssertions) {
      errors.add(error.getFirstChild().getTextContent());
    }

    List<Node> errorReports = getAllReportMessages(ERROR_FLAG_ATTR_TEXT);
    for (Node error : errorReports) {
      errors.add(error.getFirstChild().getTextContent());
    }

    return errors;
  }

  /**
   * Get a list of all of the assertion and report warning messages from the SVRL report.
   *
   * @return list of warning strings
   */
  @Override
  public List<String> getWarnings() {
    List<String> warnings = new ArrayList<>();

    if (isEmpty()) {
      return warnings;
    }

    List<Node> warningAssertions = getAllAssertMessages(WARNING_FLAG_ATTR_TEXT);
    for (Node warning : warningAssertions) {
      LOGGER.debug("warning(from assertions) = {}", warning.getFirstChild().getTextContent());
      warnings.add(warning.getFirstChild().getTextContent());
    }

    List<Node> warningReports = getAllReportMessages(WARNING_FLAG_ATTR_TEXT);
    for (Node warning : warningReports) {
      LOGGER.debug("warning(from reports) = {}", warning.getFirstChild().getTextContent());
      warnings.add(warning.getFirstChild().getTextContent());
    }

    return warnings;
  }

  /**
   * Retrieve the entire SVRL report as an XML-formatted string.
   *
   * @return XML-formatted string representation of SVRL report
   */
  @Override
  public String getReportAsText() throws TransformerException {
    Writer sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw);

    TransformerFactory tfactory = XMLUtils.getInstance().getSecureXmlTransformerFactory();
    Transformer transformer = tfactory.newTransformer();
    Properties props = new Properties();
    props.put("method", "xml");
    props.put("indent", "yes");
    transformer.setOutputProperties(props);
    transformer.transform(new DOMSource(root), new StreamResult(out));
    out.close();

    return sw.toString();
  }

  @Override
  public boolean isEmpty() {
    return root == null;
  }
}
