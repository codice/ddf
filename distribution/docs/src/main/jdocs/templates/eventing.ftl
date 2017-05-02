
<#list eventings as ev>
<#if (ev.title == "Eventing Intro")>

=== ${ev.title}

include::${ev.file}[]
<#else> icon::[star]
</#if>

<#if (ev.title == "Subscriptions")>

=== ${ev.title}

include::${ev.file}[]
<#else> icon[heart]
</#if>
</#list>