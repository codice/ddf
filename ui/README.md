# UI

This is the root maven module that drives all the node based build tooling
for javascript projects in DDF.

To skip this module in a maven build, specify the `-pl !\ui` build flag.

## Getting Started

All of the javascript is structured as npm packages located in the
`packages` directory. They can have their own dependencies specified in
the `package.json` file. Those dependencies can be from npm or packages in
the `packages` directory. Package specific help may be provided in the
package's `README.md`.

### Ace

An existing example of a locally shared dependency is `ace`. It is a node
cli for interoperating with the maven eco-system and is depended upon by
any package that wishes to produce a web app. If you wish for `ace` to be
accessible globally, do:

    cd packages/ace/ && yarn link

To get more help on `ace`, do:

    ace --help

### Web Apps

Any `package.json` with the `context-path` key is designated as a web app.
Web apps should produce (with the help of `ace package`) an OSGI bundle
that will be attached as an artifact to the root ui maven artifact. To
attach a new artifact, do:

    ace pom

### OSGI Hot Deploy

To hot deploy a single web app, instead of building the entire maven
project, you can run `ace package` and `ace install` at the root of the
package. This will package any changes and install the OSGI bundle into
your local m2 using the maven coordinate
`mvn:org.codice.ddf/ui/${project.version}/jar/${package.name}`.

