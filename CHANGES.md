# Changes between Versions


## 2.11.0
	Release Date: Pending
_This is a preview of a pending release and is subject to change._    
    
<h3>Bug</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-1802'>DDF-1802</a> - Metacards with GeometryCollection cause a cometd listener exception when rendered on both 2D and 3D maps
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2192'>DDF-2192</a> - Gazetteer service cannot handle large WKT
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2431'>DDF-2431</a> - Update/Delete will not finish if a source does not respond in a timely manner
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2519'>DDF-2519</a> - Fix NumberFormatException when a non-required metatype field is blank
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2563'>DDF-2563</a> - Path lengths in the distribution cause it to unzip improperly using windows&#39; built-in archive tool, forcing administrators to use a third-party tool
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2643'>DDF-2643</a> - TikaInputTransformer ingest fails on large files
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2650'>DDF-2650</a> - An invalid character in the URI could cause a content action to fail
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2778'>DDF-2778</a> - WMTS Imagery Providers do not project EPSG:4326 properly for Cesium
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2786'>DDF-2786</a> - Depending on the zoom level, system header and footer are sometimes cut off in the Catalog UI
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2788'>DDF-2788</a> - An improperly constructed GET request for a WSDL can cause an exception in the PEPAuthorizingInterceptor and keep it from logging the attempt
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2789'>DDF-2789</a> - The source poller can stop working when a source is removed because of a race condition with blueprint which causes sources to stop updating with new status
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2790'>DDF-2790</a> - Large Usernames in the Data Usage Admin App push table columns off the screen, causing erroneous behavior
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2797'>DDF-2797</a> - HTTP Proxy destroy method causes an exception when stopping endpoints which causes the Camel context to not shut down correctly
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2809'>DDF-2809</a> - URLs with extremely long paths can cause an exception in the web context policy manager
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2832'>DDF-2832</a> - Catalog UI should display metacard validation issues in the upload view
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2835'>DDF-2835</a> - CSW queries using extended attributes return 0 results, forcing integrators to use a different endpoint for advanced searches
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2864'>DDF-2864</a> - Security headers prevent login through grunt proxy for Intrigue
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2872'>DDF-2872</a> - Requesting a response size for a Query to MAX_INT causes Solr Cloud to throw an ArrayIndexOutOfBounds exception
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2893'>DDF-2893</a> - Missing headers prevent some resources from caching in browsers
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2895'>DDF-2895</a> - IdP and SP HTTP metadata requests only perform exponential backoff for unsuccessful attempts and not for IOExceptions which can leave the IdP and SP unusable until a restart
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2901'>DDF-2901</a> - Viewing the source configuration for a source created from a registry results in unsupported sources appearing as selectable for that node
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2903'>DDF-2903</a> - CSW source only works against endpoints that are secured with a GUEST, BASIC, or PKI authentication policy
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2922'>DDF-2922</a> - Outgoing requests fail when using PKI authentication to any CXF REST source
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2923'>DDF-2923</a> - When restoring a metacard without content the metacard shows as being deleted and restored
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2928'>DDF-2928</a> - SolrCatalogProvider uses default row value of 10, which causes only the first 10 metacards to be updated from an UpdateRequest
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2928'>DDF-3098</a> - Fix XstreamTreeWriter to correctly handle the case when a parent and child element contain the same attribute.
    </li>
</ul>

<h3>Story</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-1765'>DDF-1765</a> - As a user, I want to add a layer to the Search UI map from a KML file so that I can visualize my KML data
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2039'>DDF-2039</a> - As an integrator, I want to make a single request to the catalog framework to get back both the metacard and the resource.
    </li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2820'>DDF-2820</a> - As a user, I would like the ability to provide feedback on DDF.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2961'>DDF-2961</a> - As an administrator I would like to set the transformer backup plugin to have an option to skip any metacards that has warnings or errors on it so that a non-conformant metadata card is not backed up
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2973'>DDF-2973</a> - As an integrator, I want required attributes on Metacards to be enforced so that invalid Metacards don't get ingested
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2999'>DDF-2999</a> - As an integrator, I would like to support S3 as a MetacardBackupStorageProvider
    </li>
</ul>
    
<h3>New Feature</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-2866'>DDF-2866</a> - As a user I would like to be able to specify a Geospatial query by Universal Transverse Mercator so that I can express Geospatial queries via Universal Transverse Locator in the catalog UI
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2916'>DDF-2916</a> - Add cache busting to the Catalog (Intrigue) UI
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2936'>DDF-2936</a> - As an integrator, I would like to have configurable writers for the MetacardBackupPlugin
    </li>
</ul>
    
<h3>Task</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-2698'>DDF-2698</a> - Create a catalog:export command
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2822'>DDF-2822</a> - Update name for Catalog UI
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2843'>DDF-2843</a> - Add incremental builder extension to speed up PR builds
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2867'>DDF-2867</a> - Move documentation out of profile into main build process
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2878'>DDF-2878</a> - Implement System Administrator Alerts in 2.11.0
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2896'>DDF-2896</a> - Update admin-ui to apply iframeresizer to AdminModule plugins.
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2911'>DDF-2911</a> - The directory monitor creates an extra dataDurableFileConsumer directory
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2935'>DDF-2935</a> - Update DDF Support version to 2.3.7
    </li>
</ul>
    
<h3>Improvement</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-1882'>DDF-1882</a> - Support XPath pre-filtering with Solr 5
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2241'>DDF-2241</a> - Create a Historian user for all history actions
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2572'>DDF-2572</a> - Create ProcessingFramework Default (In Memory) Implementation
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2573'>DDF-2573</a> - Create ProcessingPostIngestPlugin
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2695'>DDF-2695</a> - Add taxonomy attributes for better normalization
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2715'>DDF-2715</a> - Make Point of Contact Read-only
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2783'>DDF-2783</a> - Update thumbnails to allow animation in the summary view
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2791'>DDF-2791</a> - Update the SolrFilterDelegate to support empty isEqualTo and isLike queries
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2793'>DDF-2793</a> - Make Catalog UI page size configurable
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2799'>DDF-2799</a> - Update the Catalog UI to allow querying for empty String fields
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2811'>DDF-2811</a> - Invalid data in workspace metacards causes workspaces page to fail
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2812'>DDF-2812</a> - Bad workspace data can be manually inserted causing unreliable workspace view
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2815'>DDF-2815</a> - As an integrator I would like to be able to specify a Geospatial query by Universal Transverse Mercator so that I can express Geospatial queries via Universal Transverse Locator through web services
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2817'>DDF-2817</a> - HTTP responses from DDF should include a CSP header
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2827'>DDF-2827</a> - Change the default authentication mechanism in DDF to the IdP
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2848'>DDF-2848</a> - As a user I would like to specify my preferred result count in my preferences so that I can control the bandwidth requirements to deliver me a result list
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2849'>DDF-2849</a> - CQL endpoint does not respect Accept-Encoding header
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2851'>DDF-2851</a> - As an administrator, I want to be able to configure the Content Directory Monitor to ignore certain file patterns
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2855'>DDF-2855</a> - Reduce amount of data returned in Node Information tab to improve performance
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2860'>DDF-2860</a> - Create an LDAP server and unit tests for LDAP login module
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2870'>DDF-2870</a> - Cache Node Information tabs table information on the backend for quick retrieval
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2874'>DDF-2874</a> - Allow viewing and editing of geometry attributes
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2894'>DDF-2894</a> - Enhance query results by allowing sorting by any attribute
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2899'>DDF-2899</a> - Update setAttribute method comments in the metacard interface to reflect the expected contract.
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2910'>DDF-2910</a> - Add an exception to the IdP handler to log in legacy systems using user agent to determine whether or not the client is a browser
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2919'>DDF-2919</a> - Create generic WebClients with SecureCxfClientFactory
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-3030'>DDF-3030</a> - Added support for match any validator
    </li>    	
</ul>

<h3>Technical Debt</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-2451'>DDF-2451</a> - Deprecate all the legacy attributes and add a compatibility plugin
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2731'>DDF-2731</a> - Move PropertiesFileReader where it can be re-used
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2798'>DDF-2798</a> - There are several possible null pointers within the ECP handlers
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2821'>DDF-2821</a> - Check audit messages for special characters and encode them before writing the audit
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2913'>DDF-2913</a> - OpenSearch block in DDF TestSecurity is disabled
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2931'>DDF-2931</a> - Resolve issue with an unclosed InputStream
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2933'>DDF-2933</a> - Resolve issue with dereferenced null value and modifications to unmod list
    </li>
</ul>
    
<h3>Dependency Upgrade</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-2132'>DDF-2132</a> - Upgraded Karaf to 4.1.1
    </li>
</ul>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-2818'>DDF-2818</a> - Upgrade to Solr version 6.4.1
    </li>
</ul>


## 2.10.2
	Release Date: 2017-04-12

<h3>Bug</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-2845'>DDF-2845</a> - Catalog UI result list help-text doesn&#39;t reflect the attribute name alias
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2859'>DDF-2859</a> - Dependency order incorrect causing missing dependency after release
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2862'>DDF-2862</a> - Concurrent updates can cause a resource shortage making the system less responsive
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2865'>DDF-2865</a> - The Content Directory Monitor fails to ingest files during slower copies to the monitored directory
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2869'>DDF-2869</a> - The ConfigurationFilePoller may fail to detect configurations in certain storage configurations
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2893'>DDF-2893</a> - Missing headers prevent some resources from caching in browsers
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2895'>DDF-2895</a> - IdP and SP HTTP metadata requests only perform exponential backoff for unsuccessful attempts and not for IOExceptions which can leave the IdP and SP unusable until a restart
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2901'>DDF-2901</a> - Viewing the source configuration for a source created from a registry results in unsupported sources appearing as selectable for that node
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2902'>DDF-2902</a> - Users may get 401/403 authentication errors when redirected to login page due to use of the wrong realm
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2903'>DDF-2903</a> - CSW source only works against endpoints that are secured with a GUEST, BASIC, or PKI authentication policy
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2922'>DDF-2922</a> - Outgoing requests fail when using PKI authentication to any CXF REST source
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2923'>DDF-2923</a> - When restoring a metacard without content the metacard shows as being deleted and restored
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2934'>DDF-2934</a> - The handlebars helper &#39;bind&#39; allows html injection
    </li>
</ul>

<h3>Story</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-2854'>DDF-2854</a> - As an administrator, I would like to be able to view the security attributes of any user in the Admin UI
    </li>
</ul>
    
<h3>New Feature</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-2494'>DDF-2494</a> - Create email session configuration and API
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2921'>DDF-2921</a> - VideoThumbnail plugin does not recognize unqualified content
    </li>
</ul>
    
<h3>Task</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-2649'>DDF-2649</a> - Add a Secure Development section to the docs
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2912'>DDF-2912</a> - Implement System Administrator Alerts in 2.10.2
    </li>
</ul>
    
<h3>Improvement</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-2847'>DDF-2847</a> - Add indication of unsaved changes to workspaces
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2852'>DDF-2852</a> - Add functionality for retrieving a logged-in user&#39;s security claims
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2853'>DDF-2853</a> - Add endpoint to retrieve subject attributes for a user
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2855'>DDF-2855</a> - Reduce amount of data returned in Node Information tab to improve performance
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2858'>DDF-2858</a> - Remove bouncy castle packages from platform-util
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2870'>DDF-2870</a> - Cache Node Information tabs table information on the backend for quick retrieval
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2905'>DDF-2905</a> - When using default layers with the 2D Map, geometries appear incorrectly positioned
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2910'>DDF-2910</a> - Add an exception to the IdP handler to log in legacy systems using user agent to determine whether or not the client is a browser
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2915'>DDF-2915</a> - Update the Admin UI loading scheme to reduce the initial network cost
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2917'>DDF-2917</a> - Update caching to exclude base app pages and html
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2919'>DDF-2919</a> - Create generic WebClients with SecureCxfClientFactory
    </li>
</ul>
    
<h3>Documentation</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-2856'>DDF-2856</a> - Update Installation Prerequisites to include Hardware Guidelines. 
    </li>
    <li><a href='https://codice.atlassian.net/browse/DDF-2652'>DDF-2652</a> - document OSGi Basics for contributing developers
    </li>
</ul>
    
<h3>Technical Debt</h3>
<ul>
    <li><a href='https://codice.atlassian.net/browse/DDF-2873'>DDF-2873</a> - Address bad feature dependencies between Search and Catalog UIs and rename UI app
    </li>
</ul>
    

## 2.10.1
	Release Date: 2017-03-08    
<h3>Bug</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2775'>DDF-2775</a> - In the Catalog UI and the Standard UI, actions that are not supported for a given metacard’s tag type are displayed.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2790'>DDF-2790</a> - Large Usernames in the Data Usage Admin App push table columns off the screen, causing erroneous behavior
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2792'>DDF-2792</a> - Update client side sorting to handle null / blank values more consistently
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2795'>DDF-2795</a> - The Content Directory Monitor &quot;Attribute Overrides&quot; fails when there is a comma in the attribute
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2797'>DDF-2797</a> - HTTP Proxy destroy method causes an exception when stopping endpoints which causes the Camel context to not shut down correctly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2806'>DDF-2806</a> - Add Jetty dependency to Catalog UI Search pom.xml
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2809'>DDF-2809</a> - URLs with extremely long paths can cause an exception in the web context policy manager
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2814'>DDF-2814</a> - Catalog UI area searches fail when the area includes self-intersecting polygons
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2830'>DDF-2830</a> - Hot deploying KAR files does not work on a production system
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2835'>DDF-2835</a> - CSW queries using extended attributes return 0 results, forcing integrators to use a different endpoint for advanced searches
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2845'>DDF-2845</a> - Catalog UI result list help-text doesn&#39;t reflect the attribute name alias
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2850'>DDF-2850</a> - Catalog UI gazetteer searches fail in 3d view
	</li>
</ul>

<h3>Task</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2822'>DDF-2822</a> - Update name for Catalog UI
	</li>
</ul>
    
