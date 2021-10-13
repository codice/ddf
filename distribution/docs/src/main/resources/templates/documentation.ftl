= ${branding-expanded} Documentation: Complete Documentation
:title: ${branding-expanded} Documentation: Complete Documentation
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

<#include "introduction.ftl">

<#include "quickstart-build.ftl">

:sectnums!:
== Managing
:sectnums:

<#include "managing-build.ftl">

:sectnums!:
== Using
:sectnums:

<#include "using-build.ftl">

:sectnums!:
== Integrating
:sectnums:

<#include "integrating-build.ftl">

:sectnums!:
== Developing
:sectnums:

<#include "developing-build.ftl">

<#include "developing-components.ftl">

<#include "development-guidelines.ftl">

:sectnums!:
== Appendices
:sectnums:

[appendix]
== Application References

<#include "application-reference.ftl">

[appendix]
<#include "reference-build.ftl">

[appendix]
<#include "metadata-reference.ftl">

ifdef::backend-pdf[]

[index]
== Index
endif::backend-pdf[]


ifdef::backend-html5[]

include::${project.build.directory}/asciidoctor-ready-${project.version}/scripts.html[]
endif::backend-html5[]