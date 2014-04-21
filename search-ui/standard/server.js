/*global require,__dirname*/
var express = require('express'),
    server = require('./server-impl');

var app = express();
// uncomment to get some debugging
//app.use(express.logger());
//enable the live reload
app.use(require('connect-livereload')());

// our compiled css gets moved to /target/webapp/css so use it there
app.use('/css',express.static(__dirname + '/target/webapp/css'));
app.use('/lib',express.static(__dirname + '/target/webapp/lib'));
app.use(express.static(__dirname + '/src/main/webapp'));

//if we're mocking, it is being run by grunt
console.log('setting up proxy only');
app.all('/services/*', server.requestProxy);
app.all('/cometd/*', server.requestProxy);
app.all('/search/*', server.requestProxy);

exports = module.exports = app;

exports.use = function() {
	app.use.apply(app, arguments);
};