<h3>Improvement</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2208'>DDF-2208</a> - Remove DDF from app names
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2572'>DDF-2572</a> - Create ProcessingFramework Default (In Memory) Implementation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2573'>DDF-2573</a> - Create ProcessingPostIngestPlugin
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2715'>DDF-2715</a> - Make Point of Contact Read-only
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2791'>DDF-2791</a> - Update the SolrFilterDelegate to support empty isEqualTo and isLike queries
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2794'>DDF-2794</a> - Set registry metacard security markings based on system high/low and user input
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2807'>DDF-2807</a> - Clean up maven profiles and add quickbuild profile
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2816'>DDF-2816</a> - Update details view to allow links to be clickable
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2817'>DDF-2817</a> - HTTP responses from DDF should include a CSP header
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2837'>DDF-2837</a> - Metacards with attributes that have a null value are not properly marshaled which can result in failed actions and NPEs. 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2849'>DDF-2849</a> - CQL endpoint does not respect Accept-Encoding header
	</li>
</ul>
    
<h3>Documentation</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2653'>DDF-2653</a> - document OSGi Basics for contributing developers
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2804'>DDF-2804</a> - Update hardening guide to recommend 10 minute timeout
	</li>
</ul>
    
<h3>Technical Debt</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2785'>DDF-2785</a> - Saving polygons in catalog UI creates duplicate point
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2798'>DDF-2798</a> - There are several possible null pointers within the ECP handlers
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2810'>DDF-2810</a> - Upgrade GeoWebCache from 1.5.0 to 1.9.1
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2821'>DDF-2821</a> - Check audit messages for special characters and encode them before writing the audit
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2826'>DDF-2826</a> - Increase default JVM memory to 4096MB
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2836'>DDF-2836</a> - Upgrade Spark from 2.5 to 2.5.5
	</li>
</ul>
    
<h3>Dependency Upgrade</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2818'>DDF-2818</a> - Upgrade to Solr version 6.4.1
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2833'>DDF-2833</a> - Upgrade to latest version of Yarn
	</li>
</ul>


## 2.10.0
	Release Date: 2017-02-08    
<h3>Bug</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-839'>DDF-839</a> - catalog:removeall command gets OutOfMemoryError with large datasets
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1460'>DDF-1460</a> - GeoJsonInputTransformer does not correctly support a geometry type of GeometryCollection
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1789'>DDF-1789</a> - Empty query is created in Workspace if &#39;&lt; Workspace&#39; is clicked instead of cancel button
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1804'>DDF-1804</a> - Metacards with MultiPolygon geometry cause a cesium rendering error when results are rendered in the standard search-ui
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1927'>DDF-1927</a> - CSW Serialization does not retain sort order of a query response
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2024'>DDF-2024</a> - Errors encountered while running &#39;dump&#39; command are not reflected in the log
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2035'>DDF-2035</a> - Data ingested via CSW is modified, but should maintain transaction integrity.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2043'>DDF-2043</a> - Null Principals provided to ClaimsHandlers can cause claims processing to fail
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2083'>DDF-2083</a> - Password values can be seen as clear text when inspecting the password’s input element in the Admin UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2086'>DDF-2086</a> - Remove upstream dependency from platform-configuration-impl
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2087'>DDF-2087</a> - Fix CswFilterDelegate to set the propertyIsLike filter matchCase flag.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2089'>DDF-2089</a> - Streaming input transformers don&#39;t declare a mimetype
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2090'>DDF-2090</a> - GeoJSON input transformer doesn&#39;t handle multiple values correctly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2093'>DDF-2093</a> - DeleteRequestImpl is not correctly using generics
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2097'>DDF-2097</a> - DDF federate source service fails cache and to return the federated source list in a timely fashion 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2098'>DDF-2098</a> - PDFs missing a modification date can fail to ingest
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2115'>DDF-2115</a> - setenv modifications do not work in non-bash shells - update to be posix compatible
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2116'>DDF-2116</a> - NPM related build errors in Bamboo
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2122'>DDF-2122</a> - CSW Endpoint logs erroneous warnings that 2 operations are equal when they are not
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2123'>DDF-2123</a> - ffmpeg is logging to System.out when creating new video thumbnails
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2131'>DDF-2131</a> - Previous versions of metacards can be returned along with ingested metacards
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2138'>DDF-2138</a> - File Install config creates directory with comment in name
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2148'>DDF-2148</a> - CSW federated source created with bad credentials will not immediately reconnect after they are fixed
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2150'>DDF-2150</a> - Windows attribute .cfg files do not get parsed properly in AbstractExpansion
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2166'>DDF-2166</a> - Log viewer UI should specify only exact versions of NPM dependencies
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2169'>DDF-2169</a> - Fix issues in hubotinit.sh that cause bamboo bot to startup incorrectly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2171'>DDF-2171</a> - In the Standard Search UI faceted search view, submitting a textual search with an empty value causes an exception in the search endpoint
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2178'>DDF-2178</a> - FederationAdminService doesn&#39;t initialize because init doesn&#39;t have a subject to run its commands.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2183'>DDF-2183</a> - STS Ldap Login bundle ignores configured usernameAttribute
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2189'>DDF-2189</a> - Cached resource is deleted when a new search is done
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2198'>DDF-2198</a> - Advanced search not correctly filtering on metadata content type
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2205'>DDF-2205</a> - ContentDirectoryMonitor stopped ingesting files on directories with a large amount of content
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2209'>DDF-2209</a> - The LDAP claims handler should only be decrypting passwords for keystores if it is going to use them
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2216'>DDF-2216</a> - DDF does not update its federated source list when a federated source changes its name
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2219'>DDF-2219</a> - The registry source and publication handlers don&#39;t have the needed permission to execute some of their methods
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2220'>DDF-2220</a> - Additional query parameters are not preserved after a search is performed
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2222'>DDF-2222</a> - PDP does not have the correct mappings set up for expansion services
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2224'>DDF-2224</a> - DDF does not work in offline mode
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2232'>DDF-2232</a> - Update pom-fix to run correctly on a maven build.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2238'>DDF-2238</a> - Can&#39;t compile from source due to branding-api bundle being located in distribution
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2250'>DDF-2250</a> - SaxEventToXmlConverter does not handle nested namespace redeclarations
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2252'>DDF-2252</a> - Cannot uninstall specific sdk-app features
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2259'>DDF-2259</a> - Registry IdentificationPlugin is not handling updates updates correctly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2270'>DDF-2270</a> - Various UIs in DDF are fetching fonts from the internet which causes them to load slowly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2284'>DDF-2284</a> - Platform Command Scheduler fails to run scheduled jobs due to a NullPointerException, forcing administrator to manually run commands in the karaf console
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2288'>DDF-2288</a> - XacmlPdp should format IPv6 attributes to conform to RFC-2732
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2289'>DDF-2289</a> - SchematronValidationService should not fail on null/empty metadata, because this erroneously flag metacards as invalid
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2290'>DDF-2290</a> - Catalog-app features won&#39;t start without access to a populated maven repo
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2292'>DDF-2292</a> - Deploying sdk-app after compiling from source fails to deploy to maven repository
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2293'>DDF-2293</a> - Invalid caching status reported to user due to resource caching flag being set as a basic type metacard attribute
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2297'>DDF-2297</a> - Federated queries never timeout or return results in the standard search ui
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2300'>DDF-2300</a> - Range header not supported by CSW endpoint which causes resources to be corrupted
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2301'>DDF-2301</a> - Mapquest no longer provides a public tile service so the maps are no longer rendering correctly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2307'>DDF-2307</a> - Saved map layer preferences in the UI are not updated with the most recent URLs from the configuration causing any users with saved layers to lose all map tiles
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2311'>DDF-2311</a> - Web context policies for paths that extend beyond a servlet&#39;s context path are not applied
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2316'>DDF-2316</a> - Tests and OWASP fail on Windows build
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2320'>DDF-2320</a> - Only one InputTransformer per mime-type is called, causing files that would be supported with subsequent InputTransformers to be given only basic metadata
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2324'>DDF-2324</a> - The DDF Registry App does not stop when deactivated
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2326'>DDF-2326</a> - Search UI Summary tab does not show Nearby cities
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2329'>DDF-2329</a> - NCSA logs don&#39;t show content-length
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2337'>DDF-2337</a> - Ingested Metacards that fail security checks attempt to log the ID, which does not exist.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2346'>DDF-2346</a> - If the Identity Node contains special characters it cannot be displayed/edited using the Admin UI, causing System Administrators to resort to command line workarounds.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2347'>DDF-2347</a> - Queries against the cache don&#39;t filter out non-resource metacards, causing users to see unintended results
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2349'>DDF-2349</a> - Percentages in the Ingest Modal on the Standard Search UI show up as NaN when they should be 0%
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2350'>DDF-2350</a> - MetacardType implementations should not return static mutables
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2368'>DDF-2368</a> - ResourceUsagePlugin does not count first download against user&#39;s limit
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2385'>DDF-2385</a> - Admin Console does not provide any feedback that you might be logged out which can confuse system admins
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2392'>DDF-2392</a> - OpenDJ Embedded LDAP server feature fails on Windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2407'>DDF-2407</a> - Application Config Installer lacks permissions to auto-install apps, preventing unattended installs
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2416'>DDF-2416</a> - Clients using scoped ipv6 addresses are denied guest access
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2424'>DDF-2424</a> - Updating registry entries fails when versioning is enabled
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2432'>DDF-2432</a> - Metacard validation UI incorrectly reports a Metacard is a duplicate of itself
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2440'>DDF-2440</a> - SourceOperationsTest.&#39;test source is available&#39; is unreliable
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2441'>DDF-2441</a> - UI Scroll Bar does not work in Workspace on Details View
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2455'>DDF-2455</a> - SchematronValidationService incorrectly appears as a Source in the Sources Admin UI tab
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2457'>DDF-2457</a> - Improve the Sources Tab with appropriate and current status of the Sources
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2459'>DDF-2459</a> - Content directory monitor stops finding new files, requiring administrators to refresh configuration settings to trigger updates
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2465'>DDF-2465</a> - If a source or type gets added, removed, or changed, the Search UI will throw an error
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2466'>DDF-2466</a> - CSW queries that encounter errors during transformation respond with 0 results rather than an error
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2474'>DDF-2474</a> - Ingesting a large zip file containing metacards/content ingests only some of the data
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2497'>DDF-2497</a> - Tab characters are prepended to WMS Layer configuration, causing layers from ArcGIS to not display in the UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2504'>DDF-2504</a> - Multiple SSO sessions are assigned per browser, which can cause unexpected logout from UI, forcing the user to log in again.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2506'>DDF-2506</a> - Sorting by distance can take minutes on large indexes
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2526'>DDF-2526</a> - WFS 1.0 Source fails to accept its name change
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2540'>DDF-2540</a> - Unable to ingest mpegts clips that have incomplete TS packets
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2543'>DDF-2543</a> - CSW product retrieval may fail due to a missing security subject
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2547'>DDF-2547</a> - Can&#39;t download products when using basic auth on a source connected to a non DDF endpoint
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2548'>DDF-2548</a> - Queries to SDK Twitter source fail with nullpointer exception
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2555'>DDF-2555</a> - CSW Federation Profile Source configuration does not * out the password, exposing the password to the screen
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2584'>DDF-2584</a> - catalog:removeall fails on metacards that have resource-uris other than file: or content:
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2588'>DDF-2588</a> - Add retry logic to TemporaryFileBackedOutputStream file deletion
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2594'>DDF-2594</a> - Content Directory Monitor fails to ingest large files
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2600'>DDF-2600</a> - CSW and GMD exports do not have a mime type set resulting in no extension on export
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2601'>DDF-2601</a> - Some completed searches are not removed from the query monitor
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2604'>DDF-2604</a> - The Latitude and Longitude attributes are editable when they should be read only for a remote node on the Node Information tab of the DDF Registry.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2612'>DDF-2612</a> - Resource Management Active Queries page displays queries that have completed
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2617'>DDF-2617</a> - Solr logs an error when it fails to create a core (even when the core is already created), which makes it look like DDF is malfunctioning
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2620'>DDF-2620</a> - Unable to update metacard immediately after creating metacard with Solr Provider
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2625'>DDF-2625</a> - Enumeration Validator was not returning correct values
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2630'>DDF-2630</a> - Point of contact attribute is not set on metacards for ingested products
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2632'>DDF-2632</a> - Video should have a datatype attribute of &#39;Video&#39;.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2633'>DDF-2633</a> - Configuration file users.attributes not being migrated which may cause cloned systems to be misconfigured
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2636'>DDF-2636</a> - Keyword queries containing a hyphen do not match expected metacards when not using ANY_TEXT in solr catalog/cache
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2637'>DDF-2637</a> - Installations fail on very slow systems
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2640'>DDF-2640</a> - System high is not set during installation, requiring additional admin configuration
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2654'>DDF-2654</a> - Results are returned from all federated sources when querying a specific source from the Standard Search UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2657'>DDF-2657</a> - Dumping catalog data to a zip file can leave the file open 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2663'>DDF-2663</a> - CatalogFramework does not correctly add local catalog source descriptor to request for sources which can cause the catalog framework to fail a request
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2665'>DDF-2665</a> - Ingesting PowerPoint files  would sometimes fail because of missing dependency
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2667'>DDF-2667</a> - Queries to unresponsive sources can return successfully with no results instead of failing with exception(s)
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2687'>DDF-2687</a> - At higher zoom levels in Cesium, polygon and line drawings come out incorrect
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2688'>DDF-2688</a> - Temporary files for qualified content being left behind after UpdateStorageRequest
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2690'>DDF-2690</a> - Possible race condition during workspace fetch
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2692'>DDF-2692</a> - Reverting an item with a thumbnail causes the result inspector pane to crash
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2693'>DDF-2693</a> - Race condition on first page load when routing directly to a cloud workspace
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2694'>DDF-2694</a> - Deleted Metacard Types should be excluded from versioning
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2696'>DDF-2696</a> - change default return address for email notifications to donotreply@example.com
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2697'>DDF-2697</a> - Determining the mime type of a file passed into the catalog will fail if the first detector can&#39;t determine the type which then sets it to the default
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2699'>DDF-2699</a> - Add a filter to the TikaMimeTypeResolver in the REST endpoint blueprint
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2700'>DDF-2700</a> - CswRegistryStore tests in TestRegistry do not pass when run alone
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2701'>DDF-2701</a> - Catalog UI help description for &quot;Hide from Future Searches&quot; does not accurately reflect the steps to undo this action
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2711'>DDF-2711</a> - Thumbnails are not displayed in the &quot;table&quot; visualization
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2718'>DDF-2718</a> - Documentation references fonts only available over the internet
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2722'>DDF-2722</a> - Multi-select Editing in CatalogUI can sometimes return a 404
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2723'>DDF-2723</a> - Metacard cache can improperly cache modified metacards, thus returning incorrect information on subsequent requests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2726'>DDF-2726</a> - Solr textual sorting appears to be out of order for multi-worded fields
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2727'>DDF-2727</a> - DirectoryStream is not closed in the FileSystemStorageProvider, causing potential lock issues on Windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2733'>DDF-2733</a> - Video thumbnail generation can fail on Windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2734'>DDF-2734</a> - Unable to enter 0 values in geolocation search
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2739'>DDF-2739</a> - Default bluemarble WMS tile server from the Standard and Catalog UI is frequently unavailable
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2741'>DDF-2741</a> - Fix issues raised by Coverity
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2744'>DDF-2744</a> - CatalogFramework UpdateStorageRequest loses resource-uri of original metacard
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2745'>DDF-2745</a> - ContentUriAccessPlugin fails when incoming URI is null, but existing Metacard URI is not null
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2747'>DDF-2747</a> - LDAP login module should check for bad characters in the user&#39;s name
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2750'>DDF-2750</a> - Regenerate Sources operation does not re-enable the re-created sources
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2753'>DDF-2753</a> - Lat &amp; Lon are not verified on the Local Node UI modal
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2755'>DDF-2755</a> - Duplicate attribute descriptors cause workspace creation to fail
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2759'>DDF-2759</a> - Feature configuration files are not always being processed which causes some managed services to not come up
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2762'>DDF-2762</a> - While editing workspace e-mail addresses, the cursor jumps unexpectedly to the end of the line
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2772'>DDF-2772</a> - Landing Page configuration shows the &quot;Description&quot; field as grayed out although it can be edited
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2774'>DDF-2774</a> - The client.bat script does not give the user access to the DDF console when running DDF as a service on Windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2779'>DDF-2779</a> - Extra points are added to line / polygon drawings in Cesium
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2781'>DDF-2781</a> - Catalog UI User Name may display as `guest` in some cases
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2782'>DDF-2782</a> - Move the location of Karaf&#39;s lock file so that a non-admin user can modify it on a hardened system
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2788'>DDF-2788</a> - An improperly constructed GET request for a WSDL can cause an exception in the PEPAuthorizingInterceptor and keep it from logging the attempt
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2789'>DDF-2789</a> - The source poller can stop working when a source is removed because of a race condition with blueprint which causes sources to stop updating with new status
	</li>
