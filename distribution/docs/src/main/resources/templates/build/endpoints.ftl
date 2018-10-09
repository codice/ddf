
== Endpoints

<#list endpointIntros as ei>
<#if (ei.title == "Endpoints Intro")>
include::${ei.file}[]

</#if>
</#list>

=== Available Endpoints

The following endpoints are available in a standard installation of ${branding}:

<#assign count=0>
<#list endpoints as endpoint>
<#if (endpoint.status == "published")>
<#assign count++>
<<${endpoint.link},${endpoint.title}>>:: ${endpoint.summary}

</#if>
</#list>
<#if (count == 0)>
None.
</#if>

<#list endpoints as endpoint>
<#if (endpoint.status == "published")>

==== ${endpoint.title}

include::${endpoint.file}[]

</#if>
</#list>

=== Endpoint Utility Services

<#list endpointIntros as ei>
<#if (ei.title == "Endpoint Utilities Intro")>
include::${ei.file}[]
</#if>

</#list>
<#list endpointServices as endpointService>
<#if (endpointService.status == "published")>

==== ${endpointService.title}

include::${endpointService.file}[]

</#if>
</#list>