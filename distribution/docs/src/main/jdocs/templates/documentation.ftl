= ${branding-expanded} Documentation
include::${project.build.directory}/doc-contents/_contents/config.adoc[]

:sectnums!:
== Introduction
:sectnums:

<#include "introduction.ftl">

== Quick Start Tutorial
<#include "quickstart.ftl">

:sectnums!:
== Managing
:sectnums:

<#include "managing.ftl">

:sectnums!:
== Using
:sectnums:

<#include "using.ftl">

:sectnums!:
== Integrating
:sectnums:

<#include "integrating.ftl">

== Endpoints

<#include "endpoints.ftl">

== Eventing

<#include "eventing.ftl">

== Sources

<#include "sources.ftl">

== Security Services

<#include "security-services.ftl">

:sectnums!:
== Developing
:sectnums:

<#include "developing.ftl">

== Transformers

<#include "transformers.ftl">

== Catalog Plugins
<#include "plugins.ftl">

<#include "developing-components.ftl">

:sectnums!:
== Appendices
:sectnums:

[appendix]
== Application References

<#include "application-reference.ftl">

<#include "appendices.ftl">

// Include iframeResizer to fix scolling issue when displayed in admin-ui.
++++
<script type="text/javascript" src="/admin/iframe-resizer/2.6.2/js/iframeResizer.contentWindow.min.js"></script>
<script type="text/javascript" >
"use strict";

function makeTocExpandable() {
	var level = 1;

	while (level < 6) {
	const tocHeading = document.querySelectorAll(".sectlevel" + level + ">li");
		for (var i = tocHeading.length - 1; i >= 0; i--) {
			const childLevels = tocHeading[i].querySelector("ul li");
			if (childLevels) {
				tocHeading[i].innerHTML ='<a class="expandable">+ </a>' + tocHeading[i].innerHTML + '';
			}
		}
		const levelUp = level + 1
		const higherlevels = document.querySelectorAll(".sectlevel" + levelUp );

		for (var i = higherlevels.length - 1; i >= 0; i--) {
			higherlevels[i].classList.add("collapsed");
		}
		level++;
	}

}

function expandList(section) {
	const currentSection = section.target.parentNode;

	currentSection.querySelector("ul").classList.remove("collapsed");
	currentSection.querySelector(".expandable").innerHTML = "- ";
	currentSection.querySelector(".expandable").removeEventListener("click", expandList);
	currentSection.querySelector(".expandable").classList.add("expanded");
	currentSection.querySelector(".expanded").classList.remove("expandable");
	currentSection.querySelector(".expanded").addEventListener("click", collapseList);
}

function collapseList(section) {
	const currentSection = section.target.parentNode;

	currentSection.querySelector("ul").classList.add("collapsed");
	currentSection.querySelector(".expanded").innerHTML = "+ ";
	currentSection.querySelector(".expanded").removeEventListener("click", collapseList);
	currentSection.querySelector(".expanded").classList.add("expandable");
	currentSection.querySelector(".expandable").classList.remove("expanded");
	currentSection.querySelector(".expandable").addEventListener("click", expandList);
}

function addTocListeners() {
	const expandableSections = document.querySelectorAll(".expandable")
	const expandedSections = document.querySelectorAll(".expanded")

	for (var i = expandableSections.length - 1; i >= 0; i--) {
		expandableSections[i].addEventListener("click", expandList);
	}
	for (var i = expandedSections.length - 1; i >= 0; i--) {
		expandedSections[i].addEventListener("click", collapseList);
	}
}

document.onreadystatechange = () => {
	if (document.readyState === 'complete') {
	  // The page is fully loaded
		makeTocExpandable();
		addTocListeners();
	}
};
</script>
<style>
.collapsed {display: none;}
</style>
++++
