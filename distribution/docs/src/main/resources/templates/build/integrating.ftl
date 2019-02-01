<#list integratingIntros as ii>
<#if (ii.status == "published")>

include::${ii.file}[]
</#if>
</#list>

<#include "endpoints.ftl">

include::${project.build.directory}/doc-contents/content/scripts.html[]
