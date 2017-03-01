
<#list developingComponents as developingComponent>
<#if (developingComponent.status == "published")>

=== ${developingComponent.title}

include::${developingComponent.projectpath}/_developingComponents/${developingComponent.filename}[]

</#if>
</#list>