</ul>

<h3>Story</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1376'>DDF-1376</a> - As a user, I want to clearly differentiate features that are closely clustered on the map so that I can find the feature I want
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1631'>DDF-1631</a> - Display the cities closest to a metacard in the Search UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1750'>DDF-1750</a> - As an Administrator I want to be warned if a problem occurs during a configuration export
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1824'>DDF-1824</a> - As an Administrator, I want to be able to export my catalog as part of the migration process
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2051'>DDF-2051</a> - As an administrator, I want to be able to harden my system to operate with no users.properties file
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2075'>DDF-2075</a> - As an administrator, I want ingest/dump/remove commands to perform their respective actions on history metacards.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2214'>DDF-2214</a> - As a user, I want to clearly differentiate features that are closely clustered on the Cesium map so that I can find the feature I want
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2225'>DDF-2225</a> - As a user, I want to clearly differentiate features that are closely clustered on the OpenLayers map so that I can find the feature I want
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2236'>DDF-2236</a> - Verify New Features in Release Notes pages are appropriately documented.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2239'>DDF-2239</a> - As an Integrator, I need the ability to get the list of all current cache downloads for all users
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2274'>DDF-2274</a> - As an administrator, I want data purged based on its age so that I can control the size of the catalog
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2285'>DDF-2285</a> - As an administrator, I would like to add certificates to the trust store by specifying a URL that I want DDF to interoperate with
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2331'>DDF-2331</a> - As an administrator, I want an option to configure the thread pool for the ContentDirectoryMonitor so that I can better control file ingest
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2333'>DDF-2333</a> - As an administrator, I want to seed the metadata and resource cache from the enterprise so that my local node is updated with the most relevant information
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2502'>DDF-2502</a> - As an administrator, I need to ability to add attributes to ingested metacards coming from a specific source before they get processed
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2590'>DDF-2590</a> - Resolve Apache Commons FileUpload Dependency Check Failure
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2627'>DDF-2627</a> - As an integrator, I want a highly available Solr Cache so that I can lose a machine and maintain operations
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2638'>DDF-2638</a> - As an integrator, I want a highly available persistence store so that I can lose a machine and maintain operations.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2644'>DDF-2644</a> - Create a Catalog Search UI to enable viewing, editing, and exploitation of enterprise data
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2671'>DDF-2671</a> - As an Administrator, I want to be able to see the currently active url for each source and be warned when a source is configured for loopback federation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2710'>DDF-2710</a> - As an administrator, I want the content directory monitor to be able to monitor files in place, so that I can expose files without removing them from their current location
	</li>
</ul>
    
<h3>New Feature</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2005'>DDF-2005</a> - As an administrator, I want to configure the layers utilized by the GeoWebCache app so that I can easily control what tile services and layers are used
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2056'>DDF-2056</a> - Audit whenever a new session is created.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2060'>DDF-2060</a> - Clean up exceptions passed back from catalog endpoints
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2111'>DDF-2111</a> - Create Score Command
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2119'>DDF-2119</a> - As an Integrator, I want to be able to trigger the caching of selected products in order to allow users to download them from the cache at a later time
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2120'>DDF-2120</a> - As an Integrator, I need the DDF API to provide a status of the number of results returned/success/failed/partial of the query that is in progress
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2162'>DDF-2162</a> - Add the ability to inject attributes into Metacard Types
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2170'>DDF-2170</a> - Add srcclr profile to root pom
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2196'>DDF-2196</a> - Separate admin duties via Admin Console
	</li>
</ul>
    
<h3>Task</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1829'>DDF-1829</a> - Create Default expansion configuration files 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1886'>DDF-1886</a> - Create the UI for editing local node information
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1964'>DDF-1964</a> - Update Sources Tab to show additional information for registry entries
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2008'>DDF-2008</a> - Create ActionProvider for registry metacards to provide an action (url)
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2044'>DDF-2044</a> - Create registry event manager to handle publication updates
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2057'>DDF-2057</a> - Update the remote registry UI so that it can set a remote registry name in addition to the local definition
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2062'>DDF-2062</a> - Create unit test(s) for documentation module
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2108'>DDF-2108</a> - Remove dependencies that some bundles have on the Felix web console and disable the Felix console
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2125'>DDF-2125</a> - Add the ability to inject default values into metacards during ingest
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2145'>DDF-2145</a> - Fix MetacardValidityMarkerPlugin to run on update requests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2164'>DDF-2164</a> - Move /jolokia context under /admin
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2165'>DDF-2165</a> - Create registry action provider for publishing/unpublishing
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2181'>DDF-2181</a> - Create registry publication service
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2188'>DDF-2188</a> - Add service for metadata extraction from text documents
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2191'>DDF-2191</a> - Create Integration Tests covering Ingest through the FTP Endpoint
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2194'>DDF-2194</a> - Move the &quot;Download Started&quot; publish event to the ReliableResourceDownloader
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2195'>DDF-2195</a> - Create registry rest endpoint
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2217'>DDF-2217</a> - Clean up bower dependencies
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2249'>DDF-2249</a> - Refactor FTP default port to be configurable in system properties 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2263'>DDF-2263</a> - Update Intellij code formatter for javax.swing and java.awt imports
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2278'>DDF-2278</a> - Turn on catalog-core-validationparser by default
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2296'>DDF-2296</a> - History of non-standard attributes are not retained when versioned
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2317'>DDF-2317</a> - Metacard created and modified dates should be set during ingest to reflect the new taxonomy mappings
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2335'>DDF-2335</a> - Disable Hazelcast version check on startup
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2344'>DDF-2344</a> - Redesign CatalogFrameworkImpl to delegate work to logical subcomponents
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2373'>DDF-2373</a> - Create Integration Tests covering Ingest through the FTPS Endpoint
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2500'>DDF-2500</a> - Create internal Pre-Policy plug-in interface and add servlet filter to extract and pass client information to plug-ins
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2539'>DDF-2539</a> - Cannot log into the Standard Search UI with IE 11 or Firefox on Windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2577'>DDF-2577</a> - Uploaded products without security markings should be rejected and should update the ingest log
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2642'>DDF-2642</a> - Remove accordion view from sources tab.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2651'>DDF-2651</a> - Document Using Landing Page, Simple Search UI, and Catalog UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2704'>DDF-2704</a> - Switch default UI from standard to catalog
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2729'>DDF-2729</a> - Allow administrators the ability to disable the use of the cache with Catalog UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2730'>DDF-2730</a> - Address Node Security scan findings for Catalog UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2780'>DDF-2780</a> - Update Cesium to latest version
	</li>
</ul>
    
