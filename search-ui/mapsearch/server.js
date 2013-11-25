/*global require,__dirname*/
var httpProxy = require('http-proxy'),
    proxy = new httpProxy.RoutingProxy(),
    URL = require('url'),
    express = require('express');


var app = express();
// uncomment to get some debugging
//app.use(express.logger());
//enable the live reload
app.use(require('connect-livereload')());

// our compiled css gets moved to /target so use it there
app.use('/css',express.static(__dirname + '/target'));
//app.get('/foo/*',express.static(__dirname + '/src/main/webapp'));
app.use(express.static(__dirname + '/src/main/webapp'));

app.all('/services/*', function (req, res) {
    "use strict";

    req.url = "http://localhost:8181" + req.url;
    var urlObj = URL.parse(req.url);
    req.url = urlObj.path;
    // Buffer requests so that eventing and async methods still work
    // https://github.com/nodejitsu/node-http-proxy#post-requests-and-buffering
    var buffer = httpProxy.buffer(req);
    console.log('Proxying Request "' + req.url + '"');

    proxy.proxyRequest(req, res, {
        host: urlObj.hostname,
        port: urlObj.port || 80,
        buffer: buffer,
        changeOrigin: true
    });

});
var port = 8282;
app.listen(port);
console.log('listening for requests on port: ', port);

