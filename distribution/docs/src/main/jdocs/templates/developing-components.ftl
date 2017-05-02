
<#list developingComponents as developingComponent>
<#if (developingComponent.status == "published")>

=== ${developingComponent.title}

include::${developingComponent.file}[]

</#if>
</#list>