<h3>Improvement</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-519'>DDF-519</a> - GeoJSON transformer needs to be decoupled from the MetacardType Registry
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1560'>DDF-1560</a> - ForkJoinPool.commonPool should be used instead of new pools
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1723'>DDF-1723</a> - Update platform-parser-api to support all marshal/unmarshal paths
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1869'>DDF-1869</a> - Refactor the FilterDelegate and all classes extending
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2017'>DDF-2017</a> - Move registry-app under spatial
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2036'>DDF-2036</a> - Provide the option to &quot;drape&quot; an overview of a georeferenced image onto the display map.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2050'>DDF-2050</a> - As an administrator, I would like to import/export catalog data with linked product data so that I can easily replicate catalog/content store contents between multiple DDF instances
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2076'>DDF-2076</a> - Use a RNG for the generation of SessionIndex in the SAML element AuthnStatement
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2078'>DDF-2078</a> - Audit confguration changes to the PDP
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2080'>DDF-2080</a> - Improve TestSpatial to include Csw queries with XPath expressions
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2081'>DDF-2081</a> - Record configuration changes to the system to improve security auditability
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2082'>DDF-2082</a> - MetacardValidityMarkerPlugin should run on update requests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2084'>DDF-2084</a> - Update the model CertificateSigningRequest to allow setting a full DN
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2094'>DDF-2094</a> - Add support for decrypting encrypted passwords in all sources
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2095'>DDF-2095</a> - setenv should detect if &#39;/dev/urandom&#39; is available for Linux environments
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2096'>DDF-2096</a> - showInvalidMetacards flag should be able to distinguish between &quot;validation-errors&quot; &amp; &quot;validation-warnings&quot;
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2109'>DDF-2109</a> - Update logic in IdentityPlugin and RegistryStoreImpl to use common code to do ebrim metacard id updates
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2110'>DDF-2110</a> - The XACML PDP should attempt to determine the value types for resources and subjects
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2112'>DDF-2112</a> - Change the default transformer to XML for catalog:ingest and catalog:dump commands
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2113'>DDF-2113</a> - Move the subject role check in org.codice.ddf.security.common.Security
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2114'>DDF-2114</a> - Add modal pop up support for the admin UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2117'>DDF-2117</a> - Add failover configuration for security logs
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2118'>DDF-2118</a> - Remove orphaned project directories
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2121'>DDF-2121</a> - Add scripts to the catalog-app for windows and unix that call the `catalog:dump --include-content` command and sign the created zip file to aid administrators with import/export
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2124'>DDF-2124</a> - Update registry api interfaces from ddf-registry repo
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2126'>DDF-2126</a> - klv library needs an isError method
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2133'>DDF-2133</a> - As an administrator, I want an option to configure the FTP endpoint&#39;s port number.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2134'>DDF-2134</a> - Move the /cometd context so it is beneath the /search context
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2135'>DDF-2135</a> - Rename export script for catalog:dump command and add additional status messages in script
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2136'>DDF-2136</a> - The catalog:dump --include-content command should place the dumped metacards in a metacard folder
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2137'>DDF-2137</a> - Spatial Connected Sources should be uninstalled by default.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2140'>DDF-2140</a> - Add polling to check if subscription response handlers are available
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2143'>DDF-2143</a> - The SchematronValidationService should sanitize its messages
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2146'>DDF-2146</a> - Update Registry itests 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2147'>DDF-2147</a> - Search UI Download Notifications Aren&#39;t Delivered to the UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2151'>DDF-2151</a> - Add checks for bad filenames and mime types to content storage
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2152'>DDF-2152</a> - ActionProviders that simply present an attribute should only be available if that attribute exists
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2154'>DDF-2154</a> - write a better FileBackedOutputStream for ddf
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2158'>DDF-2158</a> - Batch audit logging related to isPermitted to improve performance
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2159'>DDF-2159</a> - Add a new SubjectUtils helper for getting subject attributes.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2168'>DDF-2168</a> - Add integration tests for resource download caching
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2175'>DDF-2175</a> - As an Integrator, I need the DDF to provide an indicator of whether or not a file has already been cached, so that I may display the visual indicator within the returned search results to allow for a quick download
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2177'>DDF-2177</a> - Add a configuration property to the registry store, &#39;Auto push identity node&#39;, which is disabled by default
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2180'>DDF-2180</a> - As an integrator, I would like support for FTPS on the FTP endpoint
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2190'>DDF-2190</a> - Address registry edit ui usability feedback
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2197'>DDF-2197</a> - Remove all maven repositories other than maven central and codice nexus
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2199'>DDF-2199</a> - Add utility to registry to help with common registry metacard operations
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2221'>DDF-2221</a> - Make the FeatureType a configurable property for WFS Sources
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2229'>DDF-2229</a> - Improve csw queries to support fuzzy searches
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2230'>DDF-2230</a> - Document the SaxEventHandlers, SaxEventHandlerFactories, and the pluggable Input Transformer
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2233'>DDF-2233</a> - Add CQL support to catalog-export scripts
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2235'>DDF-2235</a> - Update ffmpeg to address related OWASP findings
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2243'>DDF-2243</a> - Prevent linking content when exporting history metacards using the catalog:dump --include-content command
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2254'>DDF-2254</a> - As a user, I want to be able to switch between registry types when adding a remote registry in the registry-app.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2255'>DDF-2255</a> - offline-gazetteer fails to install
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2257'>DDF-2257</a> - Prevent linking content when exporting history metacards using the catalog:dump --include-content command
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2258'>DDF-2258</a> - The JSON definition file parser should handle file updates and deletes
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2260'>DDF-2260</a> - Update PDF Input Tranformer to Extract GeoPDF Metadata
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2266'>DDF-2266</a> - Registry metacards should be deleted when the associated remote registry is deleted
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2267'>DDF-2267</a> - Update the local nodes UI for Registry to display the list of all nodes instead of just local
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2268'>DDF-2268</a> - Update PdfInputTransformer to use expanded taxonomy
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2271'>DDF-2271</a> - Update Install Profile to include Registry
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2273'>DDF-2273</a> - The ResourceManagement App no longer reports Data Usage Limit exceeded in the UI when a download fails
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2275'>DDF-2275</a> - Implement new Taxonomy in the Catalog Core API
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2276'>DDF-2276</a> - The ResourceManagement App Data Usage plugin should provide the ability to choose to restrict usage for federated sources only
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2282'>DDF-2282</a> - Update FederationAdminService to directly query remote registry instead of using the catalog framework
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2302'>DDF-2302</a> - Clarify the tooltip for &quot;Enforced Validator&quot; in Metacard Validation Marker Plugin
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2303'>DDF-2303</a> - Add authentication support to CometD client used in integration tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2304'>DDF-2304</a> - Fix relative paths in DDF to use ddf.home
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2308'>DDF-2308</a> - Update the Tika and Pdf InputTransformers to inject attributes to BasicMetacard
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2315'>DDF-2315</a> - Add support for download cancellation to the CometDClient
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2325'>DDF-2325</a> - As a user, I would like geolocated metacards to also have attributes for ISO_3166-1 Alpha3 country codes so that I can search by country code.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2328'>DDF-2328</a> - Update the Gmd transformers to use the expanded taxonomy
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2332'>DDF-2332</a> - Add type attribute to core taxonomy
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2338'>DDF-2338</a> - Update references to renamed attributes in the taxonomy
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2339'>DDF-2339</a> - Fix typo in CSW Metatype Metacard Mappings 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2340'>DDF-2340</a> - Add test coverage to CswUnmarshallHelper.convertToDate for xs:gYearMonth and xs:gYear
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2341'>DDF-2341</a> - Update PdfInputTransformer to use expanded taxonomy
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2348'>DDF-2348</a> - dateTime slots in Registry Metacard XML should have the xs namespace 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2351'>DDF-2351</a> - Replace external REST endpoint used to download resources with internal one
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2363'>DDF-2363</a> - Source polling interval should be configurable
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2367'>DDF-2367</a> - update GmdConverter so that it can be extended to create MgmpConverter
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2379'>DDF-2379</a> - Update PPTX InputTransformer to use filename for title
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2384'>DDF-2384</a> - Clarify, test, and optimize methods to validate query response and update source ids in fanout mode
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2386'>DDF-2386</a> - As an administrator, I need a configurable resource download data limit so that I can enforce my specific download requirements.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2388'>DDF-2388</a> - Align GMD CSW ISO Source and Endpoint Metacard Mappings to the expanded taxonomy
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2409'>DDF-2409</a> - code clarity change in UpdateOperations
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2413'>DDF-2413</a> - Update CSW Record Transformers to use the expanded taxonomy
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2430'>DDF-2430</a> - As a system admin, I would like the catalog:replicate (and like commands) allow for a temporal argument of last N seconds (or milliseconds) and to specify which time type so that the administrator can issue very specific relative time ranges.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2445'>DDF-2445</a> - Trash icon for deleting remote registry nodes should verify before deleting
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2449'>DDF-2449</a> - Remote Nodes in the registry should be sorted alphabetically
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2458'>DDF-2458</a> - Provide finer grain control of registry write operations.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2467'>DDF-2467</a> - Investigate Solr Cloud support
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2468'>DDF-2468</a> - FTP endpoint should support mkdir, cwd, and other commands
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2475'>DDF-2475</a> - Add srsName paramter to Wfs 2.0 Source GetFeature requests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2477'>DDF-2477</a> - Infer Default Port Assignment from HTTP/S Ports in System Configuration Settings
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2478'>DDF-2478</a> - As a developer, I want a utility to convert country codes from FIPS 10-4 to ISO_31661-1
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2479'>DDF-2479</a> - Remove WFS Endpoint from DDF baseline
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2491'>DDF-2491</a> - The replicate command issues 2 queries when 1 would suffice. As an integrator I would prefer only 1 query would be issued so that the system will perform better.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2495'>DDF-2495</a> - update org.apache.lucene.lucene-core to 6.0.0
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2496'>DDF-2496</a> - Add camel-jackson feature to support marshalling and unmarshalling Java objects to and from JSON via camel routes
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2499'>DDF-2499</a> - The Resource Derived Download URL values in the Standard Search UI are not anchors
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2503'>DDF-2503</a> - The Content Monitor attribute override should take effect after the addition of injectable attributes so the administrator can inject and then set an attribute
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2508'>DDF-2508</a> - Allow for the ingest of a resource and a metacard at the same time
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2514'>DDF-2514</a> - Update coordinate order drop-down descriptions for sources
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2515'>DDF-2515</a> - As an administrator, I would like commands that support the --cql option to also support temporal options
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2520'>DDF-2520</a> - Improve Ingest Performance and Versioning Space Usage
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2528'>DDF-2528</a> - Implement a validator for Location.COUNTRY_CODE metacard attribute
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2529'>DDF-2529</a> - Update MetacardMapperImpl to latest DDF taxonomy
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2533'>DDF-2533</a> - Investigate and possibly remove XLogger references
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2534'>DDF-2534</a> - Add autocomplete/suggestion support to the registry node modal fields
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2545'>DDF-2545</a> - Fix itest slowdown in TestFederation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2546'>DDF-2546</a> - Creating announcements with dates is difficult for the Landing Page
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2558'>DDF-2558</a> - Add debug logging for the VideoThumbnailPlugin to help diagnose issues that can arise from Thumbnail Generation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2559'>DDF-2559</a> - As a user, I would like to add my own custom links to the landing page
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2560'>DDF-2560</a> - Update WFS Feature MetacardTypes to use taxonomy attributes
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2565'>DDF-2565</a> - Upgrade Apache Camel to 2.18.0 and Artemis to 1.4.0
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2567'>DDF-2567</a> - Add support for binary type in persistence store
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2569'>DDF-2569</a> - Create PostProcessPlugin API
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2571'>DDF-2571</a> - Create ProcessingFramework API
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2578'>DDF-2578</a> - Add more flexible API to XMLUtils
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2579'>DDF-2579</a> - Update all sources, the SP, and IdP to follow the SAML ECP spec
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2583'>DDF-2583</a> - Add user bind methods to LDAP.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2587'>DDF-2587</a> - Tika Input Transformer Metacards do not have a datatype
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2602'>DDF-2602</a> - Allow the LDAP services to be configured to ignore user DNs from certificates
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2607'>DDF-2607</a> - Upgrade ActiveMQ Artemis to 1.5.0 and add an OpenWire acceptor
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2609'>DDF-2609</a> - Separate the attributes for user login and group membership so that group membership can be based on a different DN than the one used to log a user into LDAP
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2645'>DDF-2645</a> - Map common file type metadata to taxonomy for use by metacard fallback transformer (tika)
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2659'>DDF-2659</a> - Leverage Ant&#39;s SignJar functionality in ZipCompression instead of forcing an Administrator to sign the jar manually
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2669'>DDF-2669</a> - Update WFS 1.0.0 Source to leverage GeoTools transformation functionality to support various projections
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2672'>DDF-2672</a> - Update the Tika input transformer to include mime type aliases
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2702'>DDF-2702</a> - Add system usage message to Catalog UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2703'>DDF-2703</a> - Update metacard view to cancel in progress searches as soon as the metacard is found
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2706'>DDF-2706</a> - Add configuration option to disable editing of results in the Catalog UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2707'>DDF-2707</a> - Both Search UI&#39;s attempt to access an external imagery server regardless of configuration
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2708'>DDF-2708</a> - Publish WorkspaceMetacardTypes as a service so it can be referenced throughout DDF
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2709'>DDF-2709</a> - Update platform command scheduler to support cron scheduling
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2712'>DDF-2712</a> - Allow banner customization based on the chosen security profile
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2724'>DDF-2724</a> - Add the ability to search historical data to the individual query drop down menu in the Catalog UI.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2738'>DDF-2738</a> - Add video input transformer and metacard type.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2749'>DDF-2749</a> - Hide enterprise and jdbc features in admin app list
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2751'>DDF-2751</a> - Repackage the tika-bundle with full poi xml schemas so that the tika input transformer can parse metadata out of MS Visio files
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2763'>DDF-2763</a> - Update standing searches retry in cases where the request was successful, but the query was not
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2765'>DDF-2765</a> - Set default CSW and WFS &quot;force spatial filter&quot; to &quot;NO_FILTER&quot; instead of null
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2787'>DDF-2787</a> - Action urls may not be properly created in the Catalog UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2808'>DDF-2808</a> - Add Integration Tests to TestSpatial for ogc:Not filters in order to prevent regressions
	</li>
</ul>
    
<h3>Documentation</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1124'>DDF-1124</a> - Create a Dependency Matrix showing current DDF 3rd party dependencies
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1788'>DDF-1788</a> - DDF documentation still refers to old .cfg configuration files
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1793'>DDF-1793</a> - Update Configurable Properties List
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1794'>DDF-1794</a> - HTTP Port Config Table needs a Property Column
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1795'>DDF-1795</a> - Expansion Configuration Files are not documented
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1796'>DDF-1796</a> - Update securing section in Management document
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1797'>DDF-1797</a> - Signing a JAR/KAR is missing an Introduction 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1798'>DDF-1798</a> - Enable SSL for Clients section is duplicated
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1805'>DDF-1805</a> - Configuring Global System Properties should be formatted as a table and appropriately combined
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1867'>DDF-1867</a> - UI Development Recommendation section needs content
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1963'>DDF-1963</a> - Automate generation of Managing documentation for individual applications
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2141'>DDF-2141</a> - Update Example Query Response table and Activity Events table
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2149'>DDF-2149</a> - Improve web context policy configuration documentation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2155'>DDF-2155</a> - Document DDF registry for administrators
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2157'>DDF-2157</a> - Document DDF registry for integrators
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2163'>DDF-2163</a> - Document installation and configuration of GeoWebCache app
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2167'>DDF-2167</a> - Remove DDfConfigurationWatcher and DdfConfigurationManager from Documentation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2174'>DDF-2174</a> - Create maven properties for default host url prefixes
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2218'>DDF-2218</a> - Document Metacard Validity Marker Plugin
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2280'>DDF-2280</a> - Document XML Input Transformer
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2309'>DDF-2309</a> - Revise Integrating documentation by capability
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2359'>DDF-2359</a> - Thumbnail generation can cause image ingests to fail when running on a headless server causing system admins to need to update the ddf start script
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2398'>DDF-2398</a> - Update Endpoint section in Integrating docs
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2425'>DDF-2425</a> - Update DDF documentation to include examples of all imagery provider configurations.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2428'>DDF-2428</a> - Create documentation for the Resource Management Application
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2471'>DDF-2471</a> - Add content to core concepts section in documentation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2472'>DDF-2472</a> - Update Configuring Security and Hardening documentation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2481'>DDF-2481</a> - Add Windows versions of command line instructions in Managing Documentation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2541'>DDF-2541</a> - Update docs for Configuring a Java Keystore
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2554'>DDF-2554</a> - Remove incorrect maven property resolving in code sample
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2593'>DDF-2593</a> - Remove outdated screenshots from Search UI docs
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2624'>DDF-2624</a> - revise &quot;Metacard Required Attributes&quot; section of docs to reflect taxonomy changes.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2631'>DDF-2631</a> - Fix image linkings breaking in PDFs when building on Windows.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2682'>DDF-2682</a> - Document Neutral Taxonomy for Integrators
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2737'>DDF-2737</a> - Add taxonomy attributes to docs not included in wiki
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2748'>DDF-2748</a> - Prepare release documentation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2766'>DDF-2766</a> - Landing Page&#39;s Additional Links documentation does not reflect actual behavior
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2770'>DDF-2770</a> - Update LogViewer documentation to add additional information
	</li>
</ul>
    
