= ${branding} User's Guide
include::config.adoc[]
:title: Using ${branding-expanded}
:architecture-prefix: architecture.adoc#_
:developing-prefix: developing.adoc#_
:integrating-prefix: documentation.adoc#_
:introduction-prefix: introduction.adoc#_
:managing-prefix: managing.adoc#_
:metadata-prefix: metadata.adoc#_
:quickstart-prefix: quickstart.adoc#_
:reference-prefix: reference.adoc#_
:using-prefix: _

== {title}

<#include "build/using.ftl">


ifdef::backend-html5[]

include::${project.build.directory}/asciidoctor-ready-${project.version}/scripts.html[]
endif::backend-html5[]