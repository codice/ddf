<#list managingIntros as mi>
<#if (mi.title == "Managing Intro" && mi.status == "published")>
include::${mi.file}[]
</#if>
</#list>

== Installing

<#list installingIntros as ii>
<#if (ii.title == "Installing Intro" && ii.status == "published")>
include::${ii.file}[]
</#if>
</#list>

<#-- installation steps have an 'order' property as they need to be performed in a specific order and documented as such. -->
<#list installings?sort_by("order") as installing>
<#if (installing.project == "${branding}" && installing.status == "published")>

include::${installing.file}[]

</#if>
</#list>

== Configuring

<#-- configuration steps have an 'order' property as they need to be performed in a specific order and documented as such. -->
<#list configuringIntros?sort_by("order") as ci>
<#if (ci.status == "published")>
include::${ci.file}[]

</#if>
</#list>

=== Configuring from the ${admin-console}

<#-- configuration steps have an 'order' property as they need to be performed in a specific order and documented as such. -->
<#list configuringAdminConsoles?sort_by("order") as configuringAdminConsole>
<#if (configuringAdminConsole.status == "published")>
include::${configuringAdminConsole.file}[]

</#if>
</#list>

=== Configuring from the ${command-console}

<#-- configuration steps have an 'order' property as they need to be performed in a specific order and documented as such. -->
<#list configuringCommandConsoles?sort_by("order") as configuringCommandConsole>
<#if (configuringCommandConsole.status == "published")>
include::${configuringCommandConsole.file}[]

</#if>
</#list>

=== Configuring from Configuration Files

<#-- configuration steps have an 'order' property as they need to be performed in a specific order and documented as such. -->
<#list configuringConfigFiles?sort_by("order") as configuringConfigFile>
<#if (configuringConfigFile.status == "published")>
include::${configuringConfigFile.file}[]

</#if>
</#list>

=== Importing Configurations

<#-- configuration steps have an 'order' property as they need to be performed in a specific order and documented as such. -->
<#list configuringImports?sort_by("order") as configuringImport>
<#if (configuringImport.status == "published")>
include::${configuringImport.file}[]

</#if>
</#list>

=== Other Configurations

<#-- configuration steps have an 'order' property as they need to be performed in a specific order and documented as such. -->
<#list configuringOthers?sort_by("order") as configuringOther>
<#if (configuringOther.status == "published")>
include::${configuringOther.file}[]

</#if>
</#list>

== Running

<#list runningIntros as ri>
<#if (ri.status == "published")>
include::${ri.file}[]
</#if>
</#list>

=== Starting

<#list startingIntros as si>
<#if (si.status == "published")>
include::${si.file}[]

</#if>
</#list>

=== Maintaining

<#list maintainings?sort_by("order") as maintaining>
<#if (maintaining.status == "published")>
include::${maintaining.file}[]

</#if>
</#list>

=== Monitoring

<#list monitorings?sort_by("order") as monitoring>
<#if (monitoring.status == "published")>
include::${monitoring.file}[]

</#if>
</#list>

=== Troubleshooting

<#list troubleshootings?sort_by("order") as troubleshooting>
<#if (troubleshooting.status == "published")>
include::${troubleshooting.file}[]

</#if>
</#list>

== Data Management

<#list dataManagements?sort_by("order") as dataManagement>
<#if (dataManagement.status == "published")>
include::${dataManagement.file}[]

</#if>
</#list>

