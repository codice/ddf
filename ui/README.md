# UI

This is the root maven module that drives all the node based build tooling
for javascript projects in DDF.

To skip this module in a maven build, specify the `-pl !\ui` build flag.

**Warning:** If you choose to skip the ui module and the distribution
module is unable to find a previously built artifact, the build will fail.

## Getting Started

All of the javascript is structured as npm packages located in the
`packages` directory. They can have their own dependencies specified in
the `package.json` file. Those dependencies can be from an npm registry or
local packages in the `packages` directory. Package specific help may be
provided in the package's `README.md`.

To install all dependencies for all packages, do:

    yarn install

You can also run this command in any of the sub-packages, however it will
always install dependencies for all packages. This is a consequence of
using yarn workspaces. To find out more about yarn workspaces, please
visit their [documentation](https://yarnpkg.com/lang/en/docs/workspaces/).

### yarn.lock

The `yarn.lock` is checked into source control, managed by `yarn` and
provides deterministic builds. To do so, it captures the dependency graph
and the resolved versions based on the state of the yarn registry. It is
very **important** that this file is stable. If it has changes, ensure
those changes are related to your current changes and not a remnant of
another unrelated branch.

### Dev Server

Most of the packages have a `start` script to start running a development
server which serves local html/js/css content and proxies all other
requests to a running `DDF` instance. To invoke the start script, do:

    yarn start

The host and port which the dev server binds to is project specific, but
is most likely [http://localhost:8080](http://localhost:8080) or
[http://localhost:8282](http://localhost:8282).

### Web Context Policy Manager

The first issue most encounter when running the dev server is a redirect
to the IDP server. Currently authentication through IDP does not work
behind a proxy. Ideally, the IDP would work transparently behind a proxy,
at which point this solution will no longer be necessary. Until then, to
disable IDP we need to re-configure the web context policy manager.
Luckily, it's as easy as:

    yarn disable-idp

## Ace

As mentioned above, dependencies can be resolved locally. An example of a
locally shared dependency is `ace`. It is a node cli for interoperating
with the maven eco-system and is depended upon by any package that wishes
to produce a web app. **Most of the time you won't need to use `ace`
directly, but you may be using it indirectly through scripts specified in
the `package.json`**. This information is mostly for those who need to
know a little bit more about the ui build.

If you do need to interact with `ace` directly, do:

    yarn ace [options] [command]

or if you wish for `ace` to be accessible as a global command, do:

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

