= ${branding-expanded} Documentation: Complete Documentation
include::${project.build.directory}/doc-contents/content/config.adoc[]
:application-prefix: _
:architecture-prefix: _
:integrating-prefix: _
:introduction-prefix: _
:developing-prefix: _
:managing-prefix: _
:metadata-prefix: _
:managing-prefix: _
:quickstart-prefix: _
:using-prefix: _

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

== Endpoints

<#include "build/endpoints.ftl">

== Eventing

<#include "build/eventing.ftl">

== Security Services

<#include "build/security-services.ftl">

:sectnums!:
== Developing
:sectnums:

<#include "build/developing.ftl">

:sectnums!:
== Appendices
:sectnums:

[appendix]
== Application References

<#include "build/application-reference.ftl">

<#include "build/appendices.ftl">

include::${project.build.directory}/doc-contents/content/scripts.html[]