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
package org.codice.ddf.landing;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.common.annotations.VisibleForTesting;
import ddf.platform.resource.bundle.locator.ResourceBundleLocator;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.branding.BrandingPlugin;
import org.codice.ddf.branding.BrandingRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple HttpServlet to serve up a landing page that displays deployment-specific information
 * such as data source availability and announcements. Announcements are implemented as configurable
 * properties. The Title and Version are retrieved via the {@link BrandingPlugin}. Additionally, a
 * phone number, email address, external website, and description are configurable properties.
 */
@SuppressWarnings({"squid:S2226", "squid:S1068"} /* Lifecycle managed by blueprint. */)
public class LandingPage extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private transient Optional<BrandingRegistry> branding = Optional.empty();

  private String title;

  private String version;

  private String description;

  private String phone;

  private String email;

  private String externalUrl;

  private String sourceAvailabilityHeader;

  private List<String> announcements;

  private String linksTitle;

  private List<String> links;

  private Map<String, String> parsedLinks;

  private static final String LANDING_PAGE_FILE = "index";

  private static final String NO_DATE = "No date specified";

  private static final String LANDING_PAGE_BASE_NAME = "LandingPageBundle";

  private static final List<String> ACCEPTED_PATHS =
      Arrays.asList(
          "/index.html",
          "/index.htm",
          "home.html",
          "home.htm",
          "landing.html",
          "landing.htm",
          "/",
          "");

  private static final Logger LOGGER = LoggerFactory.getLogger(LandingPage.class);

  private String logo;

  private String logoToUse;

  private String background;

  private String foreground;

  public void setSourceAvailabilityHeader(ResourceBundleLocator resourceBundleLocator) {
    try {
      ResourceBundle resourceBundle = resourceBundleLocator.getBundle(LANDING_PAGE_BASE_NAME);
      this.sourceAvailabilityHeader = resourceBundle.getString("data.source.availability");
    } catch (IOException e) {
      LOGGER.debug(
          "An error occurred while creating class loader to URL for ResourceBundle: {}, {}",
          LANDING_PAGE_BASE_NAME,
          Locale.getDefault(),
          e);
    }
  }

  @VisibleForTesting
  void setSourceAvailabilityHeader(String sourceKeyword) {
    this.sourceAvailabilityHeader = sourceKeyword;
  }

  @VisibleForTesting
  String getSourceAvailabilityHeader() {
    return this.sourceAvailabilityHeader;
  }

  public List<String> getAnnouncements() {
    return announcements;
  }

  public void setBranding(BrandingRegistry branding) {
    this.branding = Optional.ofNullable(branding);
  }

  public BrandingRegistry getBranding() {
    return branding.orElse(null);
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setExternalUrl(String externalUrl) {
    this.externalUrl = externalUrl;
  }

  // Should this be included as a workaround for how the admin UI handles metatype lists?
  public void setAnnouncements(String announcements) {
    setAnnouncements(Arrays.asList(announcements.split(",")));
  }

  public void setAnnouncements(List<String> announcements) {
    this.announcements = announcements;
    sortAnnouncementsByDate();
  }

  public synchronized void setLinks(List<String> links) {
    this.links = links;
    parseLinks();
  }

  public synchronized Map<String, String> getParsedLinks() {
    return parsedLinks;
  }

  private synchronized void parseLinks() {
    parsedLinks = new HashMap<>();
    for (String link : links) {
      if (StringUtils.countMatches(link, ",") != 1) {
        LOGGER.warn("Unable to parse link ({}). Links should have exactly one comma.", link);
        continue;
      }
      String[] parts = link.split(",");
      String text = parts[0].trim();
      String url = parts[1].trim();
      if (text.isEmpty() || url.isEmpty()) {
        LOGGER.warn("Unable to parse link ({}). Neither the text nor the URL can be empty.", link);
        continue;
      }
      parsedLinks.put(text, url);
    }
  }

  public void setBackground(String background) {
    this.background = background;
  }

  public void setForeground(String foreground) {
    this.foreground = foreground;
  }

  public void setLogo(String logo) {
    this.logo = logo;
  }

  public String getProductImage() {
    return branding
        .map(registry -> registry.getAttributeFromBranding(BrandingPlugin::getBase64ProductImage))
        .orElse("");
  }

  public String getLinksTitle() {
    return linksTitle;
  }

  public void setLinksTitle(String linksTitle) {
    this.linksTitle = linksTitle.trim();
  }

  public String getTitle() {
    return branding.map(BrandingRegistry::getProductName).orElse("");
  }

  public String getVersion() {
    return branding.map(BrandingRegistry::getProductVersion).orElse("");
  }

  public String getFavicon() {
    return branding
        .map(registry -> registry.getAttributeFromBranding(BrandingPlugin::getBase64FavIcon))
        .orElse("");
  }

  private synchronized void sortAnnouncementsByDate() {
    Collections.sort(
        announcements,
        (firstAnnouncement, secondAnnouncement) -> {
          // Try to extract the dates from the announcements.
          String firstDateString = extractDate(firstAnnouncement, false);
          String secondDateString = extractDate(secondAnnouncement, false);

          final Date firstDate = dateFromString(firstDateString);
          final Date secondDate = dateFromString(secondDateString);
          // Sort the dates in descending order (most recent first).
          return secondDate.compareTo(firstDate);
        });
  }

  private Date dateFromString(String dateString) {
    final DateFormat format = new SimpleDateFormat("MM/dd/yy");
    Date date;
    try {
      date = format.parse(dateString);
    } catch (ParseException e) {
      date = new Date(0L);
    }
    return date;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML);
    if (ACCEPTED_PATHS.contains(req.getRequestURI())) {
      resp.getWriter().write(compileTemplateWithProperties());
    }
  }

  // package-private for unit testing
  String compileTemplateWithProperties() {
    title = getTitle();
    version =
        branding
            .map(registry -> registry.getAttributeFromBranding(BrandingPlugin::getProductName))
            .orElse("");
    logoToUse = StringUtils.isNotEmpty(logo) ? logo : getProductImage();
    // FieldValueResolver so this class' fields can be accessed in the template.
    // MapValueResolver so we can access {{@index}} in the #each helper in the template.
    final Context context =
        Context.newBuilder(this)
            .resolver(FieldValueResolver.INSTANCE, MapValueResolver.INSTANCE)
            .build();
    // The template is "index.handlebars".
    final TemplateLoader templateLoader = new ClassPathTemplateLoader("/", ".handlebars");
    final Handlebars handlebars = new Handlebars(templateLoader);
    // extractDate(), extractAnnouncement(), expanded(), and in() are helper functions used in the
    // template.
    handlebars.registerHelpers(this);
    String landingPageHtml;
    try {
      final Template template = handlebars.compile(LANDING_PAGE_FILE);
      landingPageHtml = template.apply(context);
    } catch (IOException e) {
      LOGGER.info("Unable to compile Landing Page template.", e);
      landingPageHtml = "<p>We are experiencing some issues. Please contact an administrator.</p>";
    }
    return landingPageHtml;
  }

  // A helper function used in the Handlebars template (index.handlebars). Also used locally.
  public String extractDate(String announcement, boolean reformat) {
    announcement = announcement.trim();

    // Regular expression to (loosely) match a date at the beginning of the string (we don't require
    // leading 0s
    // for the month and day).
    // We expect the date to be at the beginning of the string and be in the format MM/dd/yy (where
    // the month and
    // day can have either 1 or 2 digits).
    final String pattern = "^\\d{1,2}/\\d{1,2}/\\d\\d";
    final Pattern regex = Pattern.compile(pattern);
    final Matcher matcher = regex.matcher(announcement);

    if (matcher.find()) {
      final String matchedText = matcher.group();
      if (reformat) { // Return the date in a nice-looking format. Used for displaying the dates on
        // the Web page.
        final Date date = dateFromString(matchedText);
        final SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy");
        return format.format(date);
      } else { // Just return the date as it was in the announcement.
        return matchedText;
      }
    }

    return NO_DATE;
  }

  // A helper function used in the Handlebars template (index.handlebars).
  public String extractAnnouncement(String announcement) {
    // Try and grab the date from the beginning of the announcement.
    final String date = extractDate(announcement, false);
    if (!date.equals(
        NO_DATE)) { // There is a valid date, so we need to exclude it from what we display (it's
      // not
      try { // part of the announcement).
        // Ignore the date part of the announcement.
        announcement = announcement.substring(announcement.indexOf(date) + date.length());
      } catch (IndexOutOfBoundsException e) {
        // Do nothing. The announcement is empty.
      }
    }

    return announcement.trim();
  }

  // A helper function used in the Handlebars template (index.handlebars).
  public String expanded(int index) {
    return index == 0 ? "true" : "false";
  }

  // A helper function used in the Handlebars template (index.handlebars).
  public String in(int index) {
    return index == 0 ? "in" : "";
  }

  // A helper function used in the Handlebars template (index.handlebars).
  public String noAnnouncements(List<String> announcements) {
    if (announcements.isEmpty()) {
      return "No announcements";
    } else {
      return "";
    }
  }
}
