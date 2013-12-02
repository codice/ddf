Grunt development workflow, should help speed things up, no need to `mvn clean install` to see a change in css or javascript.  Workflow is NOT required to build with maven, works separately and parallel to it.  Here's how to use it:
First, make sure you have node and npm installed (npm comes with node installer)
From `search-ui/mapsearch` directory,  run `npm install` which will install the libraries needed
To serve up the application, run `node server.js`
To run tasks including 'watch', in a separate window run `grunt`

Make sure ddf services are running, and open browser window to localhost:8282 (configured in server.js)

benefits:
all changes are immediately linted, watch task will complain immediately if something goes wrong.
css changes minified and get pushed to server immediately without page refreshes
all changes to javascript files are available immediately, just refresh the page.  You may have to disable caching on your browser to see full effect of this

