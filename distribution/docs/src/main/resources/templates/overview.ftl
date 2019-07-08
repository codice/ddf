= ${branding-expanded} Overview
include::config.adoc[]
:title: ${branding-expanded} Executive Summary: Core Concepts and Introduction
:architecture-prefix: architecture.adoc#_
:developing-prefix: developing.adoc#_
:integrating-prefix: documentation.adoc#_
:introduction-prefix: _
:managing-prefix: managing.adoc#_
:metadata-prefix: metadata.adoc#_
:quickstart-prefix: quickstart.adoc#_
:reference-prefix: reference.adoc#_
:using-prefix: using.adoc#_

<#include "build/introduction.ftl">

ifdef::backend-html5[]

include::${project.build.directory}/asciidoctor-ready-${project.version}/scripts.html[]
endif::backend-html5[]