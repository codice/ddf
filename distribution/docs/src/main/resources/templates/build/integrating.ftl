<#list integratingIntros as ii>
<#if (ii.status == "published")>

include::${ii.file}[]
</#if>
</#list>

<#include "endpoints.ftl">
