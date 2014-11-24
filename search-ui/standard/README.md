
## To Build this Project

Java and JavaScript can be built with Maven without installing `node` or `npm` locally.

```
maven clean install
```

## Development with Grunt

###Building
An Express application has been included to allow rapid development without having to build with Maven and deploy bundles for every change.

* Install `node` and `npm` locally or use [these helper scripts](https://github.com/eirslett/frontend-maven-plugin/tree/master/frontend-maven-plugin/src/it/example/helper-scripts) to use the node and npm downloaded by Maven
* Install package.json with `npm`

```
npm install
```
* Make sure an instance of `ddf` is running locally on port 8181
* Run the default `grunt` task in a separate window

```
grunt
```
* Open http://localhost:8282 to test and debug

###Testing
Automated tests are executed as part of the build but it is possible to manually run the tests against a local Selenium or Webdriver server.
* Install and start a local Selenium server.  [Webdrvr](https://www.npmjs.org/package/webdrvr) can automate this process.

```
npm install -g webdrvr
webdrvr
```
* Use the `test:selenium` task with the `--browser` parameter.  You can specify `chrome`, `firefox`, `safari`, and `ie` as target browsers.

```
grunt test:selenium --browser=chrome
```
