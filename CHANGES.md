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

### API CHANGES

- None in this version

### BUG FIXES
(update {VERSION-NUMBER} in JIRA query


- Important issues resolved
	- [Jira view of all issues resolved](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20resolution%20%3D%20Fixed%20AND%20fixVersion%20%3D%20VERSION_NUMBER%20ORDER%20BY%20priority) in this version.

### KNOWN ISSUES
(update {VERSION-NUMBER} in JIRA query


- Open bugs affecting this version
	- [Jira view of open Issues](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20issuetype%20%3D%20bug%20%20AND%20status%20%3D%20Open%20AND%20affectedVersion%20%3D%20VERSION_NUMBER%20AND%20fixVersion%20!%3D%20VERSION_NUMBER%20ORDER%20BY%20priority) affecting this version.
 -->

## 2.10.0
	Release Date: `unreleased`

### NOTES

- This version will require data to be reindexed.

### NEW FEATURES

- [DDF-2413](https://codice.atlassian.net/browse/DDF-2413), [DDF-2388](https://codice.atlassian.net/browse/DDF-2388) Improved support for Expanded metadata taxonomy 
- [DDF-2196](https://codice.atlassian.net/browse/DDF-2196) Separate admin duties via Admin Console
- [DDF-2162](https://codice.atlassian.net/browse/DDF-2162) Add the ability to inject attributes into Metacard Types
- [DDF-2005](https://codice.atlassian.net/browse/DDF-2005) Enabled configuration of the layers utilized by the GeoWebCache app
- [DDF-2260](https://codice.atlassian.net/browse/DDF-2260) Update PDF Input Tranformer to Extract GeoPDF Metadata
- [DDF-2180](https://codice.atlassian.net/browse/DDF-2180) Added support for FTPS on the FTP endpoint
- [DDF-2133](https://codice.atlassian.net/browse/DDF-2133) The FTP endpoint port number is configurable.
- [DDF-2094](https://codice.atlassian.net/browse/DDF-2094) Add support for decrypting encrypted passwords in all sources
- [DDF-2036](https://codice.atlassian.net/browse/DDF-2036) Provide the option to "drape" an overview of a georeferenced image onto the display map.
- [DDF-2508](https://codice.atlassian.net/browse/DDF-2508) Allowed for the ingest of a resource/metadata through the Catalog REST Endpoint.
- [DDF-2188](https://codice.atlassian.net/browse/DDF-2188) Add service for metadata extraction from text documents
- [DDF-2125](https://codice.atlassian.net/browse/DDF-2125) Add the ability to inject default values into metacards during ingest
	- [JIRA View of All New Features](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20issuetype%20in%20%28%22new%20feature%22%2C%20improvement%2C%20story%2C%20task%29%20AND%20fixVersion%20%3D%20ddf-2.10.0%20ORDER%20BY%20priority) 

### API CHANGES

- None in this version

### BUG FIXES

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
	- [Jira view of all issues resolved](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20resolution%20%3D%20Fixed%20AND%20fixVersion%20%3D%20ddf-2.10.0%20ORDER%20BY%20resolutiondate)
	in this version.

### KNOWN ISSUES
- None
	- [Jira view of open Issues](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20issuetype%20%3D%20bug%20%20AND%20status%20%3D%20Open%20AND%20affectedVersion%20%3D%20ddf-2.10.0%20AND%20fixVersion%20!%3D%20ddf-2.10.0%20ORDER%20BY%20priority) affecting this version.

## 2.9.3
	Release Date: `2016-10-27`

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
	- [Jira view of all issues resolved](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20resolution%20%3D%20Fixed%20AND%20fixVersion%20%3D%20ddf-2.9.3%20ORDER%20BY%20resolutiondate) in this version.

### KNOWN ISSUES

- None
	- [Jira view of open Issues](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20issuetype%20%3D%20bug%20%20AND%20status%20%3D%20Open%20AND%20affectedVersion%20%3D%20ddf-2.9.3%20AND%20fixVersion%20!%3D%20ddf-2.9.3%20ORDER%20BY%20priority) affecting this version.

## 2.9.2
	Release Date: `2016-08-26`

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

	- [Jira view of all issues resolved](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20resolution%20%3D%20Fixed%20AND%20fixVersion%20%3D%20ddf-2.9.2%20ORDER%20BY%20resolutiondate) in this version.

### KNOWN ISSUES

- [DDF-2513](https://codice.atlassian.net/browse/DDF-2513) Password fields in Admin UI appear to be populated with a password when none has been set
- [DDF-2507](https://codice.atlassian.net/browse/DDF-2507) CSW queries that include numeric criteria always return 0 results
- [DDF-2506](https://codice.atlassian.net/browse/DDF-2506) Sorting by distance can take minutes on large indexes
- [DDF-2504](https://codice.atlassian.net/browse/DDF-2504) Multiple SSO sessions are assigned per browser, which can cause unexpected logout from UI, forcing the user to log in again.
- [DDF-2455](https://codice.atlassian.net/browse/DDF-2455) SchematronValidationService incorrectly appears as a Source in the Sources Admin UI tab
- [DDF-2432](https://codice.atlassian.net/browse/DDF-2432) Metacard validation UI incorrectly reports a Metacard is a duplicate of itself

	- [Jira view of open Issues](https://codice.atlassian.net/issues/?jql=project%3DDDF%20AND%20type%20%3D%20Bug%20AND%20resolution%20%3D%20Unresolved%20AND%20affectedVersion%20%3D%20ddf-2.9.2%20ORDER%20BY%20createdDate) affecting this version.

