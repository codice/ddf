
<#list usingIntros as ui>
<#if (ui.status == "published")>
include::${ui.file}[]

<#list usings?sort_by("order") as userInterface>
${userInterface.file}: ${userInterface.order}
<#if (userInterface.status == "published" && userInterface.parent == ui.title)>
include::${userInterface.file}[leveloffset=+1]

</#if>
</#list>
</#if>
</#list>


