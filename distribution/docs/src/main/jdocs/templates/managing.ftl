<#list managingIntros as mi>
<#if (mi.title == "Managing Intro" && mi.status == "published")>
include::${mi.file}[]
</#if>
</#list>

== Installing

<#list installingIntros as ii>
<#if (ii.title == "Installing Intro" && ii.status == "published")>
include::${ii.file}[]
</#if>
</#list>

<#-- installation steps have an 'order' property as they need to be performed in a specific order and documented as such. -->
<#list installings?sort_by("order") as installing>
<#if (installing.project == "${branding}" && installing.status == "published")>
include::${installing.file}[]

</#if>
</#list>
