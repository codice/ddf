
<#list architectureIntros?sort_by("order") as ai>
<#if (ai.status == "published")>

== ${ai.title}

include::${ai.file}[]

'''
<#list architectures?sort_by("order") as architecture>
<#if (architecture.status == "published") && (ai.title?contains (architecture.parent))>

=== ${architecture.title}

include::${architecture.file}[]
<#list subArchitectures?sort_by("order") as subArchitecture>
<#if (subArchitecture.status == "published") && (architecture.title?contains (subArchitecture.parent))>

==== ${subArchitecture.title}

include::${subArchitecture.file}[]

</#if>
</#list>
</#if>
</#list>
</#if>
</#list>