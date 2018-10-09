<#list integratingIntros as ii>
<#if (ii.status == "published")>

include::${ii.file}[]
</#if>
</#list>

<#include "data-validation.ftl">

<#include "endpoints.ftl">

<#include "eventing.ftl">

include::${project.build.directory}/doc-contents/content/scripts.html[]

<#include "security-services.ftl">