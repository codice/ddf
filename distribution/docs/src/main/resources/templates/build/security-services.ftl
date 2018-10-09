
== Security Services
// TODO: additional security services content per: https://codice.atlassian.net/browse/DDF-2648
<#list securityServices?sort_by("order") as ss>
<#if ss.status == "published">

include::${ss.file}[]
</#if>
</#list>

=== Security IdP

<#list securityIdps?sort_by("order") as securityIdp>
<#if securityIdp.status == "published">
include::${securityIdp.file}[]

</#if>
</#list>

=== Security STS
<#list securityStss?sort_by("order") as securitySts>
<#if securitySts.status == "published" && securitySts.order == "00">
include::${securitySts.file}[]

</#if>

<#if securitySts.status == "published" && securitySts.order != "00">

==== ${securitySts.title}

include::${securitySts.file}[]
</#if>
</#list>