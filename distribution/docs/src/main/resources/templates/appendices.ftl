<#list appendixIntros?sort_by("order") as ai>
<#if (ai.status == "published")>

[appendix]
include::${ai.file}[]
<#list appendixs?sort_by("order") as appendix>
<#if (appendix.parent == ai.title && appendix.status == "published")>

include::${appendix.file}[leveloffset=+1]
<#list subAppendixs?sort_by("order") as subAppendix>
<#if (subAppendix.parent == appendix.title && subAppendix.status == "published")>

include::${subAppendix.file}[leveloffset=+2]

</#if>
</#list>
</#if>
</#list>
</#if>
</#list>
