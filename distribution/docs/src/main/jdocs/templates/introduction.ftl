<#list introductions as intro>
<#if (intro.title == "Introduction")>
include::${intro.file}[]
</#if>
</#list>

=== Applications

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
<#list coreConcepts as coreConcept>
<#if (coreConcept.status == "published")>
<#assign count++>

==== ${coreConcept.title}

include::${coreConcept.file}[]

</#if>
</#list>
<#if (count == 0)>
None.
</#if>
