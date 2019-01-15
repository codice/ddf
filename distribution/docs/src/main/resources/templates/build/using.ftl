<#list usingIntros as ui>
<#if (ui.title == "Using Intro")>
include::${ui.file}[]
</#if>
</#list>

<#assign count=0>
<#list usings?sort_by("order") as userInterface>
<#if (userInterface.status == "published" && userInterface.summary != "")>
<#assign count++>
<<${userInterface.link?lower_case},${userInterface.title}>>:: ${userInterface.summary}
</#if>
</#list>
<#if (count == 0)>
None.
</#if>

<#assign count=0>
<#list usings?sort_by("order") as userInterface>
<#if (userInterface.status == "published" && userInterface.summary != "")>
<#assign count++>

== ${userInterface.title}

include::${userInterface.file}[]

</#if>
</#list>
<#if (count == 0)>
None.
</#if>

