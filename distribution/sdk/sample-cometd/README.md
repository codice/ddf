# DDF Asynchronous API Browser Example
Example of how to interact with the DDF Asynchronous API in the Browser

## Usage
This example assumes that the CometD endpoint is available at `https://localhost:8993/cometd`, if another url is required change the `cometURL` variable in `src/main/webapp/js/application.js`

### DDF
To use in DDF run `mvn install` and `cp target/sample-cometd.jar $DDF_HOME/deploy` then go to `https://localhost:8993/sample/cometd`

### Grunt
To run inside of a standalone connect server simply run `npm install` then run `grunt` and browse to `http://localhost:8000`
