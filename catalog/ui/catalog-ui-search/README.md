
## To Build this Project

Java and JavaScript can be built with Maven without installing `node` or `npm` locally.

```
maven clean install
```

## Development with Webpack Dev Server

###Building
You can use the webpack dev server for rapid development without having to build with Maven and deploy bundles for every change.

* Install `node` and `npm` locally or use [helper scripts](https://github.com/eirslett/frontend-maven-plugin#helper-scripts) to use the node and npm downloaded by Maven
* Install package.json with `npm`

```
yarn install
```
* Make sure an instance of `ddf` is running locally on port 8993

```
npm run startplus
```
* Opens the browser for you.

* Alternatively
```
npm start
```
* Open http://localhost:8080/ to test and debug.

Note that the various variables (commit hash, commit date, isDirty) will be tied to when the dev server is initialized.

###Testing
Automated tests are executed as part of the maven build but it is also possible to manually run 
the tests.

####Headless
Run `npm test` to execute the automated tests using PhantomJS.

Run `npm testplus` to execute the automated tests in a browser.

###Layers for testing
https://server.arcgisonline.com/arcgis/rest/services/