
## To Build this Project

Java and JavaScript can be built with Maven without installing `node` or `npm` locally.

```
maven clean install
```

## Development with Grunt

###Building
An Express application has been included to allow rapid development without having to build with Maven and deploy bundles for every change.

* Install `node` and `npm` locally or use [helper scripts](https://github.com/eirslett/frontend-maven-plugin#helper-scripts) to use the node and npm downloaded by Maven
* Install package.json with `npm`

```
npm install
```
* Make sure an instance of `ddf` is running locally on port 8181
* Run the default `grunt` task in a separate window

```
grunt
```
* Open http://localhost:8282 to test and debug.
Note that URLs (such as http://localhost:8282/search/standard/) must be terminated with the trailing slash in order for the grunt proxy to determine the url root correctly.

###Testing
Automated tests are executed as part of the maven build but it is also possible to manually run 
the tests.

####Headless
Run `npm test` to execute the automated tests using PhantomJS.
