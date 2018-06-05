
== Development Guidelines

<#list developmentGuidelines?sort_by("order") as developmentGuideline>
<#if (developmentGuideline.status == "published")>

=== ${developmentGuideline.title}

include::${developmentGuideline.file}[]

</#if>
</#list>