
.Available Federated Sources
The following Federated Sources are available in a standard installation of ${branding}:

<#assign count=0>
<#list sources as source>
<#if (source.federated == "x")>
<#assign count++>
<<${source.link},${source.title}>>:: ${source.summary}

</#if>
</#list>
<#if (count == 0)>
None.
</#if>

.Available Connected Sources
The following Connected Sources are available in a standard installation of ${branding}:

<#assign count=0>
<#list sources as source>
<#if (source.connected == "x")>
<#assign count++>
<<${source.link},${source.title}>>:: ${source.summary}

</#if>
</#list>
<#if (count == 0)>
None.
</#if>

.Available Catalog Stores
The following Catalog Stores are available in a standard installation of ${branding}:

<#assign count=0>
<#list sources as source>
<#if (source.catalogstore == "x")>
<#assign count++>
<<${source.link},${source.title}>>:: ${source.summary}

</#if>
</#list>
<#if (count == 0)>
None.
</#if>

.Available Catalog Providers
The following Catalog Providers are available in a standard installation of ${branding}:

<#assign count=0>
<#list sources as source>
<#if (source.catalogprovider == "x")>
<#assign count++>
<<${source.link},${source.title}>>:: ${source.summary}

</#if>
</#list>
<#if (count == 0)>
None.
</#if>

.Available Storage Providers
The following Storage Providers are available in a standard installation of ${branding}:

<#assign count=0>
<#list sources as source>
<#if (source.storageprovider == "x")>
<#assign count++>
<<${source.link},${source.title}>>:: ${source.summary}
</#if>
</#list>
<#if (count == 0)>
None.
</#if>

.Sources Details
Availability and configuration details of available sources.

<#list sources as source>
<#if (source.status == "published")>

include::${source.file}[leveloffset=+3]

'''
</#if>
</#list>
