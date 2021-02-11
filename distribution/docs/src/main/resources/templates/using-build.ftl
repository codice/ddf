
<#list usingIntros as ui>
<#if (ui.status == "published")>
include::${ui.file}[]

<#list usings?sort_by("order") as userInterface>
<#if (userInterface.status == "published" && userInterface.parent == ui.title)>

include::${userInterface.file}[leveloffset=+1]

<#list subUsings?sort_by("order") as subSection>
<#if (subSection.status == "published"&& subSection.parent == userInterface.title)>

include::${subSection.file}[leveloffset=+2]

</#if>
</#list>
</#if>
</#list>
</#if>
</#list>


