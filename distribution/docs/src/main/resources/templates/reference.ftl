= ${branding} Reference Guide
:title: ${branding} Reference Guide
include::config.adoc[]
:toc: left
:architecture-prefix: architecture.adoc#_
:developing-prefix: developing.adoc#_
:integrating-prefix: documentation.adoc#_
:introduction-prefix: introduction.adoc#_
:managing-prefix: managing.adoc#_
:metadata-prefix: metadata.adoc#_
:quickstart-prefix: quickstart.adoc#_
:reference-prefix: _
:using-prefix: using.adoc#_

<#include "application-reference.ftl">

<#include "reference-build.ftl">

ifdef::backend-pdf[]

<<< 

[index]
== Index

endif::backend-pdf[]
ifdef::backend-html5[]

include::${project.build.directory}/asciidoctor-ready-${project.version}/scripts.html[]
endif::backend-html5[]