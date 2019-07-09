= ${branding-expanded}: Quick Start Guide
include::config.adoc[]
:title: ${branding-expanded} Quick Start Guide
:architecture-prefix: architecture.adoc#_
:developing-prefix: developing.adoc#_
:integrating-prefix: documentation.adoc#_
:introduction-prefix: introduction.adoc#_
:managing-prefix: managing.adoc#_
:metadata-prefix: documentation.adoc#_
:quickstart-prefix: _
:reference-prefix: reference.adoc#_
:using-prefix: using.adoc#_

<#include "build/quickstart.ftl">

ifdef::backend-html5[]

include::${project.build.directory}/asciidoctor-ready-${project.version}/scripts.html[]
endif::backend-html5[]