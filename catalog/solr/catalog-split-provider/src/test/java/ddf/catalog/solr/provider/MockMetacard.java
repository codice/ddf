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
package ddf.catalog.solr.provider;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

public class MockMetacard extends MetacardImpl {

  public static final String DEFAULT_TITLE = "Flagstaff";

  public static final String DEFAULT_VERSION = "mockVersion";

  public static final String DEFAULT_TYPE = "simple";

  public static final String DEFAULT_LOCATION = "POINT (1 0)";

  public static final String DEFAULT_TAG = "resource";

  public static final byte[] DEFAULT_THUMBNAIL = {-86};

  private static final long serialVersionUID = -189776439741244547L;

  public static String getFlagstaffRecord() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
        + "<rss version=\"2.0\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"\r\n"
        + "    xmlns:wfw=\"http://wellformedweb.org/CommentAPI/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\r\n"
        + "    xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:sy=\"http://purl.org/rss/1.0/modules/syndication/\"\r\n"
        + "    xmlns:slash=\"http://purl.org/rss/1.0/modules/slash/\" xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\"\r\n"
        + "    xmlns:rawvoice=\"http://www.rawvoice.com/rawvoiceRssModule/\">\r\n"
        + "\r\n"
        + "    <channel>\r\n"
        + "        <title>Flagstaff Chamber of Commerce</title>\r\n"
        + "        <atom:link href=\"http://www.flagstaffchamber.com/feed/\" rel=\"self\"\r\n"
        + "            type=\"application/rss+xml\" />\r\n"
        + "        <link>http://www.flagstaffchamber.com</link>\r\n"
        + "        <description></description>\r\n"
        + "        <lastBuildDate>Mon, 16 Jul 2012 21:32:53 +0000</lastBuildDate>\r\n"
        + "        <language>en</language>\r\n"
        + "        <sy:updatePeriod>hourly</sy:updatePeriod>\r\n"
        + "        <sy:updateFrequency>1</sy:updateFrequency>\r\n"
        + "        <generator>http://wordpress.org/?v=3.1</generator>\r\n"
        + "        <!-- podcast_generator=\"Blubrry PowerPress/2.0.4\" -->\r\n"
        + "        <itunes:summary></itunes:summary>\r\n"
        + "        <itunes:author>Flagstaff Chamber of Commerce</itunes:author>\r\n"
        + "        <itunes:explicit>no</itunes:explicit>\r\n"
        + "        <itunes:image\r\n"
        + "            href=\"http://www.flagstaffchamber.com/wp-content/plugins/powerpress/itunes_default.jpg\" />\r\n"
        + "        <itunes:subtitle></itunes:subtitle>\r\n"
        + "        <image>\r\n"
        + "            <title>Flagstaff Chamber of Commerce</title>\r\n"
        + "            <url>http://www.flagstaffchamber.com/wp-content/plugins/powerpress/rss_default.jpg</url>\r\n"
        + "            <link>http://www.flagstaffchamber.com</link>\r\n"
        + "        </image>\r\n"
        + "        <item>\r\n"
        + "            <title>Arizona Cardinals NFL Training Camp Schedule, here in Airport FLG in AZ</title>\r\n"
        + "            <link>http://www.flagstaffchamber.com/arizona-cardinals-nfl-training-camp-schedule/</link>\r\n"
        + "            <comments>http://www.flagstaffchamber.com/arizona-cardinals-nfl-training-camp-schedule/#comments</comments>\r\n"
        + "            <pubDate>Tue, 03 Jul 2012 23:16:58 +0000</pubDate>\r\n"
        + "            <dc:creator>Flagstaff Chamber</dc:creator>\r\n"
        + "            <category><![CDATA[Uncategorized]]></category>\r\n"
        + "\r\n"
        + "            <guid isPermaLink=\"false\">http://www.flagstaffchamber.com/?p=10160</guid>\r\n"
        + "            <description><![CDATA[Join the Arizona Cardinals as they conduct their NFL Training Camp in Flagstaff from July 25 to August 21.]]></description>\r\n"
        + "            <wfw:commentRss>http://www.flagstaffchamber.com/arizona-cardinals-nfl-training-camp-schedule/feed/\r\n"
        + "            </wfw:commentRss>\r\n"
        + "            <slash:comments>0</slash:comments>\r\n"
        + "        </item>\r\n"
        + "    </channel>\r\n"
        + "</rss>\r\n"
        + "\r\n"
        + "\r\n"
        + "<!-- Performance optimized by W3 Total Cache. Learn more: http://www.w3-edge.com/wordpress-plugins/ \r\n"
        + "    Served from: flagstaffchamber.com @ 2012-07-17 08:28:04 -->";
  }

  public static String getTampaRecord() {
    return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
        + "<rss xmlns:atom=\"http://www.w3.org/2005/Atom\" version=\"2.0\">\r\n"
        + "    <channel>\r\n"
        + "        <title>www2.tbo.com - News</title>\r\n"
        + "        <link>http://www2.tbo.com/news/</link>\r\n"
        + "        <description></description>\r\n"
        + "        <atom:link href=\"http://www2.tbo.com/feed/rss/news/\" rel=\"self\"></atom:link>\r\n"
        + "        <language>en-us</language>\r\n"
        + "        <copyright>Copyright (c) 2012, Media General Communications Holdings,\r\n"
        + "            LLC. A Media General company.</copyright>\r\n"
        + "        <lastBuildDate>Tue, 17 Jul 2012 13:03:04 -0400</lastBuildDate>\r\n"
        + "        <item>\r\n"
        + "            <title>Body of Staff Sgt. Ricardo Seija returns home</title>\r\n"
        + "            <link>http://www2.tbo.com/news/news/2012/jul/17/7/body-of-staff-sgt-ricardo-seija-returns-home-ar-433878/</link>\r\n"
        + "            <description>Body of Staff Sgt. Ricardo Seija returns home</description>\r\n"
        + "            <pubDate>Tue, 17 Jul 2012 13:03:04 -0400</pubDate>\r\n"
        + "            <guid>http://www2.tbo.com/news/news/2012/jul/17/7/body-of-staff-sgt-ricardo-seija-returns-home-ar-433878/</guid>\r\n"
        + "            <enclosure\r\n"
        + "                url=\"http://www2.tbo.com/mgmedia/image/100/100/217458/img-20120717-02368/\"\r\n"
        + "                length=\"30000\" type=\"image/jpeg\"></enclosure>\r\n"
        + "        </item>\r\n"
        + "        <item>\r\n"
        + "            <title>Sears steps down after years of government service</title>\r\n"
        + "            <link>http://www2.tbo.com/news/plant-city/2012/jul/17/sears-steps-down-after-years-of-government-service-ar-429159/</link>\r\n"
        + "            <description>Sears steps down after years of government service</description>\r\n"
        + "            <pubDate>Tue, 17 Jul 2012 13:00:00 -0400</pubDate>\r\n"
        + "            <guid>http://www2.tbo.com/news/plant-city/2012/jul/17/sears-steps-down-after-years-of-government-service-ar-429159/</guid>\r\n"
        + "            <enclosure\r\n"
        + "                url=\"http://www2.tbo.com/mgmedia/image/100/100/216697/pcsears18jpg/\"\r\n"
        + "                length=\"30000\" type=\"image/jpeg\"></enclosure>\r\n"
        + "        </item>\r\n"
        + "        <item>\r\n"
        + "            <title>Spirit Airlines to offer non-stop service from Tampa to\r\n"
        + "                Chicago, Airport TPA in FL</title>\r\n"
        + "            <link>http://www2.tbo.com/news/business/2012/jul/17/spirit-airlines-to-offer-non-stop-service-from-tam-ar-433900/</link>\r\n"
        + "            <description>Spirit Airlines to offer non-stop service from Tampa to\r\n"
        + "                Chicago</description>\r\n"
        + "            <pubDate>Tue, 17 Jul 2012 12:48:10 -0400</pubDate>\r\n"
        + "            <guid>http://www2.tbo.com/news/business/2012/jul/17/spirit-airlines-to-offer-non-stop-service-from-tam-ar-433900/</guid>\r\n"
        + "        </item>\r\n"
        + "        <item>\r\n"
        + "            <title>More than workers</title>\r\n"
        + "            <link>http://www2.tbo.com/news/opinion/2012/jul/17/naopino2-more-than-workers-ar-433254/</link>\r\n"
        + "            <description>More than workers</description>\r\n"
        + "            <pubDate>Tue, 17 Jul 2012 00:00:00 -0400</pubDate>\r\n"
        + "            <guid>http://www2.tbo.com/news/opinion/2012/jul/17/naopino2-more-than-workers-ar-433254/</guid>\r\n"
        + "            <enclosure\r\n"
        + "                url=\"http://www2.tbo.com/mgmedia/image/100/100/217388/ed-school/\"\r\n"
        + "                length=\"30000\" type=\"image/jpeg\"></enclosure>\r\n"
        + "        </item>\r\n"
        + "    </channel>\r\n"
        + "</rss>";
  }

  public MockMetacard(String metadata, MetacardType type, Calendar calendar) {
    super(type);
    // make a simple metacard
    this.setCreatedDate(calendar.getTime());
    this.setEffectiveDate(calendar.getTime());
    this.setExpirationDate(calendar.getTime());
    this.setModifiedDate(calendar.getTime());
    this.setMetadata(metadata);
    this.setContentTypeName(DEFAULT_TYPE);
    this.setContentTypeVersion(DEFAULT_VERSION);
    this.setLocation(DEFAULT_LOCATION);
    this.setThumbnail(DEFAULT_THUMBNAIL);
    this.setTitle(DEFAULT_TITLE);
    this.setSecurity(new HashMap<>());
    this.setTags(Collections.singleton(DEFAULT_TAG));
  }

  public static Metacard createMetacard(String metadata, MetacardType type, Calendar calendar) {
    MetacardImpl metacard = new MetacardImpl(type);

    // make a simple metacard
    metacard.setCreatedDate(calendar.getTime());
    metacard.setEffectiveDate(calendar.getTime());
    metacard.setExpirationDate(calendar.getTime());
    metacard.setModifiedDate(calendar.getTime());
    metacard.setMetadata(metadata);
    metacard.setContentTypeName(DEFAULT_TYPE);
    metacard.setContentTypeVersion(DEFAULT_VERSION);
    metacard.setLocation(DEFAULT_LOCATION);
    metacard.setThumbnail(DEFAULT_THUMBNAIL);
    metacard.setTitle(DEFAULT_TITLE);
    metacard.setSecurity(new HashMap<>());
    metacard.setTags(Collections.singleton(DEFAULT_TAG));
    return metacard;
  }

  public static Metacard createMetacard(String metadata, MetacardType type) {
    return MockMetacard.createMetacard(metadata, type, Calendar.getInstance());
  }

  public static Metacard createMetacard(String metadata) {
    return MockMetacard.createMetacard(metadata, MetacardImpl.BASIC_METACARD);
  }
}
