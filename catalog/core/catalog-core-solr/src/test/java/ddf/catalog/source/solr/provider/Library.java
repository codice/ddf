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
package ddf.catalog.source.solr.provider;

public class Library {

  /** */
  private static final long serialVersionUID = -7160191237539812384L;

  public static final String PURCHASE_ORDER_QUERY_PHRASE = "Lawnmower";
  public static final String FLAGSTAFF_QUERY_PHRASE = "Flagstaff";
  public static final String TAMPA_QUERY_PHRASE = "Tampa";
  public static final String AIRPORT_QUERY_PHRASE = "Airport";
  public static final String WEST_USA_CONTAINING_POLYGON_WKT =
      "POLYGON ((-125 49, -125 30, -100 30, -100 49, -125 49))";
  public static final String ARIZONA_POLYGON_WKT =
      "POLYGON ((-114.52062730304343 33.02770735822419, -114.55908930307925 33.03678235823264, -114.6099253031266 33.027002358223534, -114.63396730314898 33.03356735822965, -114.6451593031594 33.044412358239754, -114.66395130317692 33.038922358234636, -114.71135530322107 33.09538235828722, -114.70946330321931 33.122375358312354, -114.6781203031901 33.16725035835415, -114.6800513031919 33.224595358407555, -114.68771130319904 33.23925835842121, -114.67769330318971 33.268016358447994, -114.73542730324348 33.3057083584831, -114.70360330321384 33.352418358526606, -114.7249363032337 33.41105935858121, -114.64509230315934 33.419116358588724, -114.63057330314584 33.439425358607636, -114.621089303137 33.468599358634805, -114.59808630311556 33.48612735865113, -114.5870613031053 33.50944535867285, -114.52942030305162 33.56007335872, -114.5402473030617 33.58050735873903, -114.52717030304953 33.622136358777794, -114.52526330304775 33.66550435881818, -114.53643330305815 33.682735358834236, -114.49567630302019 33.70836935885811, -114.5102873030338 33.74320035889055, -114.50455830302846 33.7717143589171, -114.5211223030439 33.82603135896769, -114.51172230303513 33.84196535898253, -114.52096230304375 33.862926359002046, -114.49818830302253 33.925036359059895, -114.5256323030481 33.95241335908539, -114.51820830304118 33.96506335909717, -114.42898030295808 34.02984435915751, -114.42402930295347 34.07833235920266, -114.41016630294055 34.10265435922531, -114.32279930285918 34.1412973592613, -114.28536830282434 34.17123135928918, -114.23577630277813 34.186222359303144, -114.14991230269818 34.266979359378354, -114.12523030267519 34.272621359383606, -114.13412730268348 34.31454835942266, -114.15341530270143 34.33644735944305, -114.18208030272814 34.36520635946984, -114.2578423027987 34.40548835950735, -114.2833943028225 34.41206935951348, -114.30286530284062 34.43575435953554, -114.33263630286835 34.454873359553346, -114.3765073029092 34.45967935955782, -114.38386230291606 34.47708535957403, -114.3768283029095 34.536563359629426, -114.40974230294016 34.58372335967334, -114.43430230296303 34.59896335968754, -114.42227030295183 34.61089535969865, -114.46563730299222 34.70987335979083, -114.49780430302218 34.74475735982332, -114.52555330304801 34.74891135982719, -114.54204030306337 34.759958359837476, -114.5702173030896 34.83186035990444, -114.62726330314274 34.875533359945116, -114.63047530314574 34.919501359986064, -114.62100730313692 34.943609360008516, -114.63227630314742 34.997651360058846, -114.62106830313698 34.99891436006002, -114.63378030314881 35.041863360100024, -114.59563230311329 35.07605836013187, -114.6359093031508 35.11865536017154, -114.62644130314197 35.13390636018575, -114.58261630310116 35.132560360184485, -114.57225530309151 35.14006736019148, -114.56104030308107 35.17434636022341, -114.5595833030797 35.22018336026609, -114.58789030310608 35.304768360344866, -114.58958430310764 35.358378360394795, -114.64539630315963 35.450760360480835, -114.6722153031846 35.515754360541365, -114.64979230316374 35.54663736057013, -114.65313430316684 35.5848333606057, -114.63986630315449 35.611348360630394, -114.65406630316771 35.64658436066321, -114.66848630318114 35.65639936067235, -114.66509130317797 35.69309936070653, -114.68882030320007 35.73259536074332, -114.68273930319441 35.76470336077322, -114.68986730320105 35.84744236085027, -114.66246230317552 35.870960360872175, -114.66160030317472 35.88047336088104, -114.69927630320981 35.91161236091004, -114.7362123032442 35.987648360980856, -114.71767330322695 36.036758361026585, -114.72896630323746 36.058753361047074, -114.7281503032367 36.08596236107242, -114.71276130322238 36.10518136109032, -114.62161030313749 36.141966361124574, -114.59893530311636 36.13833536112119, -114.5305733030527 36.15509036113679, -114.46661330299312 36.1247113611085, -114.44394530297203 36.1210533611051, -114.38080330291321 36.15099136113298, -114.34423430287916 36.137480361120396, -114.31609530285294 36.11143836109614, -114.30385730284155 36.08710836107348, -114.30758730284502 36.06223336105032, -114.233472302776 36.01833136100943, -114.20676930275113 36.017255361008424, -114.12902330267872 36.04173036103122, -114.10777530265894 36.12109036110513, -114.04510530260056 36.19397836117301, -114.03739230259339 36.21602336119354, -114.04371630259928 36.84184936177639, -114.04393930259948 36.99653836192046, -112.89998330153409 36.99622736192016, -112.54252130120118 36.99799436192181, -112.23725830091688 36.995492361919474, -111.3561643000963 37.001709361925265, -110.7400632995225 37.002488361926, -110.48408929928411 37.003926361927334, -110.45223629925445 36.991746361915986, -109.99707629883055 36.99206736191629, -109.0484802979471 36.99664136192055, -109.0478462979465 35.99666436098925, -109.04664129794538 34.95464636001879, -109.04865229794726 34.59178035968085, -109.05034929794884 33.7833023589279, -109.050526297949 33.20516435838946, -109.05134629794976 32.779550357993074, -109.04949529794804 32.44204435767875, -109.04561529794442 31.34345335665561, -110.45257829925477 31.33766035665021, -111.07196429983162 31.335634356648328, -111.36952130010873 31.431531356737636, -113.32911130193375 32.04362135730769, -114.82176130332388 32.487169357720774, -114.80939430331236 32.6160443578408, -114.72204930323102 32.720857357938414, -114.71269530322232 32.7350133579516, -114.69404030320493 32.74142535795757, -114.60394230312102 32.72628535794347, -114.60352230312063 32.73588635795241, -114.57195930309123 32.73743935795386, -114.57221030309148 32.74882935796447, -114.56075130308079 32.74893635796457, -114.56158230308156 32.760753357975574, -114.54300430306427 32.76074935797557, -114.54318730306444 32.77123235798533, -114.53009530305225 32.7714113579855, -114.53507730305688 32.788047358000995, -114.52621930304863 32.80991235802135, -114.4614363029883 32.84542235805443, -114.47644430300228 32.9359083581387, -114.46838730299478 32.9777893581777, -114.52062730304343 33.02770735822419))";
  public static final String ARIZONA_INTERSECTING_MULTIPOLYGON_WKT =
      "MULTIPOLYGON (((-116.26171901822 34.658206701279, -113.80078151822 38.261722326279, -110.15332058072 35.625003576279, -110.06542995572 33.251956701279, -113.97656276822 32.812503576279, -116.26171901822 34.658206701279)), ((-117.88085950283 35.588624751196, -117.66113294033 40.554445063696, -120.60546887783 37.654054438696, -117.88085950283 35.588624751196)))";
  public static final String ARIZONA_INTERSECTING_POLYGON_WKT =
      "POLYGON ((-116.26171901822 34.658206701279, -113.80078151822 38.261722326279, -110.15332058072 35.625003576279, -110.06542995572 33.251956701279, -113.97656276822 32.812503576279, -116.26171901822 34.658206701279))";
  public static final String PHOENIX_AND_LAS_VEGAS_MULTIPOINT_WKT =
      "MULTIPOINT ((-112.066667 33.45), (-115.136389 36.175))";
  public static final String FLAGSTAFF_AIRPORT_POINT_WKT =
      "POINT (-111.67121887207031 35.138454437255859)";
  public static final String ARIZONA_INTERSECTING_MULTILINESTING_WKT =
      "MULTILINESTRING ((-115.33642578125 33.28662109375,-108.17333984375 35.83544921875), (-119.15527356533 36.906984126196, -114.40917981533 39.455812251196, -117.22167981533 39.719484126196, -117.26562512783 39.719484126196))";
  public static final String ARIZONA_INTERSECTING_LINESTING_WKT =
      "LINESTRING (-115.33642578125 33.28662109375,-108.17333984375 35.83544921875)";
  public static final String ARIZONA_INTERSECTING_GEOMETRYCOLLECTION_WKT =
      "GEOMETRYCOLLECTION ("
          + FLAGSTAFF_AIRPORT_POINT_WKT
          + ", "
          + ARIZONA_INTERSECTING_LINESTING_WKT
          + ", "
          + ARIZONA_INTERSECTING_MULTIPOLYGON_WKT
          + ")";
  public static final String CLOCKWISE_ARIZONA_RECTANGLE_WKT =
      "POLYGON ((-115.72998046874625 30.921076375385542, -115.72998046874625 37.47485808497204, -108.12744140624321 37.47485808497204, -108.12744140624321 30.921076375385542, -115.72998046874625 30.921076375385542))";
  public static final String COUNTERCLOCKWISE_ARIZONA_RECTANGLE_WKT =
      "POLYGON ((-108.08349609374837 30.90222470517274, -108.08349609374837 37.45741810263027, -115.70800781249432 37.45741810263027, -115.70800781249432 30.90222470517274, -108.08349609374837 30.90222470517274))";
  public static final String PHOENIX_POINT_WKT = "POINT (-112.066667 33.45)";
  public static final String ACROSS_INTERNATIONAL_DATELINE_SMALL_WKT =
      "POLYGON ((179.5 30, 179.5 29, -179.5 29, -179.5 30, 179.5 30))";
  public static final String ACROSS_INTERNATIONAL_DATELINE_LARGE_CW_WKT =
      "POLYGON ((175 30, -175 30, -175 25, 175 25, 175 30))";
  public static final String ACROSS_INTERNATIONAL_DATELINE_LARGE_CCW_WKT =
      "POLYGON ((175 30, 175 25, -175 25, -175 30, 175 30))";
  public static final String MIDWAY_ISLANDS_POINT_WKT = "POINT (-177.372736 28.208365)";
  public static final String LAS_VEGAS_POINT_WKT = "POINT (-115.136389 36.175)";
  public static final String ARABIAN_SEA_POINT_WKT = "POINT (62.5 14)";
  public static final String ARABIAN_SEA_OVERLAPPING_GEOMETRYCOLLECTION_WKT =
      "GEOMETRYCOLLECTION (MULTIPOLYGON (((56 9, 64 9, 60 14, 56 9)), ((61 9, 69 9, 65 14, 61 9)), ((51 9, 59 9, 55 14, 51 9))), LINESTRING (50 8, 50 15, 70 15, 70 8, 50 8), MULTIPOINT ((62.5 14), (67.5 14), (57.5 14), (52.5 14)))";
  public static final String GULF_OF_GUINEA_MULTIPOINT_SINGLE_WKT = "MULTIPOINT ((1 1))";
  public static final String GULF_OF_GUINEA_MULTIPOINT_WKT = "MULTIPOINT ((1 1), (0 0), (2 2))";
  public static final String GULF_OF_GUINEA_POINT_WKT = "POINT (1 1)";
  public static final String GULF_OF_GUINEA_MULTILINESTRING_WKT =
      "MULTILINESTRING ((1 1, 2 1), (1 2, 0 0))";
  public static final String GULF_OF_GUINEA_LINESTRING_WKT = "LINESTRING (1 1,2 1)";
  public static final String GULF_OF_GUINEA_MULTIPOLYGON_WKT =
      "MULTIPOLYGON (((1 1,2 1,2 2,1 2,1 1)), ((0 0,1 1,2 0,0 0)))";
  public static final String GULF_OF_GUINEA_GEOMETRYCOLLECTION_WKT =
      "GEOMETRYCOLLECTION ("
          + GULF_OF_GUINEA_POINT_WKT
          + ", "
          + GULF_OF_GUINEA_LINESTRING_WKT
          + ", "
          + GULF_OF_GUINEA_MULTIPOLYGON_WKT
          + ")";
  public static final String GULF_OF_GUINEA_POLYGON_WKT = "POLYGON ((1 1,2 1,2 2,1 2,1 1))";
  public static final String TAMPA_AIRPORT_POINT_WKT =
      "POINT (-82.533248901367188 27.975471496582031)";
  public static final String SHOW_LOW_AIRPORT_POINT_WKT =
      "POINT (-110.00540924072266 34.265270233154297)";

