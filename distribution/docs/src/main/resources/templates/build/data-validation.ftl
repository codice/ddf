
== Data and Metadata

<#list datas?sort_by("order") as data>
<#if (data.status == "published")>
include::${data.file}[]

</#if>
</#list>

=== Data Validation
<#list dataValidationIntros?sort_by("order") as dvi>
<#if (dvi.status == "published")>
include::${dvi.file}[]

</#if>
</#list>

.Available Validation Services
<#list dataValidations?sort_by("order") as dataValidation>
<#if (dataValidation.status == "published")>
<<${dataValidation.link},${dataValidation.title}>>:: ${dataValidation.summary}
</#if>
</#list>

<#list dataValidations?sort_by("order") as dataValidation>
<#if (dataValidation.status == "published")>

==== ${dataValidation.title}

include::${dataValidation.file}[]

</#if>
</#list>