
<#list pluginIntros?sort_by("order") as pi>
<#if (pi.order == "00")>
include::${pi.file}[]
</#if>
<#if (pi.plugintypes != "general")>
<<${pi.link},${pi.title}>>:: ${pi.summary}
</#if>
<#if (pi.title == "Plugin Invocation")>
include::${pi.file}[]
</#if>

</#list>

.[[_catalog_plugin_compatibility]]Catalog Plugin Compatibility
[cols="9" options="header"]
|===

|Plugin
|<<_pre-authorization_plugins,Pre-Authorization Plugins>>
|<<_policy_plugins,Policy Plugins>>
|<<_access_plugins,Access Plugins>>
|<<_pre-ingest_plugins,Pre-Ingest Plugins>>
|<<_post-ingest_plugins,Post-Ingest Plugins>>
|<<_pre-query_plugins,Pre-Query Plugins>>
|<<_post-query_plugins,Post-Query Plugins>>
|<<_post-process_plugins,Post-Process Plugins>>

<#list plugins as plugin>
<#if (plugin.status == "published" && plugin.plugintypes?contains ("preauthorization") || plugin.plugintypes?contains ("policy") || plugin.plugintypes?contains ("access") || plugin.plugintypes?contains ("preingest") || plugin.plugintypes?contains ("postingest") || plugin.plugintypes?contains ("prequery") || plugin.plugintypes?contains ("postquery") || plugin.plugintypes?contains ("postprocess")) >
|<<${plugin.link},${plugin.title}>>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("preauthorization"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("policy"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("access"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("preingest"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("postingest"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("prequery"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("postquery"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("postprocess"))>x!
</#if>
<#else>
</#if>

</#list>
|===

.Catalog Plugin Compatibility, Cont.
[cols="11" options="header"]
|===

|Plugin
|<<_pre-federated_query_plugins,Pre-Federated-Query Plugins>>
|<<_post-federated_query_plugins,Post-Federated-Query Plugins>>
|<<_pre-resource_plugins,Pre-Resource Plugins>>
|<<_post-resource_plugins,Post-Resource Plugins>>
|<<_pre-create_storage_plugins,Pre-Create Storage Plugins>>
|<<_post-create_storage_plugins,Post-Create Storage Plugins>>
|<<_pre-update_storage_plugins,Pre-Update Storage Plugins>>
|<<_post-update_storage_plugins,Post-Update Storage Plugins>>
|<<_pre-subscription_plugins,Pre-Subscription Plugins>>
|<<_pre-delivery_plugins,Pre-Delivery Plugins>>

<#list plugins as plugin>
<#if (plugin.status == "published" && plugin.plugintypes?contains ("preresource") || plugin.plugintypes?contains ("postresource") || plugin.plugintypes?contains ("precreatestorage") || plugin.plugintypes?contains ("postcreatestorage") || plugin.plugintypes?contains ("preupdatestorage") || plugin.plugintypes?contains ("postupdatestorage") || plugin.plugintypes?contains ("presubscription") || plugin.plugintypes?contains ("predelivery"))>
|<<${plugin.link},${plugin.title}>>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("prefederatedquery"))>x!
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("postfederatedquery"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("preresource"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("postresource"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("precreatestorage"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("postcreatestorage"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("preupdatestorage"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("postupdatestorage"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("presubscription"))>x
</#if>
|<#if (plugin.status == "published" && plugin.plugintypes?contains ("predelivery"))>x
</#if>
</#if>

</#list>
|===

<#list pluginIntros?sort_by ("order") as pi>
<#if pi.status == "published" && pi.order != ("00") && pi.order != ("9999")>

==== ${pi.title}

include::${pi.file}[]

===== Available ${pi.title}

<#assign count=0>
<#list plugins as plugin>
<#if plugin.status == "published" && plugin.plugintypes?contains (pi.plugintypes)>
<#assign count++>
<<${plugin.link},${plugin.title}>>:: ${plugin.summary}
</#if>
</#list>
<#if count == 0>
None.
</#if>
</#if>
</#list>

=== Catalog Plugin Details

Installation and configuration details listed by plugin name.

<#list plugins as plugin>
<#if plugin.status == "published">

==== ${plugin.title}

include::${plugin.file}[]
</#if>
</#list>
