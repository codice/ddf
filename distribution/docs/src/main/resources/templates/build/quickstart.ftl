<#list quickStarts?sort_by("order") as quickStart>
<#if (quickStart.status == "published")>
include::${quickStart.file}[]

</#if>
</#list>