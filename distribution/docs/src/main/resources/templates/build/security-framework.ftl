
<#list securityFrameworkIntros as sfi>
<#if (sfi.status == "published")>

include::${sfi.file}[]

<#list securityFrameworks?sort_by("order") as securityFramework>

<#if (securityFramework.status == "published") && (securityFramework.parent == sfi.title)>

include::${securityFramework.file}[leveloffset=+1]

<#list subSecurityFrameworks?sort_by("order") as subSecurityFramework>
<#if (subSecurityFramework.status == "published") && (subSecurityFramework.parent == securityFramework.title)>

include::${subSecurityFramework.file}[leveloffset=+2]

</#if>
</#list>
</#if>
</#list>
</#if>
</#list>