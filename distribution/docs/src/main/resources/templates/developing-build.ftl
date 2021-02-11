<#list developingIntros?sort_by("order") as di>
<#if (di.status == "published")>
include::${di.file}[]

</#if>
</#list>

<#include "catalog-frameworks.ftl">

<#include "transformers.ftl">

<#include "plugins.ftl">

<#include "architectures-build.ftl">

<#include "security-framework.ftl">