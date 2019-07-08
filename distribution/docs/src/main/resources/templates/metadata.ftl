= ${branding} Metadata Reference Guide
include::config.adoc[]
:title: Metadata Reference Guide
:toc: left
:architecture-prefix: architecture.adoc#_
:developing-prefix: developing.adoc#_
:integrating-prefix: documentation.adoc#_
:introduction-prefix: introduction.adoc#_
:managing-prefix: managing.adoc#_
:metadata-prefix: _
:quickstart-prefix: quickstart.adoc#_
:reference-prefix: reference.adoc#_
:using-prefix: using.adoc#_

<#include "build/metadata-reference.ftl">

ifdef::backend-html5[]

include::${project.build.directory}/asciidoctor-ready-${project.version}/scripts.html[]
endif::backend-html5[]