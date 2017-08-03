<#list developingIntros?sort_by("order") as di>
<#if (di.status == "published")>
include::${di.file}[]

</#if>
</#list>

== Catalog Framework API
<#list catalogFrameworkIntros?sort_by("order") as cfi>
<#if (cfi.status == "published")>
include::${cfi.file}[]

</#if>
</#list>

=== Included Catalog Frameworks, Associated Components, and Configurations

These catalog frameworks are available in a standard ${branding} installation:

<#assign count=0>
<#list catalogFrameworks?sort_by("order") as catalogFramework>
<#if (catalogFramework.status == "published")>
<#assign count++>
<<${catalogFramework.link},${catalogFramework.title}>>:: ${catalogFramework.summary}

</#if>
</#list>
<#if (count == 0)>
None.
</#if>

<#list catalogFrameworks?sort_by("order") as catalogFramework>
<#if (catalogFramework.status == "published")>

==== ${catalogFramework.title}

include::${catalogFramework.file}[]

</#if>
</#list>
