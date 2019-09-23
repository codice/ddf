= ${branding-expanded} Documentation: Complete Documentation
include::config.adoc[]
:architecture-prefix: _
:developing-prefix: _
:integrating-prefix: _
:introduction-prefix: _
:managing-prefix: _
:metadata-prefix: _
:quickstart-prefix: _
:reference-prefix: _
:using-prefix: _
:reference: appendix

:sectnums!:
== Introduction
:sectnums:

<#include "build/introduction.ftl">

<#include "build/quickstart.ftl">

:sectnums!:
== Managing
:sectnums:

<#include "build/managing.ftl">

:sectnums!:
== Using
:sectnums:

<#include "build/using.ftl">

:sectnums!:
== Integrating
:sectnums:

<#include "build/integrating.ftl">

:sectnums!:
== Developing
:sectnums:

<#include "build/developing.ftl">

<#include "build/developing-components.ftl">

<#include "build/development-guidelines.ftl">

:sectnums!:
== Appendices
:sectnums:

[appendix]
== Application References

<#include "build/application-reference.ftl">

[appendix]
<#include "build/reference.ftl">

[appendix]
<#include "build/metadata-reference.ftl">

ifdef::backend-pdf[]

[index]
== Index
endif::backend-pdf[]

ifdef::backend-html5[]

include::${project.build.directory}/asciidoctor-ready-${project.version}/scripts.html[]
endif::backend-html5[]