  public Library() {}

  // @formatter:off
  public static String getIndexingRecord() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
        + "<rss version=\"2.0\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"\r\n"
        + "    xmlns:wfw=\"http://wellformedweb.org/CommentAPI/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\r\n"
        + "    xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:sy=\"http://purl.org/rss/1.0/modules/syndication/\"\r\n"
        + "    xmlns:slash=\"http://purl.org/rss/1.0/modules/slash/\" xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\"\r\n"
        + "    xmlns:rawvoice=\"http://www.rawvoice.com/rawvoiceRssModule/\">\r\n"
        + "\r\n"
        + "    <channel>\r\n"
        + "        <title><!--description-->content text<![CDATA[<greeting>Hello</greeting>]]>other content</title>"
        + "        <atom:link title=\"Flagstaff atom feed\" href=\"http://www.flagstaffchamber.com/feed/\" rel=\"self\"\r\n"
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

  public static String getShowLowRecord() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
        + "<rss version=\"2.0\">\r\n"
        + "    <channel>\r\n"
        + "        <title>Show Low, AZ Weather from Weather Underground</title>\r\n"
        + "        <link>http://www.wunderground.com/</link>\r\n"
        + "        <description>Weather Underground RSS Feed for Show Low, AZ US\r\n"
        + "        </description>\r\n"
        + "        <language>EN</language>\r\n"
        + "        <generator>WU-RSS</generator>\r\n"
        + "        <webMaster>support@wunderground.com (Wunderground Support)</webMaster>\r\n"
        + "        <category>weather</category>\r\n"
        + "        <image>\r\n"
        + "            <url>http://icons.wunderground.com/graphics/smash/wunderTransparent.gif\r\n"
        + "            </url>\r\n"
        + "            <link>http://www.wunderground.com/</link>\r\n"
        + "            <title>Show Low, AZ Weather from Weather Underground</title>\r\n"
        + "        </image>\r\n"
        + "        <pubDate>Tue, 17 Jul 2012 12:11:00 MST</pubDate>\r\n"
        + "        <lastBuildDate>Tue, 17 Jul 2012 12:11:00 MST</lastBuildDate>\r\n"
        + "        <ttl>5</ttl>\r\n"
        + "        <item>\r\n"
        + "            <guid isPermaLink=\"false\">1342552299</guid>\r\n"
        + "            <title>Current Conditions at Show Low Municipal Airport SOW in AZ : 80.0F / 26.7C, Clear - 12:11 PM MST Jul.\r\n"
        + "                17\r\n"
        + "            </title>\r\n"
        + "            <link>http://www.wunderground.com/US/AZ/Show_Low.html</link>\r\n"
        + "            <description><![CDATA[Temperature: 80.0&deg;F / 26.7&deg;C | Humidity: 35% | Pressure: 30.27in / 1025hPa (Rising) | Conditions: Clear | Wind Direction: WSW | Wind Speed: 3.0mph / 4.8km/h<img src=\"http://server.as5000.com/AS5000/adserver/image?ID=WUND-00070&C=0\" width=\"0\" height=\"0\" border=\"0\"/>]]>\r\n"
        + "            </description>\r\n"
        + "            <pubDate>Tue, 17 Jul 2012 12:11:00 MST</pubDate>\r\n"
        + "        </item>\r\n"
        + "\r\n"
        + "        <item>\r\n"
        + "            <title>Tuesday as of Jul. 17 8:00 AM MST</title>\r\n"
        + "            <link>http://www.wunderground.com/US/AZ/Show_Low.html</link>\r\n"
        + "            <description><![CDATA[\r\n"
        + "    Tuesday - Partly cloudy with a chance of a thunderstorm and a chance of rain. High of 84F. Winds from the SW at 10 to 15 mph. Chance of rain 40%.\r\n"
        + "      ]]></description>\r\n"
        + "            <pubDate>Tue, 17 Jul 2012 08:00:00 MST</pubDate>\r\n"
        + "            <guid isPermaLink=\"false\">1342537200-1</guid>\r\n"
        + "        </item>\r\n"
        + "        <item>\r\n"
        + "            <title>Tuesday Night as of Jul. 17 8:00 AM MST</title>\r\n"
        + "            <link>http://www.wunderground.com/US/AZ/Show_Low.html</link>\r\n"
        + "            <description><![CDATA[\r\n"
        + "    Tuesday Night - Partly cloudy with a chance of a thunderstorm and a chance of rain. Low of 52F. Winds from the SSW at 5 to 15 mph. Chance of rain 30%.\r\n"
        + "      ]]></description>\r\n"
        + "            <pubDate>Tue, 17 Jul 2012 08:00:00 MST</pubDate>\r\n"
        + "            <guid isPermaLink=\"false\">1342537200-2</guid>\r\n"
        + "        </item>\r\n"
        + "        <item>\r\n"
        + "            <title>Wednesday as of Jul. 17 8:00 AM MST</title>\r\n"
        + "            <link>http://www.wunderground.com/US/AZ/Show_Low.html</link>\r\n"
        + "            <description><![CDATA[\r\n"
        + "    Wednesday - Partly cloudy with a chance of a thunderstorm and a chance of rain. High of 88F. Winds from the WSW at 5 to 10 mph. Chance of rain 30%.\r\n"
        + "      ]]></description>\r\n"
        + "            <pubDate>Tue, 17 Jul 2012 08:00:00 MST</pubDate>\r\n"
        + "            <guid isPermaLink=\"false\">1342537200-3</guid>\r\n"
        + "        </item>\r\n"
        + "    </channel>\r\n"
        + "</rss>";
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