<h3>Technical Debt</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-420'>DDF-420</a> - Remove or update spatial-wfs-endpoint
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1937'>DDF-1937</a> - Update PreIngest Registry Creation Process to add External Identifier
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1940'>DDF-1940</a> - Fix potential issue with query tags and fanout proxy
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2037'>DDF-2037</a> - MetacardValidityChecker may prevent non-catalog queries from returning results
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2064'>DDF-2064</a> - Investigate and fix DDF itest Windows CI failures
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2092'>DDF-2092</a> - DDF fails to build on windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2100'>DDF-2100</a> - Add registry-common unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2101'>DDF-2101</a> - Update registry-federation-admin-impl unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2102'>DDF-2102</a> - Update registry-federation-admin-service-impl unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2104'>DDF-2104</a> - Update registry-policy-plugin unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2105'>DDF-2105</a> - Create registry-report-viewer unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2106'>DDF-2106</a> - Update registry-schema-bindings unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2107'>DDF-2107</a> - Create registry-source-configuration-handler unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2153'>DDF-2153</a> - Disable NPM install scripts and address any consequent problems
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2172'>DDF-2172</a> - Use https to download packages from Maven Central and other Maven repositories
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2173'>DDF-2173</a> - Fix OWASP Failures
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2176'>DDF-2176</a> - Break up FederationAdminServiceImpl to reduce the class&#39; complexity and better adhere to the Single Responsibility Principle
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2182'>DDF-2182</a> - waitForReady startup script broken. 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2185'>DDF-2185</a> - Create new cache interface and deprecate ResourceCacheInterface
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2202'>DDF-2202</a> - Remove dependency between CatalogFrameworkImpl and ResourceCache
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2237'>DDF-2237</a> - Update Cesium Version
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2253'>DDF-2253</a> - Security common class should not hardcode an admin role
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2262'>DDF-2262</a> - default subscription polling interval when certificate expiration not present
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2281'>DDF-2281</a> - Add automated tests for simultaneous downloads
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2306'>DDF-2306</a> - NPE thrown from RegistryPolicyPlugin when performing update when the metacard to be updated can&#39;t be found in the catalog
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2322'>DDF-2322</a> - GenericFileOperationFailedException thrown intermittently during Content Directory Monitor ingest
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2323'>DDF-2323</a> - LdapClaimsHandler logs stack traces as ERRORS, which can flood the log, making it hard to read
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2354'>DDF-2354</a> - The user experience on the login and logout pages needs to be improved
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2355'>DDF-2355</a> - Docs fail to build using Oracle JDK on Linux
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2357'>DDF-2357</a> - Add search capability to the Cometd test client
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2358'>DDF-2358</a> - Update system properties to include new GCM ciphers available in Java 8
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2361'>DDF-2361</a> - Enable fanout and ddf registry to be used at the same time in the same ddf instance
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2370'>DDF-2370</a> - AuthNStatementProvider creates a new RNG for each request, which could hurt performance
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2382'>DDF-2382</a> - Update registry to use new metacard attribute taxonomy.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2390'>DDF-2390</a> - Review log messages throughout DDF and change logging level appropriately
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2422'>DDF-2422</a> - TestFederation Tests Fail Intermittently on Bamboo
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2423'>DDF-2423</a> - Improve code clarity throughout system
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2426'>DDF-2426</a> - Create Spock shaded jar and add gmavenplus plugin to root pom
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2433'>DDF-2433</a> - Remove unused SecuritySettingsService interface
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2437'>DDF-2437</a> - MetacardVersion cleanup
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2442'>DDF-2442</a> - Disable the use of inline DTD in XML
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2444'>DDF-2444</a> - Filtering on Metacard.TAGS causes itest failures
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2450'>DDF-2450</a> - Attributes with identical keys cause ingest failure when history is enabled
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2452'>DDF-2452</a> - Add javadoc and sources to the release profile
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2462'>DDF-2462</a> - Start and stop feature utility methods used for integration tests do not wait when requested
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2470'>DDF-2470</a> - FtpTest integration tests fail intermittently
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2498'>DDF-2498</a> - As an administrator, I would like the migrate command to have a list of catalog providers, and be able to specify the source and target for migration	
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2518'>DDF-2518</a> - Add methods to RegistryPackageTypeHelper that returns associated objects
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2521'>DDF-2521</a> - Refactor implementations of SaxEventHandler to use an AbstractSaxEventHandler
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2530'>DDF-2530</a> - Fix the crlinterceptor pom to use ${project.version}
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2531'>DDF-2531</a> - Update GeoCoderPlugin to handle UpdateRequests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2537'>DDF-2537</a> - Unit test testCanValidateBadIp fails when invalid IPs are resolved by the DNS provider
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2550'>DDF-2550</a> - itests can hang indefinitely if executing a command as the system subject fails
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2592'>DDF-2592</a> -  Quickbuilds on Windows fail, mostly due to node/npm issues
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2596'>DDF-2596</a> - ApplicationImpl unchecked String error logs misleading errors during install
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2608'>DDF-2608</a> - schematronvalidationservice should compile rulesets asynchronously
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2611'>DDF-2611</a> - Setting a cookie path for some servlets causes a meaningless exception
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2616'>DDF-2616</a> - Protect against DTD and entity expansion
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2621'>DDF-2621</a> - AttributeRegistry allows duplicate attribute definitions
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2622'>DDF-2622</a> - Enforce UTF-8 in Integration Tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2661'>DDF-2661</a> - Point of Contact attribute(s) not consistent across MetacardTypes
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2713'>DDF-2713</a> - Fix flaky itest issues
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2717'>DDF-2717</a> - Upgrade Apache Shiro to version 1.2.5 or higher
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2720'>DDF-2720</a> - Address null dereference issues
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2721'>DDF-2721</a> - Replace npm with yarn
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2761'>DDF-2761</a> - Fixing multithreading issue in FederationStrategyTest
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2764'>DDF-2764</a> - Metadata containing GeometryCollections with intersecting polygons fails to ingest
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2768'>DDF-2768</a> - Shut-down of DDF will not occur if the camel route in the http proxy service is waiting for an unavailable imagery provider
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2776'>DDF-2776</a> - Change constant for Associations.EXTERNAL from metacard.associations.external to associations.external
	</li>
</ul>

<h3>Dependency Upgrade</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1945'>DDF-1945</a> - Update org.apache.karaf.bundle dependency in platform/admin/admin-app/pom.xml
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2079'>DDF-2079</a> - Upgrade to Karaf 4.0.5
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2088'>DDF-2088</a> - Update DDF to use Arbitro instead of Balana
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2127'>DDF-2127</a> - Update Apache Tika to version 1.13
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2201'>DDF-2201</a> - Upgrade npm to strengthen ddf security
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2264'>DDF-2264</a> - Upgrade Camel to 2.16.3 and add camel-amqp support
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2364'>DDF-2364</a> - Update commons-fileupload to 1.3.2
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2383'>DDF-2383</a> - Upgrade to CXF 3.1.7
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2406'>DDF-2406</a> - Upgrade to opendj-osgi 1.3.3
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2505'>DDF-2505</a> - Update logback classic to version 1.1.7
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2511'>DDF-2511</a> - Update karaf to 4.0.7 to fix blueprint issues
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2568'>DDF-2568</a> - Upgrade frontend-maven-plugin from 0.0.28 to 1.2
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2614'>DDF-2614</a> - Update asciidoctor to latest
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2689'>DDF-2689</a> - Update grunt to 1.0.1
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2752'>DDF-2752</a> - Investigate removal or upgrade of Terracotta Toolkit in Catalog UI
	</li>
</ul>


## ddf-2.9.4
	Release Date: 2016-12-16    
<h3>Bug</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2507'>DDF-2507</a> - CSW queries that include numeric criteria always return 0 results
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2564'>DDF-2564</a> - Can&#39;t login to the search ui on windows due to cometd endpoint failing to initialize, forcing user to restart DDF multiple times.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2591'>DDF-2591</a> - Node information updates don&#39;t always propagate to published locations.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2599'>DDF-2599</a> - Multivalued URL attributes don&#39;t show up as links in the standard search ui
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2613'>DDF-2613</a> - The Latitude and Longitude attributes are editable when they should be read only for a remote node on the Node Information tab of the DDF Registry.
	</li>
</ul>

<h3>Task</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2642'>DDF-2642</a> - Remove accordion view from sources tab.
	</li>
</ul>
    
<h3>Improvement</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2583'>DDF-2583</a> - Add user bind methods to LDAP.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2602'>DDF-2602</a> - Allow the LDAP services to be configured to ignore user DNs from certificates
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2609'>DDF-2609</a> - Separate the attributes for user login and group membership so that group membership can be based on a different DN than the one used to log a user into LDAP
	</li>
</ul>
    
<h3>Documentation</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2593'>DDF-2593</a> - Remove outdated screenshots from Search UI docs
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2603'>DDF-2603</a> - Create a summary view of supported document formats for DDF ingest
	</li>
</ul>
     
<h3>Technical Debt</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2550'>DDF-2550</a> - itests can hang indefinitely if executing a command as the system subject fails
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2598'>DDF-2598</a> - Add logging in itests to help fix &quot;Exam Setup Failed&quot;
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2611'>DDF-2611</a> - Setting a cookie path for some servlets causes a meaningless exception
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2616'>DDF-2616</a> - Protect against DTD and entity expansion
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2705'>DDF-2705</a> - Remove old Catalog Solr Provider
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2796'>DDF-2796</a> - Remove old Catalog Solr Provider
	</li>
</ul>
    

## ddf-2.9.3
	Release Date: 2016-10-16
<h3>Bug</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1789'>DDF-1789</a> - Empty query is created in Workspace if &#39;&lt; Workspace&#39; is clicked instead of cancel button
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2171'>DDF-2171</a> - In the Standard Search UI faceted search view, submitting a textual search with an empty value causes an exception in the search endpoint
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2316'>DDF-2316</a> - Tests and OWASP fail on Windows build
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2345'>DDF-2345</a> - Registry Node Information screen will not display information if an invalid date is in any node&#39;s information 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2349'>DDF-2349</a> - Percentages in the Ingest Modal on the Standard Search UI show up as NaN when they should be 0%
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2432'>DDF-2432</a> - Metacard validation UI incorrectly reports a Metacard is a duplicate of itself
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2434'>DDF-2434</a> - Federated opensearch source url isn&#39;t populated correctly when it is generated by the registry
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2438'>DDF-2438</a> - Configuring a registry in loopback mode (a client to itself) causes the identity node to be duplicated.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2446'>DDF-2446</a> - Checked items in Search UI&#39;s Specific Sources drop-down get out of sync with label and makes federated searches confusing
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2455'>DDF-2455</a> - SchematronValidationService incorrectly appears as a Source in the Sources Admin UI tab
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2457'>DDF-2457</a> - Improve the Sources Tab with appropriate and current status of the Sources
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2459'>DDF-2459</a> - Content directory monitor stops finding new files, requiring administrators to refresh configuration settings to trigger updates
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2465'>DDF-2465</a> - If a source or type gets added, removed, or changed, the Search UI will throw an error
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2474'>DDF-2474</a> - Ingesting a large zip file containing metacards/content ingests only some of the data
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2480'>DDF-2480</a> - Search UI upload dialog cannot be re-opened if closed when file upload in progress
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2504'>DDF-2504</a> - Multiple SSO sessions are assigned per browser, which can cause unexpected logout from UI, forcing the user to log in again.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2506'>DDF-2506</a> - Sorting by distance can take minutes on large indexes
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2513'>DDF-2513</a> - Password fields in Admin UI appear to be populated with a password when none has been set
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2538'>DDF-2538</a> - Some fields in the Search UI aren&#39;t correctly escaping some characters
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2540'>DDF-2540</a> - Unable to ingest mpegts clips that have incomplete TS packets
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2542'>DDF-2542</a> - Changing a dropdown list value in the sources tab does not save the change
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2543'>DDF-2543</a> - CSW product retrieval may fail due to a missing security subject
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2548'>DDF-2548</a> - Queries to SDK Twitter source fail with nullpointer exception
	</li>
</ul>

<h3>Task</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1324'>DDF-1324</a> - Create documentation for Extending DDF-Admin
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2156'>DDF-2156</a> - Document DDF registry for developers
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2539'>DDF-2539</a> - Cannot log into the Standard Search UI with IE 11 or Firefox on Windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2551'>DDF-2551</a> - Update DDF 2.9 for minor releasability issues
	</li>
</ul>
    
<h3>Improvement</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2279'>DDF-2279</a> - Improve the HTTP stub server to support the range header
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2348'>DDF-2348</a> - dateTime slots in Registry Metacard XML should have the xs namespace 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2391'>DDF-2391</a> - The Log Viewer needs to display logs above a certain level
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2415'>DDF-2415</a> - Publish/Unpublish to Registry text on the source modal should provide feedback if the action has been done
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2420'>DDF-2420</a> - Local Site should not show up as disabled in the Sources tab
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2445'>DDF-2445</a> - Trash icon for deleting remote registry nodes should verify before deleting
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2449'>DDF-2449</a> - Remote Nodes in the registry should be sorted alphabetically
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2453'>DDF-2453</a> - Prevent the Log Viewer from scrolling when new logs are received
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2454'>DDF-2454</a> - Add ability to refresh registry generated sources
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2458'>DDF-2458</a> - Provide finer grain control of registry write operations.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2461'>DDF-2461</a> - Registry identity node name should use the site name from installer
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2534'>DDF-2534</a> - Add autocomplete/suggestion support to the registry node modal fields
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2546'>DDF-2546</a> - Creating announcements with dates is difficult for the Landing Page
	</li>
</ul>
    
<h3>Documentation</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2425'>DDF-2425</a> - Update DDF documentation to include examples of all imagery provider configurations.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2510'>DDF-2510</a> - Institute a CHANGELOG for DDF releases
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2553'>DDF-2553</a> - Update documentation on codice.org
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2554'>DDF-2554</a> - Remove incorrect maven property resolving in code sample
	</li>
</ul>
    
<h3>Technical Debt</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2295'>DDF-2295</a> - Create helper methods on org.codice.ddf.platform.util.XMLUtil
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2353'>DDF-2353</a> - Misleading Tooltips on System Configuration Settings in Admin Console
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2361'>DDF-2361</a> - Enable fanout and ddf registry to be used at the same time in the same ddf instance
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2378'>DDF-2378</a> - Add &#39;internal&#39; to registry packages that export service interfaces and to the rest endpoint path
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2382'>DDF-2382</a> - Update registry to use new metacard attribute taxonomy.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2452'>DDF-2452</a> - Add javadoc and sources to the release profile
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2462'>DDF-2462</a> - Start and stop feature utility methods used for integration tests do not wait when requested
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2536'>DDF-2536</a> - Add component team links to PR template
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2537'>DDF-2537</a> - Unit test testCanValidateBadIp fails when invalid IPs are resolved by the DNS provider
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2585'>DDF-2585</a> - Add locale to system properties
	</li>
</ul>
    
<h3>Dependency Upgrade</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2511'>DDF-2511</a> - Update karaf to 4.0.7 to fix blueprint issues
	</li>
</ul>


## ddf-2.9.2
	Release Date: 2016-08-28
<h3>Bug</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1804'>DDF-1804</a> - Metacards with MultiPolygon geometry cause a cesium rendering error when results are rendered in the standard search-ui
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2270'>DDF-2270</a> - Various UIs in DDF are fetching fonts from the internet which causes them to load slowly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2284'>DDF-2284</a> - Platform Command Scheduler fails to run scheduled jobs due to a NullPointerException, forcing administrator to manually run commands in the karaf console
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2288'>DDF-2288</a> - XacmlPdp should format IPv6 attributes to conform to RFC-2732
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2289'>DDF-2289</a> - SchematronValidationService should not fail on null/empty metadata, because this erroneously flag metacards as invalid
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2290'>DDF-2290</a> - Catalog-app features won&#39;t start without access to a populated maven repo
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2292'>DDF-2292</a> - Deploying sdk-app after compiling from source fails to deploy to maven repository
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2300'>DDF-2300</a> - Range header not supported by CSW endpoint which causes resources to be corrupted
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2301'>DDF-2301</a> - Mapquest no longer provides a public tile service so the maps are no longer rendering correctly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2307'>DDF-2307</a> - Saved map layer preferences in the UI are not updated with the most recent URLs from the configuration causing any users with saved layers to lose all map tiles
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2311'>DDF-2311</a> - Web context policies for paths that extend beyond a servlet&#39;s context path are not applied
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2324'>DDF-2324</a> - The DDF Registry App does not stop when deactivated
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2326'>DDF-2326</a> - Search UI Summary tab does not show Nearby cities
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2327'>DDF-2327</a> - Content resource URIs should not be overwritable
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2337'>DDF-2337</a> - Ingested Metacards that fail security checks attempt to log the ID, which does not exist.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2342'>DDF-2342</a> - Registry updates are lost when multiple ddf instances try to update the same node at the same time
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2346'>DDF-2346</a> - If the Identity Node contains special characters it cannot be displayed/edited using the Admin UI, causing System Administrators to resort to command line workarounds.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2347'>DDF-2347</a> - Queries against the cache don&#39;t filter out non-resource metacards, causing users to see unintended results
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2392'>DDF-2392</a> - OpenDJ Embedded LDAP server feature fails on Windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2407'>DDF-2407</a> - Application Config Installer lacks permissions to auto-install apps, preventing unattended installs
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2416'>DDF-2416</a> - Clients using scoped ipv6 addresses are denied guest access
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2464'>DDF-2464</a> - Local search results are still returned after local source is unselected from query sources
	</li>
