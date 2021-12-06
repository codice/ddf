# DDF Documentation

Documentation is made available in a single document, or individual sections can be accessed based on audience need.

## Documentation Library

### DDF Introduction
Overview of what DDF is and what DDF does

### DDF: Quick Start Guide
Set up a test, demonstration, or evaluation instance of DDF 

### Managing DDF
How to install, configure, and maintain DDF

### DDF User's Guide
Learn your way around the DDF documentation

### Integrating with DDF
How to connect to and from DDF using service interfaces

### Developing DDF Components: Developer's Guide
Create custom implementations of DDF interfaces

### DDF Architecture
Learn about how the components of DDF work together

### DDF Reference Guide
Complete list of possible configurations

### DDF Metadata Reference Guide
Schemas and attributes used by DDF

### DDF Documentation: Complete Documentation
All of the above, compliled into a single document

## Documentation Module

### Build Commands

#### Build HTML documentation
`mvn clean install`

#### Build HTML and PDF documentation
`mvn clean install -Prelease`

#### Build Results

These artifacts are produced by the standard build
- `target/docs/html`: Rendered Content
- `target/export-docs/` Zip of raw source files to use in downstream project
- `target/export-templates/` Zip of template files to optionally use in downstream projects

### plugins

- `<maven-dependency-plugin>` This plugin retrieves any content from upstream projects. 
- `<maven-resources-plugin>` copies all of the content, images, scripts, and configuration files to the target directory. During this process, the text files are filtered to replace placeholders with the configured values.
- `<jbake-maven-plugin>` takes the filtered content pages and applies freemarker tempates to determine the order of the documents.
  - The asciidoc files (`.adoc`) have assigned `type`, `order` and `parent` properties in their headers that determine placement within larger sections.

_Asciidoctor header example_
```
:title: Configuring HTTP(S) Ports
:type: configuration
:status: published
:parent: Configuring Federation
:summary: Configure HTTP(S) Ports.
:order: 01
```

  - The freemarker templates (`*.ftl`) iterate on the `type` properties to determine the document placement. Types are defined in the `jbake.properties` file.

_jbake.properties excerpt_
```
output.extension=.adoc
template.documentation.file=documentation.ftl
template.introduction.file=overview.ftl
template.coreConcept.file=overview.ftl
template.subCoreConcept.file=overview.ftl
template.quickStart.file=quickstart.ftl
```
  - The `order` property of each `adoc` file sorts the sections and `parent` property matches subsections to their larger placement. The templates can also adjust the outline level of a subsection within the document. I use a nested loop to get a three-level outline structure.

_Freemarker (`*.ftl`) example_
```
<#list configuringIntros?sort_by("order") as ci>
<#if (ci.status == "published")>

\include::${ci.file}[leveloffset=+1]

<#list configurations?sort_by("order") as configuration>
<#if (configuration.parent == ci.title)>

\include::${configuration.file}[leveloffset=+2]

<#if (configuration.title == "Connecting to Sources")>

<#include "sources.ftl">
</#if>
<#list subConfigurations?sort_by("order") as subConfiguration>
<#if (subConfiguration.parent == configuration.title)>

\include::${subConfiguration.file}[leveloffset=+3]

</#if>
</#list>
</#if>
</#list>
</#if>
</#list>
```

- `<asciidoctor-maven-plugin>` compiles the newly-created asciidoc content into the final, publishable form.
  - `HTML` pages are run by default, `PDF` generation is optional in a development environment by creating a `<profile>` for PDF output.
- `<maven-assembly>` files package the raw adocs, and completed versions in zip files for deployment.

_asciidocs.xml excerpt_
```
<fileSet>
    <fileMode>0644</fileMode>
    <directoryMode>0755</directoryMode>
    <directory>src/main/resources/content</directory>
    <includes>
        <include>**</include>
    </includes>
    <outputDirectory></outputDirectory>
</fileSet>
```

- `<build-helper-maven-plugin>` deploys the published HTML and PDF files to the server for end-users to view.

_pom.xml excerpt_
```
<artifact>
    <file>${project.build.directory}/docs/html/documentation.html</file>
    <type>html</type>
    <classifier>documentation</classifier>
</artifact>
```
