= Developing ${branding} Components: Developer's Guide
:title: Developing ${branding} Components: Developer's Guide
include::config.adoc[]
:architecture-prefix: architecture.adoc#_
:developing-prefix: _
:integrating-prefix: documentation.adoc#_
:introduction-prefix: introduction.adoc#_
:managing-prefix: managing.adoc#_
:metadata-prefix: metadata.adoc#_
:quickstart-prefix: quickstart.adoc#_
:reference-prefix: reference.adoc#_
:using-prefix: using.adoc#_

<#include "developing-components.ftl">

<#include "development-guidelines.ftl">

ifdef::backend-html5[]

include::${project.build.directory}/asciidoctor-ready-${project.version}/scripts.html[]
endif::backend-html5[]