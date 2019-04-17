<#list integratingIntros as ii>
<#if (ii.status == "published")>

include::${ii.file}[]
</#if>
</#list>

<#include "data-validation.ftl">

<#include "endpoints.ftl">

<#include "eventing.ftl">

<#include "security-services.ftl">