</ul>

<h3>Task</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2286'>DDF-2286</a> - Define the final version of new Reliable Resource API
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2335'>DDF-2335</a> - Disable Hazelcast version check on startup
	</li>
</ul>
    
<h3>Improvement</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1965'>DDF-1965</a> - Update Bamboo plans to rebuild only dependent maven modules on DDF commit
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2235'>DDF-2235</a> - Update ffmpeg to address related OWASP findings
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2254'>DDF-2254</a> - As a user, I want to be able to switch between registry types when adding a remote registry in the registry-app.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2267'>DDF-2267</a> - Update the local nodes UI for Registry to display the list of all nodes instead of just local
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2271'>DDF-2271</a> - Update Install Profile to include Registry
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2282'>DDF-2282</a> - Update FederationAdminService to directly query remote registry instead of using the catalog framework
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2308'>DDF-2308</a> - Update the Tika and Pdf InputTransformers to inject attributes to BasicMetacard
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2384'>DDF-2384</a> - Clarify, test, and optimize methods to validate query response and update source ids in fanout mode
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2411'>DDF-2411</a> - Improve presentation of Validate Command
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2566'>DDF-2566</a> - Update the NITF Content Resolver with additional file extensions
	</li>
</ul>
    
<h3>Documentation</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1963'>DDF-1963</a> - Automate generation of Managing documentation for individual applications
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2359'>DDF-2359</a> - Thumbnail generation can cause image ingests to fail when running on a headless server causing system admins to need to update the ddf start script
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2365'>DDF-2365</a> - Update recommended Java version to 8u60+
	</li>
</ul>
    
<h3>Technical Debt</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1940'>DDF-1940</a> - Fix potential issue with query tags and fanout proxy
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2306'>DDF-2306</a> - NPE thrown from RegistryPolicyPlugin when performing update when the metacard to be updated can&#39;t be found in the catalog
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2313'>DDF-2313</a> - SourceConfigurationHandler doesn&#39;t handle multiple bindings with same fpid but different bindingTypes
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2322'>DDF-2322</a> - GenericFileOperationFailedException thrown intermittently during Content Directory Monitor ingest
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2354'>DDF-2354</a> - The user experience on the login and logout pages needs to be improved
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2358'>DDF-2358</a> - Update system properties to include new GCM ciphers available in Java 8
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2370'>DDF-2370</a> - AuthNStatementProvider creates a new RNG for each request, which could hurt performance
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2448'>DDF-2448</a> - Cleanup resource download integration tests
	</li>
</ul>
    
<h3>Dependency Upgrade</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2364'>DDF-2364</a> - Update commons-fileupload to 1.3.2
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2406'>DDF-2406</a> - Upgrade to opendj-osgi 1.3.3
	</li>
</ul>


## 2.9.1
	Release Date: 2016-06-16
<h3>Bug</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1369'>DDF-1369</a> - HttpProxy needs to handle compressed content in a more robust manner
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1460'>DDF-1460</a> - GeoJsonInputTransformer does not correctly support a geometry type of GeometryCollection
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1927'>DDF-1927</a> - CSW Serialization does not retain sort order of a query response
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2035'>DDF-2035</a> - Data ingested via CSW is modified, but should maintain transaction integrity.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2083'>DDF-2083</a> - Password values can be seen as clear text when inspecting the password’s input element in the Admin UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2086'>DDF-2086</a> - Remove upstream dependency from platform-configuration-impl
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2087'>DDF-2087</a> - Fix CswFilterDelegate to set the propertyIsLike filter matchCase flag.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2089'>DDF-2089</a> - Streaming input transformers don&#39;t declare a mimetype
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2090'>DDF-2090</a> - GeoJSON input transformer doesn&#39;t handle multiple values correctly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2091'>DDF-2091</a> - Solr doesn&#39;t handle the multiple values for the metacard-tags field correctly.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2097'>DDF-2097</a> - DDF federate source service fails cache and to return the federated source list in a timely fashion 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2098'>DDF-2098</a> - PDFs missing a modification date can fail to ingest
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2115'>DDF-2115</a> - setenv modifications do not work in non-bash shells - update to be posix compatible
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2116'>DDF-2116</a> - NPM related build errors in Bamboo
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2122'>DDF-2122</a> - CSW Endpoint logs erroneous warnings that 2 operations are equal when they are not
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2123'>DDF-2123</a> - ffmpeg is logging to System.out when creating new video thumbnails
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2131'>DDF-2131</a> - Previous versions of metacards can be returned along with ingested metacards
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2138'>DDF-2138</a> - File Install config creates directory with comment in name
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2148'>DDF-2148</a> - CSW federated source created with bad credentials will not immediately reconnect after they are fixed
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2150'>DDF-2150</a> - Windows attribute .cfg files do not get parsed properly in AbstractExpansion
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2166'>DDF-2166</a> - Log viewer UI should specify only exact versions of NPM dependencies
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2178'>DDF-2178</a> - FederationAdminService doesn&#39;t initialize because init doesn&#39;t have a subject to run its commands.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2183'>DDF-2183</a> - STS Ldap Login bundle ignores configured usernameAttribute
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2198'>DDF-2198</a> - Advanced search not correctly filtering on metadata content type
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2209'>DDF-2209</a> - The LDAP claims handler should only be decrypting passwords for keystores if it is going to use them
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2216'>DDF-2216</a> - DDF does not update its federated source list when a federated source changes its name
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2219'>DDF-2219</a> - The registry source and publication handlers don&#39;t have the needed permission to execute some of their methods
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2220'>DDF-2220</a> - Additional query parameters are not preserved after a search is performed
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2222'>DDF-2222</a> - PDP does not have the correct mappings set up for expansion services
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2224'>DDF-2224</a> - DDF does not work in offline mode
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2232'>DDF-2232</a> - Update pom-fix to run correctly on a maven build.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2238'>DDF-2238</a> - Can&#39;t compile from source due to branding-api bundle being located in distribution
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2242'>DDF-2242</a> - Fix unit test filename access to work correctly on Windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2250'>DDF-2250</a> - SaxEventToXmlConverter does not handle nested namespace redeclarations
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2252'>DDF-2252</a> - Cannot uninstall specific sdk-app features
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2259'>DDF-2259</a> - Registry IdentificationPlugin is not handling updates updates correctly
	</li>
</ul>

<h3>Story</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1631'>DDF-1631</a> - Display the cities closest to a metacard in the Search UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2236'>DDF-2236</a> - Verify New Features in Release Notes pages are appropriately documented.
	</li>
</ul>
    
<h3>New Feature</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-2119'>DDF-2119</a> - As an Integrator, I want to be able to trigger the caching of selected products in order to allow users to download them from the cache at a later time
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2120'>DDF-2120</a> - As an Integrator, I need the DDF API to provide a status of the number of results returned/success/failed/partial of the query that is in progress
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2196'>DDF-2196</a> - Separate admin duties via Admin Console
	</li>
</ul>
    
<h3>Task</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-810'>DDF-810</a> - Whenever there is a DDF Distribution release the documentation should be versioned for that release
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1829'>DDF-1829</a> - Create Default expansion configuration files 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1886'>DDF-1886</a> - Create the UI for editing local node information
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1964'>DDF-1964</a> - Update Sources Tab to show additional information for registry entries
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2008'>DDF-2008</a> - Create ActionProvider for registry metacards to provide an action (url)
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2044'>DDF-2044</a> - Create registry event manager to handle publication updates
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2057'>DDF-2057</a> - Update the remote registry UI so that it can set a remote registry name in addition to the local definition
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2062'>DDF-2062</a> - Create unit test(s) for documentation module
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2108'>DDF-2108</a> - Remove dependencies that some bundles have on the Felix web console and disable the Felix console
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2125'>DDF-2125</a> - Add the ability to inject default values into metacards during ingest
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2145'>DDF-2145</a> - Fix MetacardValidityMarkerPlugin to run on update requests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2164'>DDF-2164</a> - Move /jolokia context under /admin
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2165'>DDF-2165</a> - Create registry action provider for publishing/unpublishing
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2181'>DDF-2181</a> - Create registry publication service
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2188'>DDF-2188</a> - Add service for metadata extraction from text documents
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2195'>DDF-2195</a> - Create registry rest endpoint
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2217'>DDF-2217</a> - Clean up bower dependencies
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2263'>DDF-2263</a> - Update Intellij code formatter for javax.swing and java.awt imports
	</li>
</ul>
    
<h3>Improvement</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-519'>DDF-519</a> - GeoJSON transformer needs to be decoupled from the MetacardType Registry
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2017'>DDF-2017</a> - Move registry-app under spatial
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2045'>DDF-2045</a> - Improve the way long logs are shown in the log viewer
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2050'>DDF-2050</a> - As an administrator, I would like to import/export catalog data with linked product data so that I can easily replicate catalog/content store contents between multiple DDF instances
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2076'>DDF-2076</a> - Use a RNG for the generation of SessionIndex in the SAML element AuthnStatement
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2078'>DDF-2078</a> - Audit confguration changes to the PDP
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2080'>DDF-2080</a> - Improve TestSpatial to include Csw queries with XPath expressions
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2081'>DDF-2081</a> - Record configuration changes to the system to improve security auditability
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2084'>DDF-2084</a> - Update the model CertificateSigningRequest to allow setting a full DN
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2094'>DDF-2094</a> - Add support for decrypting encrypted passwords in all sources
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2095'>DDF-2095</a> - setenv should detect if &#39;/dev/urandom&#39; is available for Linux environments
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2096'>DDF-2096</a> - showInvalidMetacards flag should be able to distinguish between &quot;validation-errors&quot; &amp; &quot;validation-warnings&quot;
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2109'>DDF-2109</a> - Update logic in IdentityPlugin and RegistryStoreImpl to use common code to do ebrim metacard id updates
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2110'>DDF-2110</a> - The XACML PDP should attempt to determine the value types for resources and subjects
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2113'>DDF-2113</a> - Move the subject role check in org.codice.ddf.security.common.Security
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2114'>DDF-2114</a> - Add modal pop up support for the admin UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2117'>DDF-2117</a> - Add failover configuration for security logs
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2118'>DDF-2118</a> - Remove orphaned project directories
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2121'>DDF-2121</a> - Add scripts to the catalog-app for windows and unix that call the `catalog:dump --include-content` command and sign the created zip file to aid administrators with import/export
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2124'>DDF-2124</a> - Update registry api interfaces from ddf-registry repo
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2126'>DDF-2126</a> - klv library needs an isError method
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2134'>DDF-2134</a> - Move the /cometd context so it is beneath the /search context
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2135'>DDF-2135</a> - Rename export script for catalog:dump command and add additional status messages in script
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2136'>DDF-2136</a> - The catalog:dump --include-content command should place the dumped metacards in a metacard folder
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2137'>DDF-2137</a> - Spatial Connected Sources should be uninstalled by default.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2140'>DDF-2140</a> - Add polling to check if subscription response handlers are available
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2143'>DDF-2143</a> - The SchematronValidationService should sanitize its messages
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2146'>DDF-2146</a> - Update Registry itests 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2147'>DDF-2147</a> - Search UI Download Notifications Aren&#39;t Delivered to the UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2151'>DDF-2151</a> - Add checks for bad filenames and mime types to content storage
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2152'>DDF-2152</a> - ActionProviders that simply present an attribute should only be available if that attribute exists
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2154'>DDF-2154</a> - write a better FileBackedOutputStream for ddf
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2158'>DDF-2158</a> - Batch audit logging related to isPermitted to improve performance
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2159'>DDF-2159</a> - Add a new SubjectUtils helper for getting subject attributes.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2168'>DDF-2168</a> - Add integration tests for resource download caching
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2175'>DDF-2175</a> - As an Integrator, I need the DDF to provide an indicator of whether or not a file has already been cached, so that I may display the visual indicator within the returned search results to allow for a quick download
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2177'>DDF-2177</a> - Add a configuration property to the registry store, &#39;Auto push identity node&#39;, which is disabled by default
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2190'>DDF-2190</a> - Address registry edit ui usability feedback
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2197'>DDF-2197</a> - Remove all maven repositories other than maven central and codice nexus
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2221'>DDF-2221</a> - Make the FeatureType a configurable property for WFS Sources
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2228'>DDF-2228</a> - validation source maps should be available to other services
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2230'>DDF-2230</a> - Document the SaxEventHandlers, SaxEventHandlerFactories, and the pluggable Input Transformer
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2244'>DDF-2244</a> - Add batching to the validation command
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2255'>DDF-2255</a> - offline-gazetteer fails to install
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2269'>DDF-2269</a> - ServiceBinding in Registry Metacard XML is missing the service attribute
	</li>
</ul>
    
<h3>Documentation</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1796'>DDF-1796</a> - Update securing section in Management document
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1915'>DDF-1915</a> - Enable indexing of documentation for visibility and searchability
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2141'>DDF-2141</a> - Update Example Query Response table and Activity Events table
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2149'>DDF-2149</a> - Improve web context policy configuration documentation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2155'>DDF-2155</a> - Document DDF registry for administrators
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2163'>DDF-2163</a> - Document installation and configuration of GeoWebCache app
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2167'>DDF-2167</a> - Remove DDfConfigurationWatcher and DdfConfigurationManager from Documentation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2174'>DDF-2174</a> - Create maven properties for default host url prefixes
	</li>