  // from http://docs.oracle.com/cd/B19306_01/appdev.102/b14259/xdb09sea.htm
  public static String getPurchaseOrderRecord() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<purchaseOrder xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
        + "               xsi:noNamespaceSchemaLocation=\"xmlschema/po.xsd\" \n"
        + "               orderDate=\"1999-10-20\">\n"
        + "  <shipTo country=\"US\">\n"
        + "    <name>Alice Smith</name>\n"
        + "    <street>123 Maple Street</street>\n"
        + "    <city>Mill Valley</city>\n"
        + "    <state>CA</state>\n"
        + "    <zip>90952</zip>\n"
        + "  </shipTo>\n"
        + "  <billTo country=\"US\">\n"
        + "    <name>Robert Smith</name>\n"
        + "    <street>8 Oak Avenue</street>\n"
        + "    <city>Old Town</city>\n"
        + "    <state>PA</state>\n"
        + "    <zip>95819</zip>\n"
        + "  </billTo>\n"
        + "  <comment>Hurry, my lawn is going wild!</comment>\n"
        + "  <items>\n"
        + "    <item partNum=\"872-AA\" a=\"b\">\n"
        + "      <productName>Lawnmower</productName>\n"
        + "      <quantity>1</quantity>\n"
        + "      <USPrice>148.95</USPrice>\n"
        + "      <comment>Confirm this is electric</comment>\n"
        + "    </item>\n"
        + "    <item partNum=\"926-AA\">\n"
        + "      <productName>Baby Monitor</productName>\n"
        + "      <quantity>1</quantity>\n"
        + "      <USPrice>39.98</USPrice>\n"
        + "      <shipDate>1999-05-21</shipDate>\n"
        + "    </item>\n"
        + "  </items>\n"
        + "</purchaseOrder>";
  }

  // @formatter:on
}
