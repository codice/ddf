== About ${branding}
<#list introductions?sort_by("priority") as intro>
<#if (intro.title == "Introduction")>
include::${intro.file}[]

</#if>
</#list>

=== Component Applications

<#list introductions as intro>
<#if (intro.title == "Applications")>
include::${intro.file}[]

</#if>
</#list>

=== Documentation Guide

<#list introductions as intro>
<#if (intro.title == "Documentation Guide")>
include::${intro.file}[]

</#if>
</#list>

=== Core Concepts

<#assign count=0>
<#list coreConcepts?sort_by("order") as coreConcept>
<#if (coreConcept.status == "published")>
<#assign count++>

include::${coreConcept.file}[leveloffset=+2]

<#list subCoreConcepts?sort_by("order") as subCoreConcept>
<#if (subCoreConcept.parent == coreConcept.title)>

include::${subCoreConcept.file}[leveloffset=+3]

</#if>
</#list>
</#if>
</#list>
<#if (count == 0)>
None.
</#if>
