<#list metadataIntros?sort_by("order") as mi>
<#if mi.status == "published">
include::${mi.file}[]

<#list metadataReferences?sort_by("order") as metadataReference>
<#if metadataReference.parent == mi.title>

include::${metadataReference.file}[leveloffset=+1]

<#list subMetadataReferences?sort_by("order") as subMetadataReference>
<#if subMetadataReference.parent == metadataReference.title>

include::${subMetadataReference.file}[leveloffset=+2]
</#if>
</#list>
</#if>
</#list>
</#if>
</#list>

