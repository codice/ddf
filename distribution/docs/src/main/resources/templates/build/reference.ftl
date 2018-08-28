<#list referenceIntros?sort_by("order") as ri>
<#if ri.status == "published">
include::${ri.file}[]

<#list references?sort_by("order") as reference>
<#if reference.parent == ri.title>

include::${reference.file}[leveloffset=+1]
</#if>
</#list>
</#if>
</#list>

