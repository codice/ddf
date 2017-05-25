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

<#if (coreConcept.title?contains("Introduction"))>
==== ${coreConcept.title}
</#if>

include::${coreConcept.file}[]

</#if>
</#list>
<#if (count == 0)>
None.
</#if>
