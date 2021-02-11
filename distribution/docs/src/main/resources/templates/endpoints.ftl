
== Endpoints
<#list endpointIntros?sort_by("order") as ei>
<#if (ei.status == "published")>

include::${ei.file}[leveloffset=+1]

<#list endpoints as endpoint>
<#if (endpoint.status == "published" && endpoint.operations?contains(ei.operations))>
<<${endpoint.link},${endpoint.title}>>:: ${endpoint.summary}
</#if>
</#list>
</#if>
</#list>

=== Endpoint Details

<#list endpoints as endpoint>
<#if (endpoint.status == "published")>

include::${endpoint.file}[leveloffset=+2]

</#if>
</#list>