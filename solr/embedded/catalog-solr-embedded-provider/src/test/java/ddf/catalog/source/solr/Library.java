/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/

package ddf.catalog.source.solr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Library {

    /**
     * Logger for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Library.class.getName());

    public Library() {

    }

    /**
	 *
	 */
    private static final long serialVersionUID = -7160191237539812384L;

    // @formatter:off
	public static String getIndexingRecord() {
		return  
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
				"<rss version=\"2.0\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"\r\n" + 
				"	xmlns:wfw=\"http://wellformedweb.org/CommentAPI/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\r\n" + 
				"	xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:sy=\"http://purl.org/rss/1.0/modules/syndication/\"\r\n" + 
				"	xmlns:slash=\"http://purl.org/rss/1.0/modules/slash/\" xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\"\r\n" + 
				"	xmlns:rawvoice=\"http://www.rawvoice.com/rawvoiceRssModule/\">\r\n" + 
				"\r\n" + 
				"	<channel>\r\n" + 
				"		<title><!--description-->content text<![CDATA[<greeting>Hello</greeting>]]>other content</title>" +
				"		<atom:link title=\"Flagstaff atom feed\" href=\"http://www.flagstaffchamber.com/feed/\" rel=\"self\"\r\n" + 
				"			type=\"application/rss+xml\" />\r\n" + 
				"		<link>http://www.flagstaffchamber.com</link>\r\n" + 
				"		<description></description>\r\n" + 
				"		<lastBuildDate>Mon, 16 Jul 2012 21:32:53 +0000</lastBuildDate>\r\n" + 
				"		<language>en</language>\r\n" + 
				"		<sy:updatePeriod>hourly</sy:updatePeriod>\r\n" + 
				"		<sy:updateFrequency>1</sy:updateFrequency>\r\n" + 
				"		<generator>http://wordpress.org/?v=3.1</generator>\r\n" + 
				"		<!-- podcast_generator=\"Blubrry PowerPress/2.0.4\" -->\r\n" + 
				"		<itunes:summary></itunes:summary>\r\n" + 
				"		<itunes:author>Flagstaff Chamber of Commerce</itunes:author>\r\n" + 
				"		<itunes:explicit>no</itunes:explicit>\r\n" + 
				"		<itunes:image\r\n" + 
				"			href=\"http://www.flagstaffchamber.com/wp-content/plugins/powerpress/itunes_default.jpg\" />\r\n" + 
				"		<itunes:subtitle></itunes:subtitle>\r\n" + 
				"		<image>\r\n" + 
				"			<title>Flagstaff Chamber of Commerce</title>\r\n" + 
				"			<url>http://www.flagstaffchamber.com/wp-content/plugins/powerpress/rss_default.jpg</url>\r\n" + 
				"			<link>http://www.flagstaffchamber.com</link>\r\n" + 
				"		</image>\r\n" + 
				"		<item>\r\n" + 
				"			<title>Arizona Cardinals NFL Training Camp Schedule, here in Airport FLG in AZ</title>\r\n" + 
				"			<link>http://www.flagstaffchamber.com/arizona-cardinals-nfl-training-camp-schedule/</link>\r\n" + 
				"			<comments>http://www.flagstaffchamber.com/arizona-cardinals-nfl-training-camp-schedule/#comments</comments>\r\n" + 
				"			<pubDate>Tue, 03 Jul 2012 23:16:58 +0000</pubDate>\r\n" + 
				"			<dc:creator>Flagstaff Chamber</dc:creator>\r\n" + 
				"			<category><![CDATA[Uncategorized]]></category>\r\n" + 
				"\r\n" + 
				"			<guid isPermaLink=\"false\">http://www.flagstaffchamber.com/?p=10160</guid>\r\n" + 
				"			<description><![CDATA[Join the Arizona Cardinals as they conduct their NFL Training Camp in Flagstaff from July 25 to August 21.]]></description>\r\n" + 
				"			<wfw:commentRss>http://www.flagstaffchamber.com/arizona-cardinals-nfl-training-camp-schedule/feed/\r\n" + 
				"			</wfw:commentRss>\r\n" + 
				"			<slash:comments>0</slash:comments>\r\n" + 
				"		</item>\r\n" + 
				"	</channel>\r\n" + 
				"</rss>\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"<!-- Performance optimized by W3 Total Cache. Learn more: http://www.w3-edge.com/wordpress-plugins/ \r\n" + 
				"	Served from: flagstaffchamber.com @ 2012-07-17 08:28:04 -->";
	}	
	
	
	public static String getFlagstaffRecord() {
		return  
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
				"<rss version=\"2.0\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"\r\n" + 
				"	xmlns:wfw=\"http://wellformedweb.org/CommentAPI/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\r\n" + 
				"	xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:sy=\"http://purl.org/rss/1.0/modules/syndication/\"\r\n" + 
				"	xmlns:slash=\"http://purl.org/rss/1.0/modules/slash/\" xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\"\r\n" + 
				"	xmlns:rawvoice=\"http://www.rawvoice.com/rawvoiceRssModule/\">\r\n" + 
				"\r\n" + 
				"	<channel>\r\n" + 
				"		<title>Flagstaff Chamber of Commerce</title>\r\n" + 
				"		<atom:link href=\"http://www.flagstaffchamber.com/feed/\" rel=\"self\"\r\n" + 
				"			type=\"application/rss+xml\" />\r\n" + 
				"		<link>http://www.flagstaffchamber.com</link>\r\n" + 
				"		<description></description>\r\n" + 
				"		<lastBuildDate>Mon, 16 Jul 2012 21:32:53 +0000</lastBuildDate>\r\n" + 
				"		<language>en</language>\r\n" + 
				"		<sy:updatePeriod>hourly</sy:updatePeriod>\r\n" + 
				"		<sy:updateFrequency>1</sy:updateFrequency>\r\n" + 
				"		<generator>http://wordpress.org/?v=3.1</generator>\r\n" + 
				"		<!-- podcast_generator=\"Blubrry PowerPress/2.0.4\" -->\r\n" + 
				"		<itunes:summary></itunes:summary>\r\n" + 
				"		<itunes:author>Flagstaff Chamber of Commerce</itunes:author>\r\n" + 
				"		<itunes:explicit>no</itunes:explicit>\r\n" + 
				"		<itunes:image\r\n" + 
				"			href=\"http://www.flagstaffchamber.com/wp-content/plugins/powerpress/itunes_default.jpg\" />\r\n" + 
				"		<itunes:subtitle></itunes:subtitle>\r\n" + 
				"		<image>\r\n" + 
				"			<title>Flagstaff Chamber of Commerce</title>\r\n" + 
				"			<url>http://www.flagstaffchamber.com/wp-content/plugins/powerpress/rss_default.jpg</url>\r\n" + 
				"			<link>http://www.flagstaffchamber.com</link>\r\n" + 
				"		</image>\r\n" + 
				"		<item>\r\n" + 
				"			<title>Arizona Cardinals NFL Training Camp Schedule, here in Airport FLG in AZ</title>\r\n" + 
				"			<link>http://www.flagstaffchamber.com/arizona-cardinals-nfl-training-camp-schedule/</link>\r\n" + 
				"			<comments>http://www.flagstaffchamber.com/arizona-cardinals-nfl-training-camp-schedule/#comments</comments>\r\n" + 
				"			<pubDate>Tue, 03 Jul 2012 23:16:58 +0000</pubDate>\r\n" + 
				"			<dc:creator>Flagstaff Chamber</dc:creator>\r\n" + 
				"			<category><![CDATA[Uncategorized]]></category>\r\n" + 
				"\r\n" + 
				"			<guid isPermaLink=\"false\">http://www.flagstaffchamber.com/?p=10160</guid>\r\n" + 
				"			<description><![CDATA[Join the Arizona Cardinals as they conduct their NFL Training Camp in Flagstaff from July 25 to August 21.]]></description>\r\n" + 
				"			<wfw:commentRss>http://www.flagstaffchamber.com/arizona-cardinals-nfl-training-camp-schedule/feed/\r\n" + 
				"			</wfw:commentRss>\r\n" + 
				"			<slash:comments>0</slash:comments>\r\n" + 
				"		</item>\r\n" + 
				"	</channel>\r\n" + 
				"</rss>\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"<!-- Performance optimized by W3 Total Cache. Learn more: http://www.w3-edge.com/wordpress-plugins/ \r\n" + 
				"	Served from: flagstaffchamber.com @ 2012-07-17 08:28:04 -->";
	}

   public static String getShowLowRecord() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
				"<rss version=\"2.0\">\r\n" + 
				"	<channel>\r\n" + 
				"		<title>Show Low, AZ Weather from Weather Underground</title>\r\n" + 
				"		<link>http://www.wunderground.com/</link>\r\n" + 
				"		<description>Weather Underground RSS Feed for Show Low, AZ US\r\n" + 
				"		</description>\r\n" + 
				"		<language>EN</language>\r\n" + 
				"		<generator>WU-RSS</generator>\r\n" + 
				"		<webMaster>support@wunderground.com (Wunderground Support)</webMaster>\r\n" + 
				"		<category>weather</category>\r\n" + 
				"		<image>\r\n" + 
				"			<url>http://icons.wunderground.com/graphics/smash/wunderTransparent.gif\r\n" + 
				"			</url>\r\n" + 
				"			<link>http://www.wunderground.com/</link>\r\n" + 
				"			<title>Show Low, AZ Weather from Weather Underground</title>\r\n" + 
				"		</image>\r\n" + 
				"		<pubDate>Tue, 17 Jul 2012 12:11:00 MST</pubDate>\r\n" + 
				"		<lastBuildDate>Tue, 17 Jul 2012 12:11:00 MST</lastBuildDate>\r\n" + 
				"		<ttl>5</ttl>\r\n" + 
				"		<item>\r\n" + 
				"			<guid isPermaLink=\"false\">1342552299</guid>\r\n" + 
				"			<title>Current Conditions at Show Low Municipal Airport SOW in AZ : 80.0F / 26.7C, Clear - 12:11 PM MST Jul.\r\n" + 
				"				17\r\n" + 
				"			</title>\r\n" + 
				"			<link>http://www.wunderground.com/US/AZ/Show_Low.html</link>\r\n" + 
				"			<description><![CDATA[Temperature: 80.0&deg;F / 26.7&deg;C | Humidity: 35% | Pressure: 30.27in / 1025hPa (Rising) | Conditions: Clear | Wind Direction: WSW | Wind Speed: 3.0mph / 4.8km/h<img src=\"http://server.as5000.com/AS5000/adserver/image?ID=WUND-00070&C=0\" width=\"0\" height=\"0\" border=\"0\"/>]]>\r\n" + 
				"			</description>\r\n" + 
				"			<pubDate>Tue, 17 Jul 2012 12:11:00 MST</pubDate>\r\n" + 
				"		</item>\r\n" + 
				"\r\n" + 
				"		<item>\r\n" + 
				"			<title>Tuesday as of Jul. 17 8:00 AM MST</title>\r\n" + 
				"			<link>http://www.wunderground.com/US/AZ/Show_Low.html</link>\r\n" + 
				"			<description><![CDATA[\r\n" + 
				"	Tuesday - Partly cloudy with a chance of a thunderstorm and a chance of rain. High of 84F. Winds from the SW at 10 to 15 mph. Chance of rain 40%.\r\n" + 
				"      ]]></description>\r\n" + 
				"			<pubDate>Tue, 17 Jul 2012 08:00:00 MST</pubDate>\r\n" + 
				"			<guid isPermaLink=\"false\">1342537200-1</guid>\r\n" + 
				"		</item>\r\n" + 
				"		<item>\r\n" + 
				"			<title>Tuesday Night as of Jul. 17 8:00 AM MST</title>\r\n" + 
				"			<link>http://www.wunderground.com/US/AZ/Show_Low.html</link>\r\n" + 
				"			<description><![CDATA[\r\n" + 
				"	Tuesday Night - Partly cloudy with a chance of a thunderstorm and a chance of rain. Low of 52F. Winds from the SSW at 5 to 15 mph. Chance of rain 30%.\r\n" + 
				"      ]]></description>\r\n" + 
				"			<pubDate>Tue, 17 Jul 2012 08:00:00 MST</pubDate>\r\n" + 
				"			<guid isPermaLink=\"false\">1342537200-2</guid>\r\n" + 
				"		</item>\r\n" + 
				"		<item>\r\n" + 
				"			<title>Wednesday as of Jul. 17 8:00 AM MST</title>\r\n" + 
				"			<link>http://www.wunderground.com/US/AZ/Show_Low.html</link>\r\n" + 
				"			<description><![CDATA[\r\n" + 
				"	Wednesday - Partly cloudy with a chance of a thunderstorm and a chance of rain. High of 88F. Winds from the WSW at 5 to 10 mph. Chance of rain 30%.\r\n" + 
				"      ]]></description>\r\n" + 
				"			<pubDate>Tue, 17 Jul 2012 08:00:00 MST</pubDate>\r\n" + 
				"			<guid isPermaLink=\"false\">1342537200-3</guid>\r\n" + 
				"		</item>\r\n" + 
				"	</channel>\r\n" + 
				"</rss>";
	}

    public static String getTampaRecord() {
    	return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + 
    			"<rss xmlns:atom=\"http://www.w3.org/2005/Atom\" version=\"2.0\">\r\n" + 
    			"	<channel>\r\n" + 
    			"		<title>www2.tbo.com - News</title>\r\n" + 
    			"		<link>http://www2.tbo.com/news/</link>\r\n" + 
    			"		<description></description>\r\n" + 
    			"		<atom:link href=\"http://www2.tbo.com/feed/rss/news/\" rel=\"self\"></atom:link>\r\n" + 
    			"		<language>en-us</language>\r\n" + 
    			"		<copyright>Copyright (c) 2012, Media General Communications Holdings,\r\n" + 
    			"			LLC. A Media General company.</copyright>\r\n" + 
    			"		<lastBuildDate>Tue, 17 Jul 2012 13:03:04 -0400</lastBuildDate>\r\n" + 
    			"		<item>\r\n" + 
    			"			<title>Body of Staff Sgt. Ricardo Seija returns home</title>\r\n" + 
    			"			<link>http://www2.tbo.com/news/news/2012/jul/17/7/body-of-staff-sgt-ricardo-seija-returns-home-ar-433878/</link>\r\n" + 
    			"			<description>Body of Staff Sgt. Ricardo Seija returns home</description>\r\n" + 
    			"			<pubDate>Tue, 17 Jul 2012 13:03:04 -0400</pubDate>\r\n" + 
    			"			<guid>http://www2.tbo.com/news/news/2012/jul/17/7/body-of-staff-sgt-ricardo-seija-returns-home-ar-433878/</guid>\r\n" + 
    			"			<enclosure\r\n" + 
    			"				url=\"http://www2.tbo.com/mgmedia/image/100/100/217458/img-20120717-02368/\"\r\n" + 
    			"				length=\"30000\" type=\"image/jpeg\"></enclosure>\r\n" + 
    			"		</item>\r\n" + 
    			"		<item>\r\n" + 
    			"			<title>Sears steps down after years of government service</title>\r\n" + 
    			"			<link>http://www2.tbo.com/news/plant-city/2012/jul/17/sears-steps-down-after-years-of-government-service-ar-429159/</link>\r\n" + 
    			"			<description>Sears steps down after years of government service</description>\r\n" + 
    			"			<pubDate>Tue, 17 Jul 2012 13:00:00 -0400</pubDate>\r\n" + 
    			"			<guid>http://www2.tbo.com/news/plant-city/2012/jul/17/sears-steps-down-after-years-of-government-service-ar-429159/</guid>\r\n" + 
    			"			<enclosure\r\n" + 
    			"				url=\"http://www2.tbo.com/mgmedia/image/100/100/216697/pcsears18jpg/\"\r\n" + 
    			"				length=\"30000\" type=\"image/jpeg\"></enclosure>\r\n" + 
    			"		</item>\r\n" + 
    			"		<item>\r\n" + 
    			"			<title>Spirit Airlines to offer non-stop service from Tampa to\r\n" + 
    			"				Chicago, Airport TPA in FL</title>\r\n" + 
    			"			<link>http://www2.tbo.com/news/business/2012/jul/17/spirit-airlines-to-offer-non-stop-service-from-tam-ar-433900/</link>\r\n" + 
    			"			<description>Spirit Airlines to offer non-stop service from Tampa to\r\n" + 
    			"				Chicago</description>\r\n" + 
    			"			<pubDate>Tue, 17 Jul 2012 12:48:10 -0400</pubDate>\r\n" + 
    			"			<guid>http://www2.tbo.com/news/business/2012/jul/17/spirit-airlines-to-offer-non-stop-service-from-tam-ar-433900/</guid>\r\n" + 
    			"		</item>\r\n" + 
    			"		<item>\r\n" + 
    			"			<title>More than workers</title>\r\n" + 
    			"			<link>http://www2.tbo.com/news/opinion/2012/jul/17/naopino2-more-than-workers-ar-433254/</link>\r\n" + 
    			"			<description>More than workers</description>\r\n" + 
    			"			<pubDate>Tue, 17 Jul 2012 00:00:00 -0400</pubDate>\r\n" + 
    			"			<guid>http://www2.tbo.com/news/opinion/2012/jul/17/naopino2-more-than-workers-ar-433254/</guid>\r\n" + 
    			"			<enclosure\r\n" + 
    			"				url=\"http://www2.tbo.com/mgmedia/image/100/100/217388/ed-school/\"\r\n" + 
    			"				length=\"30000\" type=\"image/jpeg\"></enclosure>\r\n" + 
    			"		</item>\r\n" + 
    			"	</channel>\r\n" + 
    			"</rss>";
    }
    
    // from http://docs.oracle.com/cd/B19306_01/appdev.102/b14259/xdb09sea.htm
    public static String getPurchaseOrderRecord() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                "<purchaseOrder xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" + 
                "               xsi:noNamespaceSchemaLocation=\"xmlschema/po.xsd\" \n" + 
                "               orderDate=\"1999-10-20\">\n" + 
                "  <shipTo country=\"US\">\n" + 
                "    <name>Alice Smith</name>\n" + 
                "    <street>123 Maple Street</street>\n" + 
                "    <city>Mill Valley</city>\n" + 
                "    <state>CA</state>\n" + 
                "    <zip>90952</zip>\n" + 
                "  </shipTo>\n" + 
                "  <billTo country=\"US\">\n" + 
                "    <name>Robert Smith</name>\n" + 
                "    <street>8 Oak Avenue</street>\n" + 
                "    <city>Old Town</city>\n" + 
                "    <state>PA</state>\n" + 
                "    <zip>95819</zip>\n" + 
                "  </billTo>\n" + 
                "  <comment>Hurry, my lawn is going wild!</comment>\n" + 
                "  <items>\n" + 
                "    <item partNum=\"872-AA\" a=\"b\">\n" + 
                "      <productName>Lawnmower</productName>\n" + 
                "      <quantity>1</quantity>\n" + 
                "      <USPrice>148.95</USPrice>\n" + 
                "      <comment>Confirm this is electric</comment>\n" + 
                "    </item>\n" + 
                "    <item partNum=\"926-AA\">\n" + 
                "      <productName>Baby Monitor</productName>\n" + 
                "      <quantity>1</quantity>\n" + 
                "      <USPrice>39.98</USPrice>\n" + 
                "      <shipDate>1999-05-21</shipDate>\n" + 
                "    </item>\n" + 
                "  </items>\n" + 
                "</purchaseOrder>";
    }
    
    // @formatter:on
}
