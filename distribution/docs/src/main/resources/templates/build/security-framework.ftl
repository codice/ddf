
<#list securityFrameworkIntros as sfi>
<#if (sfi.status == "published")>

== ${sfi.title}

include::${sfi.file}[]

<#list securityFrameworks?sort_by("order") as securityFramework>

<#if (securityFramework.status == "published") && (sfi.children?contains (securityFramework.parent))>
=== ${securityFramework.title}

include::${securityFramework.file}[]

<#list subSecurityFrameworks?sort_by("order") as subSecurityFramework>
<#if (subSecurityFramework.status == "published") && (securityFramework.title?contains (subSecurityFramework.parent))>
==== ${subSecurityFramework.title}

include::${subSecurityFramework.file}[]

</#if>
</#list>
</#if>
</#list>
</#if>
</#list>