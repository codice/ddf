
== Eventing

<#list eventings as ev>
<#if (ev.title == "Eventing Intro")>

=== ${ev.title}

include::${ev.file}[]

</#if>

<#if (ev.title == "Subscriptions")>

=== ${ev.title}

include::${ev.file}[]

</#if>
</#list>