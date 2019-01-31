<#list configuringIntros?sort_by("order") as ci>
<#if (ci.status == "published")>

include::${ci.file}[leveloffset=+1]

<#list configurations?sort_by("order") as configuration>
<#if (configuration.parent == ci.title)>

include::${configuration.file}[leveloffset=+2]

<#if (configuration.title == "Connecting to Sources")>

<#include "sources.ftl">
</#if>
<#list subConfigurations?sort_by("order") as subConfiguration>
<#if (subConfiguration.parent == configuration.title)>

include::${subConfiguration.file}[leveloffset=+3]

</#if>
</#list>
</#if>
</#list>
</#if>
</#list>