</ul>
    
<h3>Technical Debt</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1937'>DDF-1937</a> - Update PreIngest Registry Creation Process to add External Identifier
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2037'>DDF-2037</a> - MetacardValidityChecker may prevent non-catalog queries from returning results
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2064'>DDF-2064</a> - Investigate and fix DDF itest Windows CI failures
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2092'>DDF-2092</a> - DDF fails to build on windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2100'>DDF-2100</a> - Add registry-common unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2101'>DDF-2101</a> - Update registry-federation-admin-impl unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2102'>DDF-2102</a> - Update registry-federation-admin-service-impl unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2104'>DDF-2104</a> - Update registry-policy-plugin unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2105'>DDF-2105</a> - Create registry-report-viewer unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2106'>DDF-2106</a> - Update registry-schema-bindings unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2107'>DDF-2107</a> - Create registry-source-configuration-handler unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2153'>DDF-2153</a> - Disable NPM install scripts and address any consequent problems
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2172'>DDF-2172</a> - Use https to download packages from Maven Central and other Maven repositories
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2173'>DDF-2173</a> - Fix OWASP Failures
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2176'>DDF-2176</a> - Break up FederationAdminServiceImpl to reduce the class&#39; complexity and better adhere to the Single Responsibility Principle
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2182'>DDF-2182</a> - waitForReady startup script broken. 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2185'>DDF-2185</a> - Create new cache interface and deprecate ResourceCacheInterface
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2202'>DDF-2202</a> - Remove dependency between CatalogFrameworkImpl and ResourceCache
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2253'>DDF-2253</a> - Security common class should not hardcode an admin role
	</li>
</ul>
    
<h3>Dependency Upgrade</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1945'>DDF-1945</a> - Update org.apache.karaf.bundle dependency in platform/admin/admin-app/pom.xml
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2079'>DDF-2079</a> - Upgrade to Karaf 4.0.5
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2088'>DDF-2088</a> - Update DDF to use Arbitro instead of Balana
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2127'>DDF-2127</a> - Update Apache Tika to version 1.13
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2201'>DDF-2201</a> - Upgrade npm to strengthen ddf security
	</li>
</ul>


## ddf-2.9.0
	Release Date: 2016-04-29

<h3>Bug</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-106'>DDF-106</a> - Selecting &quot;org.ops4j.pax.logging&quot; configuration causes Ajax error
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-735'>DDF-735</a> - DDF Spatial application should show a dependency on DDF Catalog in install wizard
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1247'>DDF-1247</a> - Catalog Queries Total Results count continues to be reported until new Query is executed
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1423'>DDF-1423</a> - Zoom and Center not working
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1489'>DDF-1489</a> - OpenSearch federated spatial queries fail
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1516'>DDF-1516</a> - USNG library used in the Search UI has conversion issues in some regions
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1581'>DDF-1581</a> - Activities are not published on the correct CometD channels and they are not handled correctly in the Search UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1583'>DDF-1583</a> - Correct DDF documentation errors about installing new certs
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1598'>DDF-1598</a> - RrdJmxCollectorTest fails intermittently
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1613'>DDF-1613</a> - User cannot logout from Search UI using LDAP realm
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1616'>DDF-1616</a> - SAMLAssertionHandler needs to check session when using basic authentication
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1619'>DDF-1619</a> - PKI handler exits if there is no http response available
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1625'>DDF-1625</a> - Green target and info window pop up when clicking on the 3D map 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1629'>DDF-1629</a> - DDF cannot be built with Java 8 features
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1633'>DDF-1633</a> - Catalog Sources Tab duplicates source types
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1634'>DDF-1634</a> - SSO Metadata inaccesible
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1640'>DDF-1640</a> - Incorrect version of Woodstox dependencies being pulled in
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1645'>DDF-1645</a> - jpeg2000-thumbnail-converter fails if result list contains an empty thumbnail
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1653'>DDF-1653</a> - Erroneous instructions in pom files
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1654'>DDF-1654</a> - Search endpoint should report if a source times out or has an un-handled exception
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1660'>DDF-1660</a> - Admin console should save out falsey values as false
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1661'>DDF-1661</a> - Unable to send commands via the client script
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1666'>DDF-1666</a> - Search UI fails to allow the user to add filters on specific GeoTypes
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1676'>DDF-1676</a> - Fix outstanding coverity issues as of 11/24/2015
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1693'>DDF-1693</a> - Grunt &quot;Newer&quot; plugin doesn&#39;t work with our codebase
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1695'>DDF-1695</a> - The CertNew.sh script to generate certificates for development use, no longer creates certificates correctly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1706'>DDF-1706</a> - Drawing BoundingBoxes in the Advanced section of the Standard Search UI causes errors 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1712'>DDF-1712</a> - Modal Preferences Color Picker is not modal itself
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1718'>DDF-1718</a> - Update DDF default certs to fix expiration
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1730'>DDF-1730</a> - Release can fail due to install-profiles.app.version not resolving and self-dependencies
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1731'>DDF-1731</a> - Update the Platform Configuration Listener so it runs on Windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1737'>DDF-1737</a> - REST endpoints do not validate a HoK assertion correctly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1738'>DDF-1738</a> - RelayState should not be required in our SSO implementation for POST, Redirect, or Artifact binding.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1740'>DDF-1740</a> - SAML Logout services should validate InResponseTo field
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1741'>DDF-1741</a> - Saving configuration in Admin UI breaks window scrolling
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1742'>DDF-1742</a> - IdP and SP should reject SAML messages not sent over encrypted channel
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1752'>DDF-1752</a> - GeoCoderEndpoint.getNearbyCities() can throw an Exception in certain situations
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1756'>DDF-1756</a> - SOAP proxy factory should check the assertion that it has against the policy of the remote server before sending
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1769'>DDF-1769</a> - Map feature color preferences display incorrectly in IE11
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1776'>DDF-1776</a> - SearchUI CSS causes titles in the result list to be centered
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1780'>DDF-1780</a> - Preference modal map layer picker does not reset UI layer picker order when user resets defaults.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1781'>DDF-1781</a> - Preference modal map can disappear when user toggles between map layer tab and map feature tab.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1785'>DDF-1785</a> - SAML SOAP builders in AttributeQueryClient are not initialized causing NullPointerException
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1790'>DDF-1790</a> - Fix usages of SystemBaseUrl library class
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1800'>DDF-1800</a> - Metacards have extraneous attributes (Solr lux and score fields)
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1808'>DDF-1808</a> - TestSolrServerFactory tests fail due to an invalid Solr URL
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1830'>DDF-1830</a> - TestSolrServerFactory tests fail when a DDF with HTTP enabled is running on the same machine
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1832'>DDF-1832</a> - Intermittent failures on Integration Test, specifically TestConfig
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1833'>DDF-1833</a> - SolrServerFactory does not always follow the Future pattern correctly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1834'>DDF-1834</a> - Missing dependencies break parallel and make-dependents builds
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1835'>DDF-1835</a> - Fanout property being ignored when retrieving the list of available sources
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1837'>DDF-1837</a> - Wrong error message displayed in Admin UI when export fails
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1841'>DDF-1841</a> - Unable to add new source types to existing source in Admin UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1846'>DDF-1846</a> - AuthzRealm can cause a memory leak if authorization caching is enabled
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1847'>DDF-1847</a> - GeoJson Input Transformer fails nondeterministically because of threading
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1851'>DDF-1851</a> - CatalogBackupPlugin should not be copying each metacard twice
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1855'>DDF-1855</a> - Cannot perform an xpath csw query with namespaces
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1865'>DDF-1865</a> - Resolve DDF Critical Fortify Findings
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1871'>DDF-1871</a> - Export fails when optional CRL property is not in encryption.properties
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1875'>DDF-1875</a> - Fix Jacoco floating point comparison issue with platform-scheduler line coverage
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1889'>DDF-1889</a> - Missing navigation buttons on &quot;Select Applications&quot; page during install on Windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1901'>DDF-1901</a> - Spatial webservice-gazetteer uninstall via admin webpage fails
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1902'>DDF-1902</a> - With the Solr catalog provider, spatial sorting does not work for nested logical queries
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1906'>DDF-1906</a> - pom-fix fails on windows due to unix paths
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1913'>DDF-1913</a> - Metacard Attribute Policy Plugin fails to extract attributes when ingesting via the Content Framework
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1914'>DDF-1914</a> - migration:export command doesn&#39;t have a Subject when run
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1922'>DDF-1922</a> - If the offline-gazetteer is enabled but not populated with data, a stacktrace is logged when attempting to query the service
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1930'>DDF-1930</a> - Logo does not appear in landing page
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1936'>DDF-1936</a> - Content framework should not be using the system subject for all requests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1941'>DDF-1941</a> - Remove LDAP from default installed features
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1954'>DDF-1954</a> - CSW getDescribeRecord fails
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1956'>DDF-1956</a> - Content app fails at startup due to missing requirement
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1959'>DDF-1959</a> - Fix delete/update operations for metacards with tags other than &#39;resource&#39;
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1971'>DDF-1971</a> - As an administrator I would like cleaner log information about &quot;unavailable&quot; federations so that I can more easily diagnose failures and coordinate solutions in federations
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1976'>DDF-1976</a> - As an integrator, I want DDF to produce GMD metadata so that I can expose DDF metadata per the CSW ISO profile
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1977'>DDF-1977</a> - Search UI queries generate errors in the logs
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1985'>DDF-1985</a> - Add catalog-app prerequisite to resourcemanagement-app
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1990'>DDF-1990</a> - Fix XmlMetacardTransformer multivalued serialization.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1993'>DDF-1993</a> - Unable to execute actions when the source id has a space in the name.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1995'>DDF-1995</a> - Remove references to content-app in install profiles
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1997'>DDF-1997</a> - Fix release related issues
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2007'>DDF-2007</a> - DDF build output is in ASCII instead of UTF-8
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2012'>DDF-2012</a> - Catalog remove command will not process a remove with just CQL
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2018'>DDF-2018</a> - Content cache doesn&#39;t return size
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2019'>DDF-2019</a> - CswEndpoint incorrectly adds metacard tag to standard queries
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2029'>DDF-2029</a> - Improperly encoded Action URLs in JSON prevent results display
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2030'>DDF-2030</a> - CatalogFrameworkImpl does not properly update the request in the create method
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2073'>DDF-2073</a> - Metacard Versioning Plugin fails to start due to incorrect blueprint interface specification
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2077'>DDF-2077</a> - Restarting the system via the Admin Console installer after changing the hostname will put the system into a bad state
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2085'>DDF-2085</a> - Multiple bundles export the org.codice.ddf.security.interceptor package
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2179'>DDF-2179</a> - Search UI admin config settings do not save properly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2316'>DDF-2316</a> - Tests and OWASP fail on Windows build
	</li>
</ul>

<h3>Story</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1610'>DDF-1610</a> - As an integrator I want a Hello World example of subscribing to events so that a basic example can be viewed for integration
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1700'>DDF-1700</a> - As an Integrator I want to be able to receive asynchronous download notifications between a DDF and another backend service
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1715'>DDF-1715</a> - As an administrator, I want all necessary properties exported when using the CLI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1787'>DDF-1787</a> - As an administrator, I want the config-export command to exclude files that are symlinked
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1807'>DDF-1807</a> - Create the Migratable API module
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1816'>DDF-1816</a> - Update ConfigurationMigrationManager to call Migratable services
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1822'>DDF-1822</a> - As a developer, I want a Platform Migratable that will export all Platform configuration and system files
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1827'>DDF-1827</a> - Create Migratable Utility module
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1838'>DDF-1838</a> - As a developer, I want the Platform Migratable to export org.codice.ddf.admin.applicationlist.properties
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1840'>DDF-1840</a> - As an administrator, I would like Banana to be included with DDF, so that I can generate advanced metrics on my catalog
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1868'>DDF-1868</a> - As an administrator, I want to be able to toggle invalid metacards to be included in search results.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1952'>DDF-1952</a> - As an integrator, I want separate Migratable interfaces for configuration and data
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1957'>DDF-1957</a> - Create backend for Logging Service
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1970'>DDF-1970</a> - As an Integrator, I want a complete set of info on migratables
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1999'>DDF-1999</a> - GmdMetacardTransformer uses incorrect resource URI 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2002'>DDF-2002</a> - I would like the Content Framework to be able to Store related resources
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2023'>DDF-2023</a> - As an administrator, I want duplicate data identified at ingest so that I can limit duplicate data in the catalog
	</li>
</ul>
    
<h3>New Feature</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1508'>DDF-1508</a> - Default map data should be available for offline DDF deployments
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1599'>DDF-1599</a> - Implement SAML 2 Web Profile single logout service
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1745'>DDF-1745</a> - As an administrator, I want to securely grant or deny access to invalid metacards
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1770'>DDF-1770</a> - Expose ConfigurationMigrationService as an MBean/Jolokia service
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1799'>DDF-1799</a> - Create API for security plugins in the Catalog
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1803'>DDF-1803</a> - Merge the Java PDP and the XACML PDP together so that we can get the benefits of both and remove confusion
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1825'>DDF-1825</a> - Add a way to export configurations from the Admin UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1831'>DDF-1831</a> - Implement RegistryCatalogStore
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1844'>DDF-1844</a> - Add XPath support for additional Operators that have a PropertyName element
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1845'>DDF-1845</a> - Implement True Product Retrieval for CSW
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1858'>DDF-1858</a> - Create webhooks pubsub rest api using CSW asynchronous API
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1872'>DDF-1872</a> - WFS Sources do not present their version correctly
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1874'>DDF-1874</a> - Thumbnail should be generated for ingested PDF documents
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1896'>DDF-1896</a> - Create the UI for editing registry information
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1897'>DDF-1897</a> - Metacard Versioning
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1907'>DDF-1907</a> - Add ability to validate xml from the karaf console
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1923'>DDF-1923</a> - As an administrator, I want to visualize all active searches in the Admin UI so that I can monitor system activity
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1924'>DDF-1924</a> - As an administrator, I want to view data usage per user so that I can track data usage
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1950'>DDF-1950</a> - Refactor CswSource Part 1 - Create AbstractCswSource from current CswSource
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1953'>DDF-1953</a> - Create endpoint to consume events from a CSW subscription
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1961'>DDF-1961</a> - As an integrator, I want the DDF CSW Endpoint to support the GMD metadata format in query requests so that I can integrate using the CSW ISO profile 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1979'>DDF-1979</a> - Add Resource Management and GeoWebCache apps to Development install profile
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1984'>DDF-1984</a> - Allow the ability to deny resource URIs that are not from a secure connection
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1992'>DDF-1992</a> - Update AdminSourcePollerBean to return registry info
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2032'>DDF-2032</a> - Add a Twitter federated source
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2065'>DDF-2065</a> - Add Related and Derived Associations to a Metacard
	</li>
