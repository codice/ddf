
<#list architectureIntros as ai>
<#if (ai.status == "published")>

== ${ai.title}

include::${ai.file}[]

'''
<#list architectures as architecture>
<#if (architecture.status == "published") && (ai.children?contains ("architecture.parent"))>

=== ${architecture.title}

include::${architecture.file}[]
<#list subarchitectures as subarchitecture>
<#if (subarchitecture.status == "published") && (architecture.children?contains ("subarchitecture.parent"))>
include::${subarchitecture.file}[]

</#if>
</#list>
</#if>
</#list>
</#if>
</#list>