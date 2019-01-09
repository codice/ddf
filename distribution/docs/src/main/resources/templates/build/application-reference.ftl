
== Application Reference

====
Installation and configuration details by application.
====

<#list applicationReferences?sort_by("order") as applicationReference>

<#if applicationReference.status == "published">

include::${applicationReference.file}[leveloffset=+1]

<#assign configurations = 0>
<#list tables as table>
<#if table.application == applicationReference.title>
<#assign configurations += 1>
</#if>
</#list>

<#if configurations gt 0>

==== Configuring the ${applicationReference.title} Application

To configure the ${applicationReference.title} Application:

. Navigate to the ${admin-console}.
. Select the *${applicationReference.title}* application.
. Select the *Configuration* tab.

.${applicationReference.title} Available Configurations
[cols="3" options="header"]
|===
|Name
|Property
|Description

</#if>
<#list tables as table>
<#if table.status == "published" && table.application == applicationReference.title>
|<<{reference-prefix}${table.id},${table.title}>>
|${table.id}
|${table.summary}

</#if>
</#list>
<#if configurations gt 0>
|===
</#if>
<#list tables as table>
<#if table.status == "published" && table.application == applicationReference.title>
include::${table.file}[]
</#if>
</#list>
</#if>
</#list>
