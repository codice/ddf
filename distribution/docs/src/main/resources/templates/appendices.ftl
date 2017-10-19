<#list appendixIntros?sort_by("order") as ai>
<#if (ai.status == "published")>

[appendix]
== ${ai.title}

include::${ai.file}[]
<#list appendixs?sort_by("order") as appendix>
<#if (ai.children?contains (appendix.parent))>

=== ${appendix.title}
include::${appendix.file}[]
<#list subappendixs as subappendix>
<#if (subappendix.parent == appendix.children)>

==== ${subappendix.title}
include::${subappendix.file}[]
</#if>
</#list>
</#if>
</#list>
</#if>
</#list>
