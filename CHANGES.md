# Changes between Versions
<!-- Template: Copy and uncomment-->
<!-- ## VERSION NUMBER
	Release Date: `unreleased`
replace with next unreleased version

### NOTES

- Summary of changes requiring user action.

### NEW FEATURES

- List of features added

	- [JIRA View of All New Features](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20issuetype%20in%20%28%22new%20feature%22%2C%20improvement%2C%20story%2C%20task%29%20AND%20fixVersion%20%3D%20VERSION_NUMBER%20ORDER%20BY%20priority)
	- [Configurable retry intervals for unavailable Catalog Sources](https://codice.atlassian.net/browse/DDF-2831)

### API CHANGES

- None in this version

### BUG FIXES
(update {VERSION-NUMBER} in JIRA query

- Important issues resolved
	- [JIRA View of All Issues Resolved](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20resolution%20%3D%20Fixed%20AND%20fixVersion%20%3D%20VERSION_NUMBER%20ORDER%20BY%20priority) in this version.

### KNOWN ISSUES
(update {VERSION-NUMBER} in JIRA query


- Open bugs affecting this version
	- [JIRA View of Open Issues](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20issuetype%20%3D%20bug%20%20AND%20status%20%3D%20Open%20AND%20affectedVersion%20%3D%20VERSION_NUMBER%20AND%20fixVersion%20!%3D%20VERSION_NUMBER%20ORDER%20BY%20priority) affecting this version.
 -->

## Version 2.11.0
	Release Date: pending
_This is a preview of an unreleased version and subject to change._
<h2>        Bug
</h2>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-1802'>DDF-1802</a> -         Metacards with GeometryCollection cause a cometd listener exception when rendered on both 2D and 3D maps
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2192'>DDF-2192</a> -         Gazetteer service cannot handle large WKT
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2431'>DDF-2431</a> -         Update/Delete will not finish if a source does not respond in a timely manner
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2519'>DDF-2519</a> -         Fix NumberFormatException when a non-required metatype field is blank
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2563'>DDF-2563</a> -         Path lengths in the distribution cause it to unzip improperly using windows&#39; built-in archive tool, forcing administrators to use a third-party tool
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2643'>DDF-2643</a> -         TikaInputTransformer ingest fails on large files
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2650'>DDF-2650</a> -         An invalid character in the URI could cause a content action to fail
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2778'>DDF-2778</a> -         WMTS Imagery Providers do not project EPSG:4326 properly for Cesium
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2786'>DDF-2786</a> -         Depending on the zoom level, system header and footer are sometimes cut off in the Catalog UI
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2788'>DDF-2788</a> -         An improperly constructed GET request for a WSDL can cause an exception in the PEPAuthorizingInterceptor and keep it from logging the attempt
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2789'>DDF-2789</a> -         The source poller can stop working when a source is removed because of a race condition with blueprint which causes sources to stop updating with new status
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2790'>DDF-2790</a> -         Large Usernames in the Data Usage Admin App push table columns off the screen, causing erroneous behavior
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2797'>DDF-2797</a> -         HTTP Proxy destroy method causes an exception when stopping endpoints which causes the Camel context to not shut down correctly
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2809'>DDF-2809</a> -         URLs with extremely long paths can cause an exception in the web context policy manager
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2835'>DDF-2835</a> -         CSW queries using extended attributes return 0 results, forcing integrators to use a different endpoint for advanced searches
</li>
</ul>
        
<h2>        Story
</h2>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-1765'>DDF-1765</a> -         As a user, I want to add a layer to the Search UI map from a KML file so that I can visualize my KML data
</li>
</ul>
    
<h2>        New Feature
</h2>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-2039'>DDF-2039</a> -         As an integrator, I want to make a single request to the catalog framework to get back both the metacard and the resource.
</li>
</ul>
    
<h2>        Task
</h2>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-2698'>DDF-2698</a> -         Create a catalog:export command
</li>
</ul>
    
<h2>        Improvement
</h2>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-1882'>DDF-1882</a> -         Support XPath pre-filtering with Solr 5
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2241'>DDF-2241</a> -         Create a Historian user for all history actions
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2572'>DDF-2572</a> -         Create ProcessingFramework Default (In Memory) Implementation
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2573'>DDF-2573</a> -         Create ProcessingPostIngestPlugin
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2695'>DDF-2695</a> -         Add taxonomy attributes for better normalization
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2715'>DDF-2715</a> -         Make Point of Contact Read-only
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2783'>DDF-2783</a> -         Update thumbnails to allow animation in the summary view
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2791'>DDF-2791</a> -         Update the SolrFilterDelegate to support empty isEqualTo and isLike queries
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2811'>DDF-2811</a> -         Invalid data in workspace metacards causes workspaces page to fail
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2812'>DDF-2812</a> -         Bad workspace data can be manually inserted causing unreliable workspace view
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2815'>DDF-2815</a> -         As an integrator I would like to be able to specify a Geospatial query by Universal Transverse Mercator so that I can express Geospatial queries via Universal Transverse Locator through web services
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2817'>DDF-2817</a> -         HTTP responses from DDF should include a CSP header
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2827'>DDF-2827</a> -         Change the default authentication mechanism in DDF to the IdP
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2849'>DDF-2849</a> -         CQL endpoint does not respect Accept-Encoding header
</li>
</ul>
        
<h2>        Technical Debt
</h2>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-2436'>DDF-2436</a> -         Ignored Catalog Framework unit tests need to be updated 
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2451'>DDF-2451</a> -         Deprecate all the legacy attributes and add a compatibility plugin
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2673'>DDF-2673</a> -         Add iTests to test different query modes of CachingFederationStrategy
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2731'>DDF-2731</a> -         Move PropertiesFileReader where it can be re-used
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2798'>DDF-2798</a> -         There are several possible null pointers within the ECP handlers
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2821'>DDF-2821</a> -         Check audit messages for special characters and encode them before writing the audit
</li>
</ul>
        
<h2>        Spike
</h2>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-2655'>DDF-2655</a> -         Determine if splitting out DDF itests into separate modules provides better memory isolation and garbage cleanup
</li>
</ul>
    
<h2>        Dependency Upgrade
</h2>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-2818'>DDF-2818</a> -         Upgrade to Solr version 6.4.1
</li>
</ul>

## 2.10.2
	Release Date: pending

### NOTES

- Summary of changes requiring user action.

### NEW FEATURES

- List of new features in this version.
	- [JIRA View of All New Features](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20issuetype%20in%20%28%22new%20feature%22%2C%20improvement%2C%20story%2C%20task%29%20AND%20fixVersion%20%3D%20ddf-2.10.2%20ORDER%20BY%20priority) 

### API CHANGES

- None in this version.

### BUG FIXES

- List bugs fixed here.
	- [JIRA View of All Issues Resolved](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20resolution%20%3D%20Fixed%20AND%20fixVersion%20%3D%202.10.2%20ORDER%20BY%20resolutiondate)
        in this version.

### KNOWN ISSUES
- List known issues / or workarounds here.
	- [JIRA View of Open Issues](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20issuetype%20%3D%20bug%20%20AND%20status%20%3D%20Open%20AND%20affectedVersion%20%3D%202.10.1%20AND%20fixVersion%20!%3D%202.10.2%20ORDER%20BY%20priority) affecting this version.

## Version 2.10.1
	Release Date: 2017-03-08
            
<h3>        Bug
</h3>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-2775'>DDF-2775</a> -         In the Catalog UI and the Standard UI, actions that are not supported for a given metacard’s tag type are displayed.
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2790'>DDF-2790</a> -         Large Usernames in the Data Usage Admin App push table columns off the screen, causing erroneous behavior
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2792'>DDF-2792</a> -         Update client side sorting to handle null / blank values more consistently
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2795'>DDF-2795</a> -         The Content Directory Monitor &quot;Attribute Overrides&quot; fails when there is a comma in the attribute
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2797'>DDF-2797</a> -         HTTP Proxy destroy method causes an exception when stopping endpoints which causes the Camel context to not shut down correctly
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2806'>DDF-2806</a> -         Add Jetty dependency to Catalog UI Search pom.xml
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2809'>DDF-2809</a> -         URLs with extremely long paths can cause an exception in the web context policy manager
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2814'>DDF-2814</a> -         Catalog UI area searches fail when the area includes self-intersecting polygons
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2830'>DDF-2830</a> -         Hot deploying KAR files does not work on a production system
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2835'>DDF-2835</a> -         CSW queries using extended attributes return 0 results, forcing integrators to use a different endpoint for advanced searches
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2845'>DDF-2845</a> -         Catalog UI result list help-text doesn&#39;t reflect the attribute name alias
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2850'>DDF-2850</a> -         Catalog UI gazetteer searches fail in 3d view
</li>
</ul>
                
<h3>        Task
</h3>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-2822'>DDF-2822</a> -         Update name for Catalog UI
</li>
</ul>
    
<h3>        Improvement
</h3>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-2208'>DDF-2208</a> -         Remove DDF from app names
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2572'>DDF-2572</a> -         Create ProcessingFramework Default (In Memory) Implementation
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2573'>DDF-2573</a> -         Create ProcessingPostIngestPlugin
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2715'>DDF-2715</a> -         Make Point of Contact Read-only
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2791'>DDF-2791</a> -         Update the SolrFilterDelegate to support empty isEqualTo and isLike queries
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2794'>DDF-2794</a> -         Set registry metacard security markings based on system high/low and user input
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2807'>DDF-2807</a> -         Clean up maven profiles and add quickbuild profile
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2816'>DDF-2816</a> -         Update details view to allow links to be clickable
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2817'>DDF-2817</a> -         HTTP responses from DDF should include a CSP header
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2837'>DDF-2837</a> -         Metacards with attributes that have a null value are not properly marshaled which can result in failed actions and NPEs. 
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2849'>DDF-2849</a> -         CQL endpoint does not respect Accept-Encoding header
</li>
</ul>
    
<h3>        Documentation
</h3>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-2653'>DDF-2653</a> -         document OSGi Basics for contributing developers
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2804'>DDF-2804</a> -         Update hardening guide to recommend 10 minute timeout
</li>
</ul>
    
<h3>        Technical Debt
</h3>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-2785'>DDF-2785</a> -         Saving polygons in catalog UI creates duplicate point
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2798'>DDF-2798</a> -         There are several possible null pointers within the ECP handlers
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2810'>DDF-2810</a> -         Upgrade GeoWebCache from 1.5.0 to 1.9.1
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2821'>DDF-2821</a> -         Check audit messages for special characters and encode them before writing the audit
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2826'>DDF-2826</a> -         Increase default JVM memory to 4096MB
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2836'>DDF-2836</a> -         Upgrade Spark from 2.5 to 2.5.5
</li>
</ul>
            
<h3>        Dependency Upgrade
</h3>
<ul>
<li><a href='https://codice.atlassian.net/browse/DDF-2818'>DDF-2818</a> -         Upgrade to Solr version 6.4.1
</li>
<li><a href='https://codice.atlassian.net/browse/DDF-2833'>DDF-2833</a> -         Upgrade to latest version of Yarn
</li>
</ul>


## 2.10.0
	Release Date: 2017-02-08

### NOTES

- This version will require data to be reindexed.

### NEW FEATURES
- [DDF-2817](https://codice.atlassian.net/browse/DDF-2817) A configurable servlet filter now attaches Content Security Policy, X-XSS-Protection and X-Frame-Options headers to responses
- [DDF-2729](https://codice.atlassian.net/browse/DDF-2729) Added ability for administrators to disable the use of the cache when using the Catalog UI
- [DDF-2535](https://codice.atlassian.net/browse/DDF-2535) Added Confluence Federated Source
- [DDF-2639](https://codice.atlassian.net/browse/DDF-2639) Catalog security plugin to protect resource URIs
- [DDF-2467](https://codice.atlassian.net/browse/DDF-2467) Initial support for Solr Cloud
- [DDF-2413](https://codice.atlassian.net/browse/DDF-2413), [DDF-2388](https://codice.atlassian.net/browse/DDF-2388) Improved support for Expanded metadata taxonomy 
- [DDF-2196](https://codice.atlassian.net/browse/DDF-2196) Separate admin duties via Admin Console
- [DDF-2162](https://codice.atlassian.net/browse/DDF-2162) Add the ability to inject attributes into Metacard Types
- [DDF-2005](https://codice.atlassian.net/browse/DDF-2005) Enabled configuration of the layers utilized by the GeoWebCache app
- [DDF-2260](https://codice.atlassian.net/browse/DDF-2260) Update PDF Input Tranformer to Extract GeoPDF Metadata
- [DDF-2180](https://codice.atlassian.net/browse/DDF-2180) Added support for FTPS on the FTP endpoint
- [DDF-2133](https://codice.atlassian.net/browse/DDF-2133) The FTP endpoint port number is configurable.
- [DDF-2724](https://codice.atlassian.net/browse/DDF-2724) Add the ability to search historical data to the individual query drop down menu in the Catalog UI.
- [DDF-2094](https://codice.atlassian.net/browse/DDF-2094) Add support for decrypting encrypted passwords in all sources
- [DDF-2036](https://codice.atlassian.net/browse/DDF-2036) Provide the option to "drape" an overview of a georeferenced image onto the display map.
- [DDF-2508](https://codice.atlassian.net/browse/DDF-2508) Allowed for the ingest of a resource/metadata through the Catalog REST Endpoint.
- [DDF-2188](https://codice.atlassian.net/browse/DDF-2188) Add service for metadata extraction from text documents
- [DDF-2125](https://codice.atlassian.net/browse/DDF-2125) Add the ability to inject default values into metacards during ingest
	- [JIRA View of All New Features](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20issuetype%20in%20%28%22new%20feature%22%2C%20improvement%2C%20story%2C%20task%29%20AND%20fixVersion%20%3D%20ddf-2.10.0%20ORDER%20BY%20priority) 

### API CHANGES

- None in this version

### BUG FIXES

- [DDF-2547](https://codice.atlassian.net/browse/DDF-2547) Product download fails when using basic auth on a source connected to a non DDF endpoint
- [DDF-2557](https://codice.atlassian.net/browse/DDF-2557) CSW (and possibly other sources) fail to resolve the default URIs
- [DDF-2224](https://codice.atlassian.net/browse/DDF-2224) DDF does not work in offline mode
- [DDF-2506](https://codice.atlassian.net/browse/DDF-2506) Sorting by distance can take minutes on large indexes
- [DDF-2459](https://codice.atlassian.net/browse/DDF-2459) Content directory monitor stops finding new files, requiring administrators to refresh configuration settings to trigger updates
- [DDF-2392](https://codice.atlassian.net/browse/DDF-2392) OpenDJ Embedded LDAP server feature fails on Windows
- [DDF-2316](https://codice.atlassian.net/browse/DDF-2316) Tests and OWASP fail on Windows build
- [DDF-2311](https://codice.atlassian.net/browse/DDF-2311) Web context policies for paths that extend beyond a servlet's context path are not applied
- [DDF-2307](https://codice.atlassian.net/browse/DDF-2307) Saved map layer preferences in the UI are not updated with the most recent URLs from the configuration causing any users with saved layers to lose all map tiles
- [DDF-2297](https://codice.atlassian.net/browse/DDF-2297) Federated queries never timeout or return results in the standard search ui
- [DDF-2290](https://codice.atlassian.net/browse/DDF-2290) Catalog-app features won't start without access to a populated maven repo
- [DDF-2289](https://codice.atlassian.net/browse/DDF-2289) SchematronValidationService should not fail on null/empty metadata, because this erroneously flag metacards as invalid
- [DDF-2222](https://codice.atlassian.net/browse/DDF-2222) PDP does not have the correct mappings set up for expansion services
- [DDF-2189](https://codice.atlassian.net/browse/DDF-2189) Cached resource is deleted when a new search is done
- [DDF-2150](https://codice.atlassian.net/browse/DDF-2150) Windows attribute .cfg files do not get parsed properly in AbstractExpansion
- [DDF-2090](https://codice.atlassian.net/browse/DDF-2090) GeoJSON input transformer doesn't handle multiple values correctly
- [DDF-2339](https://codice.atlassian.net/browse/DDF-2339) Fix typo in CSW Metatype Metacard Mappings
- [DDF-2538](https://codice.atlassian.net/browse/DDF-2538) Updated metacard.handlebars template to properly escape strings
	- [JIRA View of All Issues Resolved](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20resolution%20%3D%20Fixed%20AND%20fixVersion%20%3D%20ddf-2.10.0%20ORDER%20BY%20resolutiondate)
	in this version.

### KNOWN ISSUES
- None
	- [JIRA View of Open Issues](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20issuetype%20%3D%20bug%20%20AND%20status%20%3D%20Open%20AND%20affectedVersion%20%3D%20ddf-2.10.0%20AND%20fixVersion%20!%3D%20ddf-2.10.0%20ORDER%20BY%20priority) affecting this version.

## 2.9.4
    Release Build Date: 2016-12-1

### NOTES

 - None in this version.

### NEW FEATURES

 - None in this version.

### API CHANGES

 - None in this version.

### BUG FIXES

- [DDF-2507](https://codice.atlassian.net/browse/DDF-2507): CSW numerical queries returns no results
	- [JIRA View of All Issues Resolved](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20fixVersion%20%3D%202.9.4%20ORDER%20BY%20resolutiondate) in this version.

### KNOWN ISSUES

 - Open bugs affecting this version
 	- [JIRA View of Open Issues](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20resolution%20%3D%20Unresolved%20AND%20affectedVersion%20%3D%202.9.4%20ORDER%20BY%20createdDate) affecting this version.

## 2.9.3
	Release Date: 2016-10-27

### NOTES

- This version will require data to be reindexed.

### NEW FEATURES

- Introduced change log at [CHANGES.md](https://github.com/codice/ddf/blob/master/CHANGES.md)
- [DDF-2458](https://codice.atlassian.net/browse/DDF-2458) Provide finer grain control of registry write operations.
- [DDF-2415](https://codice.atlassian.net/browse/DDF-2415) Publish/Unpublish to Registry text on the source modal should provide feedback if the action has been done
- [DDF-2430](https://codice.atlassian.net/browse/DDF-2430) As a system admin, I would like the catalog:replicate (and like commands) allow for a temporal argument of last N seconds (or milliseconds) and to specify which time type so that the administrator can issue very specific relative time ranges.
- [DDF-2449](https://codice.atlassian.net/browse/DDF-2449) Remote Nodes in the registry should be sorted alphabetically
- [DDF-2514](https://codice.atlassian.net/browse/DDF-2514) Update coordinate order drop-down descriptions for sources
- [DDF-2457](https://codice.atlassian.net/browse/DDF-2457) Improve the Sources Tab with appropriate and current status of the Sources
- [DDF-2461](https://codice.atlassian.net/browse/DDF-2461) Registry identity node name should use the site name from installer
- [DDF-2279](https://codice.atlassian.net/browse/DDF-2279) Improve the HTTP stub server to support the range header
- [DDF-2391](https://codice.atlassian.net/browse/DDF-2391) The Log Viewer needs to display logs above a certain level
- [DDF-2453](https://codice.atlassian.net/browse/DDF-2453) Prevent the Log Viewer from scrolling when new logs are received
- [DDF-2477](https://codice.atlassian.net/browse/DDF-2477) Infer Default Port Assignment from HTTP/S Ports in System Configuration Settings
- [DDF-2348](https://codice.atlassian.net/browse/DDF-2348) dateTime slots in Registry Metacard XML should have the xs namespace
- [DDF-2445](https://codice.atlassian.net/browse/DDF-2445) Trash icon for deleting remote registry nodes should verify before deleting
- [DDF-2420](https://codice.atlassian.net/browse/DDF-2420) Local Site should not show up as disabled in the Sources tab

### API CHANGES

- None in this version.

### BUG FIXES

- [DDF-2506](https://codice.atlassian.net/browse/DDF-2506) Sorting by distance can take minutes.
- [DDF-2459](https://codice.atlassian.net/browse/DDF-2459) Content directory monitor stops finding new files, requiring administrators to refresh configuration settings to trigger updates
- [DDF-2480](https://codice.atlassian.net/browse/DDF-2480) Search UI upload dialog cannot be re-opened if closed when file upload in progress
- [DDF-2465](https://codice.atlassian.net/browse/DDF-2465) If a source or type gets added, removed, or changed, the Search UI will throw an error
- [DDF-2345](https://codice.atlassian.net/browse/DDF-2345) Registry Node Information screen will not display information if an invalid date is in any node's information
- [DDF-2455](https://codice.atlassian.net/browse/DDF-2455) SchematronValidationService incorrectly appears as a Source in the Sources Admin UI tab
- [DDF-2349](https://codice.atlassian.net/browse/DDF-2349) Percentages in the Ingest Modal on the Standard Search UI show up as NaN when they should be 0%
- [DDF-2474](https://codice.atlassian.net/browse/DDF-2474) Ingesting a large zip file containing metacards/content ingests only some of the data
- [DDF-2438](https://codice.atlassian.net/browse/DDF-2438) Configuring a registry in loopback mode (a client to itself) causes the identity node to be duplicated.
- [DDF-1789](https://codice.atlassian.net/browse/DDF-1789) Empty query is created in Workspace if '< Workspace' is clicked instead of cancel button
- [DDF-2446](https://codice.atlassian.net/browse/DDF-2446) Checked items in Search UI's Specific Sources drop-down get out of sync with label and makes federated searches confusing
- [DDF-2434](https://codice.atlassian.net/browse/DDF-2434) Federated opensearch source url isn't populated correctly when it is generated by the registry
- [DDF-2171](https://codice.atlassian.net/browse/DDF-2171) In the Standard Search UI faceted search view, submitting a textual search with an empty value causes an exception in the search endpoint
	- [JIRA View of All Issues Resolved](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20resolution%20%3D%20Fixed%20AND%20fixVersion%20%3D%20ddf-2.9.3%20ORDER%20BY%20resolutiondate) in this version.

### KNOWN ISSUES

- None
	- [JIRA View of Open Issues](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20issuetype%20%3D%20bug%20%20AND%20status%20%3D%20Open%20AND%20affectedVersion%20%3D%20ddf-2.9.3%20AND%20fixVersion%20!%3D%20ddf-2.9.3%20ORDER%20BY%20priority) affecting this version.

## 2.9.2
	Release Date: 2016-08-26

### NEW FEATURES

- [DDF-2282](https://codice.atlassian.net/browse/DDF-2282) Updated `FederationAdminService` to directly query remote registry instead of using the catalog framework
- [DDF-2411](https://codice.atlassian.net/browse/DDF-2411) Improved presentation of Validate Command
- [DDF-2254](https://codice.atlassian.net/browse/DDF-2254) As a user, I want to be able to switch between registry types when adding a remote registry in the registry-app.
- [DDF-2267](https://codice.atlassian.net/browse/DDF-2267) Updated the local nodes UI for Registry to display the list of all nodes instead of just local
- [DDF-2271](https://codice.atlassian.net/browse/DDF-2271) Update Install Profile to include Registry
- [DDF-2308](https://codice.atlassian.net/browse/DDF-2308) Update the Tika and Pdf `InputTransformers` to inject attributes to `BasicMetacard`

### API CHANGES

- [DDF-2286](https://codice.atlassian.net/browse/DDF-2286) Define the final version of new Reliable Resource API

### BUG FIXES

- [DDF-2464](https://codice.atlassian.net/browse/DDF-2464) Local search results are still returned after local source is unselected from query sources
- [DDF-2342](https://codice.atlassian.net/browse/DDF-2342) Registry updates are lost when multiple ddf instances try to update the same node at the same time
- [DDF-2327](https://codice.atlassian.net/browse/DDF-2327) Content resource URIs should not be overwritable
- [DDF-2416](https://codice.atlassian.net/browse/DDF-2416) Clients using scoped ipv6 addresses are denied guest access
- [DDF-2407](https://codice.atlassian.net/browse/DDF-2407) Application Config Installer lacks permissions to auto-install apps, preventing unattended installs
- [DDF-2392](https://codice.atlassian.net/browse/DDF-2392) OpenDJ Embedded LDAP server feature fails on Windows
- [DDF-2347](https://codice.atlassian.net/browse/DDF-2347) Queries against the cache don't filter out non-resource metacards, causing users to see unintended results
- [DDF-2346](https://codice.atlassian.net/browse/DDF-2346) If the Identity Node contains special characters it cannot be displayed/edited using the Admin UI, causing System Administrators to resort to command line workarounds.
- [DDF-2337](https://codice.atlassian.net/browse/DDF-2337) Ingested Metacards that fail security checks attempt to log the ID, which does not exist.
- [DDF-2326](https://codice.atlassian.net/browse/DDF-2326) Search UI Summary tab does not show Nearby cities
- [DDF-2324](https://codice.atlassian.net/browse/DDF-2324) The DDF Registry App does not stop when deactivated
- [DDF-2311](https://codice.atlassian.net/browse/DDF-2311) Web context policies for paths that extend beyond a servlet's context path are not applied
- [DDF-2307](https://codice.atlassian.net/browse/DDF-2307) Saved map layer preferences in the UI are not updated with the most recent URLs from the configuration causing any users with saved layers to lose all map tiles
- [DDF-2301](https://codice.atlassian.net/browse/DDF-2301) Mapquest no longer provides a public tile service so the maps are no longer rendering correctly
- [DDF-2300](https://codice.atlassian.net/browse/DDF-2300) Range header not supported by CSW endpoint which causes resources to be corrupted
- [DDF-2292](https://codice.atlassian.net/browse/DDF-2292) Deploying sdk-app after compiling from source fails to deploy to maven repository
- [DDF-2290](https://codice.atlassian.net/browse/DDF-2290) Catalog-app features won't start without access to a populated maven repo
- [DDF-2289](https://codice.atlassian.net/browse/DDF-2289) SchematronValidationService should not fail on null/empty metadata, because this erroneously flag metacards as invalid
- [DDF-2288](https://codice.atlassian.net/browse/DDF-2288) XacmlPdp should format IPv6 attributes to conform to RFC-2732
- [DDF-2284](https://codice.atlassian.net/browse/DDF-2284) Platform Command Scheduler fails to run scheduled jobs due to a NullPointerException, forcing administrator to manually run commands in the karaf console
- [DDF-2270](https://codice.atlassian.net/browse/DDF-2270) Various UIs in DDF are fetching fonts from the internet which causes them to load slowly
- [DDF-1804](https://codice.atlassian.net/browse/DDF-1804) Metacards with MultiPolygon geometry cause a cesium rendering error when results are rendered in the standard search-ui

	- [JIRA View of All Issues Resolved](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20resolution%20%3D%20Fixed%20AND%20fixVersion%20%3D%20ddf-2.9.2%20ORDER%20BY%20resolutiondate) in this version.

### KNOWN ISSUES

- [DDF-2513](https://codice.atlassian.net/browse/DDF-2513) Password fields in Admin UI appear to be populated with a password when none has been set
- [DDF-2507](https://codice.atlassian.net/browse/DDF-2507) CSW queries that include numeric criteria always return 0 results
- [DDF-2506](https://codice.atlassian.net/browse/DDF-2506) Sorting by distance can take minutes on large indexes
- [DDF-2504](https://codice.atlassian.net/browse/DDF-2504) Multiple SSO sessions are assigned per browser, which can cause unexpected logout from UI, forcing the user to log in again.
- [DDF-2455](https://codice.atlassian.net/browse/DDF-2455) SchematronValidationService incorrectly appears as a Source in the Sources Admin UI tab
- [DDF-2432](https://codice.atlassian.net/browse/DDF-2432) Metacard validation UI incorrectly reports a Metacard is a duplicate of itself

	- [JIRA View of Open Issues](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20resolution%20%3D%20Unresolved%20AND%20affectedVersion%20%3D%20ddf-2.9.2%20ORDER%20BY%20createdDate) affecting this version.

