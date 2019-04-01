# UI

This is the root maven module that drives all the node based build tooling
for javascript projects in DDF.

To skip this module in a maven build, specify the `-pl !\ui` build flag.

To build the `development` bundles in a maven build, specify the
`-DwebBuildEnv=dev` flag.

**Warning:** If you choose to skip the ui module and the distribution
module is unable to find a previously built artifact, the build will fail.

## Getting Started

All of the javascript is structured as npm packages located in the
`packages` directory. They can have their own dependencies specified in
the `package.json` file. Those dependencies can be from an npm registry or
local packages in the `packages` directory. Package specific help may be
provided in the package's `README.md`.

To add the maven versions of `node` and `yarn` to your $PATH, do:

    source path.sh # you must have run `mvn install` before this

To install all dependencies for all packages, do:

    yarn install

You can also run this command in any of the sub-packages, however it will
always install dependencies for all packages. This is a consequence of
using yarn workspaces. To find out more about yarn workspaces, please
visit their [documentation](https://yarnpkg.com/lang/en/docs/workspaces/).

### Building

To build all packages, do:

    yarn build

This will run the `"build"` yarn script for all sub-packages. Any package
that does not specify a build script will be skipped. To build a single
module, cd into the package and run the same command.

Packages have the freedom to specify how they are built. Some use
[ace](./packages/ace) which mostly just wraps webpack and specifies a
default [webpack.config.js](./packages/ace/lib/webpack.config.js), some
use [grunt](https://gruntjs.com/). New packages should favor ace over
grunt.

### Tests

To test all packages, do:

    yarn test

This will run the `"test"` yarn script for all sub-packages. Any package
that does not specify a test script will be skipped. To test a single
module, cd into the package and run the same command.

Packages have the freedom to specify how tests should be run. Some use
[ace](./packages/ace) which mostly just wraps
[mocha](https://mochajs.org/) and [phantom](http://phantomjs.org/), some
use [tap](https://www.node-tap.org/basics/). New packages should favor
mocha over tap and should prefer to run in node if they do not require a
dom.

#### Test Server

Some of the packages have a `start:test` script to start running a test
server which serves local spec files that can be developed / debugged in
many browsers. To invoke the start:test script, do:

    yarn start:test

The host and port which the test server binds to is project specific, but
is most likely [http://localhost:8080](http://localhost:8080) or
[http://localhost:8282](http://localhost:8282).

### Formatting

To format the code in all sub-packages, do:

    yarn fmt

If using [Visual Studio Code](https://code.visualstudio.com/), consider
installing the
[prettier](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode)
extension so you can run the formatter in your editor. The extension should
be able to pick up the [`.prettierrc.js`](./.prettierrc.js) config.

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

### Extensibility

Currently none of the sub-packages are published to a public javascript
registry such as npm. To get access to the packages you must either pull
the code from github, or get them from maven. Along with all the web
application bundles that are built, we also publish a sources zip
specified by [`./packages.xml`](./packages.xml). Currently, we publish
sources for:

- ace
- linter
- codice-icons
- catalog-ui-search

To build and deploy the sources artifact into your local `~/.m2`, do:

    yarn m2

This will zip up all the packages listed above into a single zip and
install it into your local m2 using the maven coordinate
`mvn:org.codice.ddf/ui/${project.version}/zip/packages`.

To pull in the sources downstream, and deploy them to the target folder,
add this to your pom.xml:

```xml
<build>
  <plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
            <execution>
                <id>unpack</id>
                <phase>generate-resources</phase>
                <goals>
                    <goal>unpack</goal>
                </goals>
                <configuration>
                    <artifactItems>
                        <artifactItem>
                            <groupId>org.codice.ddf</groupId>
                            <artifactId>ui</artifactId>
                            <version>${ddf.version}</version>
                            <type>zip</type>
                            <classifier>packages</classifier>
                        </artifactItem>
                    </artifactItems>
                    <outputDirectory>${project.build.directory}</outputDirectory>
                    <overWriteSnapshots>true</overWriteSnapshots>
                </configuration>
            </execution>
        </executions>
    </plugin>
  </plugins>
</build>
```

Exporting sources is necessary but not always sufficient for
extensibility. Specifically, catalog-ui-seach has a variety of
[plugins](./packages/catalog-ui-search/src/main/webapp/plugins). Currently
they are mostly experimental, so no guarantees are made about future
compatibility.

#### Trade-offs

- Tooling is easily shared (through ace) and *mostly* just works. This
  reduces a lot of boilerplate and duplication. It also improves
  consistency between projects since they all play by the same rules.

- A lot of breakage can be determined at build time, especially as we
  introduce more static analysis through eslint and typescript.

- As we move to a more component-oriented architecture, downstream
  projects can more easily mix-and-match generic components. This would
  reduce the need for specialized plugins.

- If downstream projects depend on a SNAPSHOT version of the sources
  artifact and are using a lock file such as `yarn.lock` or
  `package-lock.json` they will be unstable. Any dependency changes in
  upstream dependencies will cascade downstream without any warning.

- Package structure changes can cause breakage downstream and is something
  we need to be diligent about for the exported packages listed above.
