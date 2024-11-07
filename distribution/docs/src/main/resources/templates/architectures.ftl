= ${branding-expanded} Architecture
:title: ${branding-expanded} Architecture
include::config.adoc[]
:architecture-prefix: _
:developing-prefix: developing.adoc#_
:integrating-prefix: documentation.adoc#_
:introduction-prefix: introduction.adoc#_
:managing-prefix: managing.adoc#_
:metadata-prefix: metadata.adoc#_
:quickstart-prefix: quickstart.adoc#_
:reference-prefix: reference.adoc#_
:using-prefix: using.adoc#_

<#include "developing-build.ftl">

ifdef::backend-pdf[]

<<< 

[index]
== Index

endif::backend-pdf[]

ifdef::backend-html5[]

include::${project.build.directory}/asciidoctor-ready-${project.version}/scripts.html[]
endif::backend-html5[]