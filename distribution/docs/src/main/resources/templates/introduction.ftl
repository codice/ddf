== About ${branding}
<#list introductions?sort_by("priority") as intro>
<#if (intro.title == "Introduction")>
include::${intro.file}[]

</#if>
</#list>

<#list introductions as intro>
<#if (intro.section == "Applications")>
include::${intro.file}[leveloffset=+1]

</#if>
</#list>

<#list introductions as intro>
<#if (intro.section == "Documentation Guide")>
include::${intro.file}[]

</#if>
</#list>

<#assign count=0>
<#list introductions as intro>
<#if (intro.status == "published" && intro.section == "Core Concepts")>
include::${intro.file}[]

<#list coreConcepts?sort_by("order") as coreConcept>
<#if (coreConcept.status == "published")>
<#assign count++>
include::${coreConcept.file}[leveloffset=+1]
<#list subCoreConcepts?sort_by("order") as subCoreConcept>
<#if (subCoreConcept.section == "Core Concepts" && subCoreConcept.parent == coreConcept.title)>

include::${subCoreConcept.file}[leveloffset=+2]

</#if>
</#list>
</#if>
</#list>
</#if>
</#list>

<#if (count == 0)>
None.
</#if>