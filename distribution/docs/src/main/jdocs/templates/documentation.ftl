= ${branding-expanded} Documentation
include::${project.build.directory}/doc-contents/_contents/config.adoc[]

:sectnums!:
== Introduction
:sectnums:

<#include "introduction.ftl">
include::${project.build.directory}/doc-contents/_contents/config.adoc[]

:sectnums!:
== Managing
:sectnums:

<#-- <#include "quickstart.ftl"> -->

<#include "managing.ftl">

:sectnums!:
== Integrating
:sectnums:

== Endpoints

<#include "endpoints.ftl">

== Eventing

<#include "eventing.ftl">

== Sources

<#include "sources.ftl">

:sectnums!:
== Developing
:sectnums:

== Transformers

<#include "transformers.ftl">

<#include "developing-components.ftl">