</ul>
    
<h3>Task</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1547'>DDF-1547</a> - Refactor SecurityCxfClientFactory to allow configuring AutoRedirects
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1567'>DDF-1567</a> - Implement CheckSumProvider Capability Into Catalog Framework
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1596'>DDF-1596</a> - Implement a way for guests (anonymous users) to kill their session
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1605'>DDF-1605</a> - Implement SAML 2 Web Profile single logout service
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1617'>DDF-1617</a> - The Anonymous User should be re-named to Guest
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1656'>DDF-1656</a> - Refactor Logout Endpoint and implement logout page
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1707'>DDF-1707</a> - Enable Findbugs by default
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1716'>DDF-1716</a> - PKI validator should provide a config option to validate only the subject level cert instead of always the entire path
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1727'>DDF-1727</a> - Revise Solr Documentation for audience/purpose
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1733'>DDF-1733</a> - Implement way to expose endpoints as they become available	
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1736'>DDF-1736</a> - Remove unused resource files in platform-configuration-listener
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1743'>DDF-1743</a> - Implement generic catalog registry with csw endpoint
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1749'>DDF-1749</a> - Implement pre/post query registry plugins
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1763'>DDF-1763</a> - Implement federation post query plugin
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1766'>DDF-1766</a> - Revise DDF Security Documentation for audience/purpose
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1771'>DDF-1771</a> - Implement FederationManagementMBean for storing/editing federation configurations
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1774'>DDF-1774</a> - Implement RegistryProvider
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1778'>DDF-1778</a> - Remove *;resolution:=optional imports in pom files
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1815'>DDF-1815</a> - Implement initial GeoWebCache application 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1848'>DDF-1848</a> - Integrate maven dependency checks into build process
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1849'>DDF-1849</a> - Move *.config files from ddf-common into features files
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1852'>DDF-1852</a> - Implement a PreResourcePlugin to keep track of data usage per user
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1854'>DDF-1854</a> - Allow/restrict access to sources/metacards based on high level groups
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1863'>DDF-1863</a> - As an integrator, I want DDF to consume GMD metadata so that I can discover data from GMD compliant services
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1893'>DDF-1893</a> - Update pom-fix to ignore unknown flags.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1921'>DDF-1921</a> - Update DDF to use ddf support version 2.3.6
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1933'>DDF-1933</a> - Revert change to metacard groomer so that a new id is generated every time
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1975'>DDF-1975</a> - Fix missing npm dependency.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1987'>DDF-1987</a> - Add property to update/delete requests that contains the original metacards before the operation is performed.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1988'>DDF-1988</a> - Remove incorrect dependencies from DDF catalog
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1989'>DDF-1989</a> - Resolve bamboo CSW transformer test failure
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1996'>DDF-1996</a> - Create integration tests to demonstrate CSW federation with all query types
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2010'>DDF-2010</a> - IDP Endpoint should not hardcode the location of the cookie to /services/idp as that /services root can change
	</li>
</ul>
    
<h3>Improvement</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-899'>DDF-899</a> - There is no AttributeFormat for URI however Metacard has a getResourceURI method which makes it unclear how this should work with the Metacard.setAttribute method
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1205'>DDF-1205</a> - Add more policy support to Anonymous Interceptor 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1271'>DDF-1271</a> - Add a banner to confluence wiki to point users to new documentation space
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1272'>DDF-1272</a> - Create &quot;Release Pages&quot; for documentation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1441'>DDF-1441</a> - Update the Search UI to allow for batch file uploads
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1525'>DDF-1525</a> - Update global configuration installer page
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1559'>DDF-1559</a> - Remove cardinality workarounds in platform security
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1588'>DDF-1588</a> - Support Code 3xx HTTP Redirects in URLResourceReader
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1635'>DDF-1635</a> - Re-add CacheException class for use in the catalog-standardframework project
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1650'>DDF-1650</a> - Improve usability of the date/time picker in the Search UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1667'>DDF-1667</a> - Update the DDF github documentation with new features
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1685'>DDF-1685</a> - Solr catalog provider should only set date fields if they are null in the request
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1690'>DDF-1690</a> - Identity provider validates SSO time bounds
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1702'>DDF-1702</a> - The Admin UI Source Editor fields have inconsistent formatting
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1726'>DDF-1726</a> - IdP should support the ForceAuthn attribute on Authn requests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1728'>DDF-1728</a> - Improve metadata generated by TikaInputTransformer
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1734'>DDF-1734</a> - I want to be able to ingest any XML schema that has an InputTransformer
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1746'>DDF-1746</a> - CI tests should profile performance and warn of regressions
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1748'>DDF-1748</a> - Combine all Configuration related integration tests into a single class
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1753'>DDF-1753</a> - Update checksum provider implementation for performance and efficiency 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1758'>DDF-1758</a> - Improve error message when getHostName fails
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1764'>DDF-1764</a> - Create a new input transformer to handle video files
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1768'>DDF-1768</a> - Support Multivalued attributes for all basic field types in Solr
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1779'>DDF-1779</a> - Use consistent Base64 encoding when SAML
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1784'>DDF-1784</a> - As and administrator, I would like better documentation on catalog commands so that I know how to use them
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1791'>DDF-1791</a> - Change DDF-Support version to 2.3.5 in DDF
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1792'>DDF-1792</a> - Implement performant XML input transformation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1809'>DDF-1809</a> - AttributeQueryClient should use CXF for sending out its request
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1814'>DDF-1814</a> - Move shared classes out of security-core-impl into ddf-security-common
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1819'>DDF-1819</a> - SchematronValidationService should be a managed service factory
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1821'>DDF-1821</a> - Delegate DDF security export/migration to the DDF Security migratable
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1823'>DDF-1823</a> - Generate thumbnails for ingested video files
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1836'>DDF-1836</a> - Expand the functionality of the IngestPlugin to allow it to restrict data from being ingested
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1850'>DDF-1850</a> - Update the ingest command to support threading + batch sizing together
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1853'>DDF-1853</a> - Reformat DDF code base using new IntelliJ settings
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1857'>DDF-1857</a> - Upgrade GeoJsonInputTransformer to use Boon
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1859'>DDF-1859</a> - Rewrite the IngestCommand so that it follows a producer-consumer model
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1862'>DDF-1862</a> - The CatalogBackupPlugin should not be backing up metacards on the main ingest thread
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1870'>DDF-1870</a> - Update system user to have attributes and turn on FilterPlugin by default
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1873'>DDF-1873</a> - Change SchematronValidationService to use paths relative to root schematron file
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1877'>DDF-1877</a> - Show Human Readable Names for Managed Service Factory Configurations in the Admin UI
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1879'>DDF-1879</a> - Update CSW Source to use true Product Retrieval
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1884'>DDF-1884</a> - Integration Tests Fail on Windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1894'>DDF-1894</a> - Update registry transformer to handle new content collections concept
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1904'>DDF-1904</a> - Provide the ability to extract KLV from MPEG-2 transport streams that adhere to the STANAG 4609 standard
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1905'>DDF-1905</a> - Add PolicyPlugin implementations for common metacard types
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1911'>DDF-1911</a> - The PDF input transformer should include document metadata in the metacard
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1928'>DDF-1928</a> - As a developer, I want reusable code to implement secure commands
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1947'>DDF-1947</a> - As an Administrator, I want to be able to view the current Karaf Log from within the Admin Console
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1948'>DDF-1948</a> - Update Csw-ebrim example to include ExtrinsicObject and ServiceLink
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1955'>DDF-1955</a> - Refactor AdminSourcePollerServiceBean to its own module 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1958'>DDF-1958</a> - Develop reusable Geo Libraries
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1966'>DDF-1966</a> - Add support to CswSource to query by additional Type Names 
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1972'>DDF-1972</a> - mvn clean should also clean npm directories
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1978'>DDF-1978</a> - Create and implement a new metacard validation API
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1994'>DDF-1994</a> - Run Access Plugin post query processing on events
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2004'>DDF-2004</a> - GeoWebCache bundle logs to standard out
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2014'>DDF-2014</a> - Update Registry PreIngestPlugin to make sure we preserve local transient attributes
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2021'>DDF-2021</a> - As a system administrator, I need an audit log of incoming and outgoing requests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2025'>DDF-2025</a> - Resource Action Provider should support building URL&#39;s for metacard&#39;s with mulitple contentItems
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2027'>DDF-2027</a> - Improvements to the Admin UI LogViewer
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2028'>DDF-2028</a> - Update the SecurityLogger with better information and capabilities
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2031'>DDF-2031</a> - Add failed login delay to prevent malicious bot attacks.
	</li>
</ul>
    
<h3>Documentation</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-633'>DDF-633</a> - Update wiki to include a Downloads section where links to latest and previous DDF releases can be found
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1810'>DDF-1810</a> - Correct DDF github build documentation link
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1817'>DDF-1817</a> - Create introductory section in Omnibus documentation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1818'>DDF-1818</a> - Remove excessive asciidoctor warnings from builds
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1861'>DDF-1861</a> - Convert Asciidoctor variables to maven properties
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1866'>DDF-1866</a> - Update developer docs re: OSGi services &amp; bundles
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1876'>DDF-1876</a> - Investigate a single docs module
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1916'>DDF-1916</a> - Publish user guide with documentation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1917'>DDF-1917</a> - Consolidate Documentation to single pages tailored to audience
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1918'>DDF-1918</a> - Archive older versions of documentation.
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1920'>DDF-1920</a> - Update documented Karaf 2 commands with renamed Karaf 3/4 commands
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1938'>DDF-1938</a> - Restructure published documentation
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1962'>DDF-1962</a> - Add documentation for Asynchronous search and product retrieval
	</li>
</ul>
    
<h3>Technical Debt</h3>
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1305'>DDF-1305</a> - Address all high severity CLM vulnerabilities
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1329'>DDF-1329</a> - Remove unused DDF Maven profiles
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1408'>DDF-1408</a> - Update admin-ui tests to use current UI testing framework
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1450'>DDF-1450</a> - Bump the test dependency versions in parent poms for added functionality
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1584'>DDF-1584</a> - Refactor and clean up the IdP
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1586'>DDF-1586</a> - Create a HTTP session creation OSGi service
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1607'>DDF-1607</a> - Fix Search UI build failures and remove --force
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1627'>DDF-1627</a> - Create Unit tests for IdP Client/Service Provider (SP)
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1647'>DDF-1647</a> - Integration tests for IdP Server - Non-Standards
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1722'>DDF-1722</a> - Parallelize maven builds
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1729'>DDF-1729</a> - Rename platform-configuration-listener module and rework packages
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1732'>DDF-1732</a> - SAML Logout Move LogoutService Builder methods into SamlProtocol
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1735'>DDF-1735</a> - Move SimpleSignTest from security-idp-client to security-core-api
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1739'>DDF-1739</a> - SAML Logout code verbiage cleanup
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1754'>DDF-1754</a> - Add unit tests for SAML logout client
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1759'>DDF-1759</a> - Rename Security.authenticateCurrentUser to Security.authorizeCurrentUser
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1762'>DDF-1762</a> - SAML Logout integration tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1767'>DDF-1767</a> - Update the CRL documentation so that it covers all of the properties files that need to be changed
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1775'>DDF-1775</a> - DDF documentation is very out of date and needs to be updated
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1783'>DDF-1783</a> - Remove impl classes in catalog-core-api, point references to catalog-core-api-impl instead
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1826'>DDF-1826</a> - Update IngestPlugin to implement AccessPlugin
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1842'>DDF-1842</a> - Add getRootNamespace method to XMLUtils
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1843'>DDF-1843</a> - Certificate generation duplicates key chain file access routines
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1912'>DDF-1912</a> - DDF Content Cataloger Plugin needs unit tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1919'>DDF-1919</a> - Fix DDF build issues on Windows
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1932'>DDF-1932</a> - Resume use of Codice NPM and Node.js as proxy during builds
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1939'>DDF-1939</a> - Create common describable interface
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1942'>DDF-1942</a> - Address catalog export code testing issues
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1943'>DDF-1943</a> - Create non-static Security class in org.codide.ddf.common.util
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1951'>DDF-1951</a> - CSW Endpoint should support downloading products with a byte offset
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1960'>DDF-1960</a> - DDF Catalog and DDF Content should be merged together
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1982'>DDF-1982</a> - Remove content-app from install profile
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2003'>DDF-2003</a> - The user and metacard expansion service bundles should be moved under expansion
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2026'>DDF-2026</a> - GMD CSW ISO source &amp; endpoint need integration tests
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2585'>DDF-2585</a> - Add locale to system properties
	</li>
</ul>
    
<h3>Dependency Upgrade</h3> 
<ul>
	<li><a href='https://codice.atlassian.net/browse/DDF-1135'>DDF-1135</a> - Up Spring Core and Shiro Core versions to 4.1.2.RELEASE and 1.2.3 respectively
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1216'>DDF-1216</a> - Update OpenSaml to address security vulnerability
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1648'>DDF-1648</a> - Upgrade to Karaf 4.0.x
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1709'>DDF-1709</a> - Update Balana to 1.0.2
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1747'>DDF-1747</a> - Upgrade to CXF 3.1.x
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1751'>DDF-1751</a> - Update Camel to 2.16.1
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-1883'>DDF-1883</a> - Upgrade to Solr 5.x
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2046'>DDF-2046</a> - Upgrade to Solr 6
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2066'>DDF-2066</a> - Upgrade opendj-osgi to 1.3.2
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2068'>DDF-2068</a> - Update imageio jpeg2000 library to the latest version from maven central
	</li>
	<li><a href='https://codice.atlassian.net/browse/DDF-2069'>DDF-2069</a> - Replace jquery-cookie with js-cookie
	</li>
</ul>


