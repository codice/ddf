<#list transformerIntros as ti>
<#if (ti.title == "Transformers Intro" && ti.status == "published")>
include::${ti.file}[]
</#if>
</#list>

=== Available Input Transformers

The following input transformers are available in a standard installation of ${branding}:

<#assign count=0>
<#list transformers as transformer>
<#if (transformer.subtype?contains ("input") && transformer.status == "published")>
<#assign count++>
<<${transformer.link},${transformer.title}>>:: ${transformer.summary}

</#if>
</#list>
<#if (count == 0)>
None.
</#if>

=== Available Metacard Transformers

The following metacard transformers are available in a standard installation of ${branding}:

<#assign count=0>
<#list transformers as transformer>
<#if (transformer.subtype?contains ("metacard") && transformer.status == "published")>
<#assign count++>
<<${transformer.link},${transformer.title}>>:: ${transformer.summary}

</#if>
</#list>
<#if (count == 0)>
None.
</#if>

=== Available Query Response Transformers

The following query response transformers are available in a standard installation of ${branding}:

<#assign count=0>
<#list transformers as transformer>
<#if (transformer.subtype?contains ("queryResponse") && transformer.status == "published")>
<#assign count++>
<<${transformer.link},${transformer.title}>>:: ${transformer.summary}

</#if>
</#list>
<#if (count == 0)>
None.
</#if>

=== Transformers Details

Availability and configuration details of available transformers.

<#list transformers as transformer>
<#if (transformer.status == "published")>

==== ${transformer.title}

include::${transformer.file}[]

</#if>
</#list>

=== Mime Type Mapper

<#list transformerIntros as ti>
<#if (ti.title == "Mime Type Mapper Intro")>
include::${ti.file}[]
</#if>
</#list>

<#assign count=0>
<#list mimeTypeMappers as mimeTypeMapper>
<#if (mimeTypeMapper.status == "published")>
<#assign count++>
<<${mimeTypeMapper.link},${mimeTypeMapper.title}>>:: ${mimeTypeMapper.summary}

</#if>
</#list>
<#if (count == 0)>
None.
</#if>

<#list mimeTypeMappers as mimeTypeMapper>
<#if (mimeTypeMapper.status == "published")>

==== ${mimeTypeMapper.title}

include::${mimeTypeMapper.file}[]

</#if>
</#list>

=== Mime Type Resolver

<#list transformerIntros as ti>
<#if (ti.title == "Mime Type Resolver Intro")>
include::${ti.file}[]
</#if>

</#list>

<#assign count=0>
<#list mimeTypeResolvers as mimeTypeResolver>
<#if (mimeTypeResolver.status == "published")>
<#assign count++>
<<${mimeTypeResolver.link},${mimeTypeResolver.title}>>:: ${mimeTypeResolver.summary}

</#if>
</#list>
<#if (count == 0)>
None.
</#if>

<#list mimeTypeResolvers as mimeTypeResolver>
<#if (mimeTypeResolver.status == "published")>

==== ${mimeTypeResolver.title}

include::${mimeTypeResolver.file}[]

</#if>
</#list>