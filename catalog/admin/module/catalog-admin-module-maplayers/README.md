# map-layers

The map-layers admin-console module.

## getting started

Before you start working on the ui, make sure all the dependencies are
installed. Simply do:

    yarn install

To start a development server, do:

    yarn start

This will load the ui in your browser and any updates to the source code
will automatically be pushed up to the browser.

## interesting places

- `./src/main/webapp`: where all of the JavaScript source code is.
- `./src/main/webapp/index.js`: the entry point into `map-layers`.
- `./src/main/resources/index.ejs`: the main html page for the app.
- `./target/webapp`: the build target for `map-layers`.
- `./target/webapp/bundle.js`: the compiled JavaScript file that gets
  loaded into the browser.

To find more interesting places, I suggest you inspect the
`webpack.config.js`.

## tests

A test file can be located anywhere in `./src/main/webapp/`, as long as it
ends with `spec.js`. It is beneficial to co-locate tests near code they
exercise to suggest a close relationship, as well as making the `import`
statements much simpler.

To develop/debug tests in a browser, do:

    yarn run start:test

To run all tests as part of a CI build in a headless browser, do:

    yarn test

After all tests have passed, coverage checks are run. The thresholds for
coverage are specificed in `.istanbul.yml`. To view an html report, do:

    yarn run coverage:report

## helpful links

Some useful information to help team members get acquainted with the
technologies used in this project. Please add to it as you find more
learning resources.

- [Getting Started with Redux](https://egghead.io/courses/getting-started-with-redux)
- [Building React Applications with Idiomatic Redux](https://egghead.io/courses/building-react-applications-with-idiomatic-redux)

It is highly recommended that you familiarize yourself with the technology
stack before you start hacking on map-layers.

