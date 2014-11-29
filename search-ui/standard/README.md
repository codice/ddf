
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
* Open http://localhost:8282 to test and debug

###Testing
Automated tests are executed as part of the build but it is also possible to manually run the tests.

####Headless
Run `grunt test` to execute the automated tests against PhantomJS.

####Selenium
Automated tests can execute against a local Selenium server and target locally installed browsers.

* Install and start a local Selenium server.  [Webdrvr](https://www.npmjs.org/package/webdrvr) can automate this process.

```
npm install -g webdrvr
webdrvr
```
* Use the `test:selenium` task with the `--browser` parameter.  You can specify `chrome`, `firefox`, `safari`, and `ie` as target browsers.

```
grunt test:selenium --browser=chrome
```

####Sauce Labs
Additionally, tests can execute against [Sauce Labs](https://saucelabs.com/opensauce).

* Use the `test:sauce` task and provide Sauce Lab credentials as `SAUCE_USERNAME` and `SAUCE_ACCESS_KEY` environment variables.

```
SAUCE_USERNAME=<username> SAUCE_ACCESS_KEY=<key> grunt test:sauce
```