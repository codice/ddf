/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global require,__dirname*/
var express = require('express');
var httpProxy = require('http-proxy');
var morgan = require('morgan')

var proxy = new httpProxy.createProxyServer({ changeOrigin: true, secure: false });

proxy.on('error', function (error) {
    console.error('http-proxy', error);
});

proxy.on('proxyRes', function(proxyRes, req, res) {
    var cookie = proxyRes.headers['set-cookie'];
    if (cookie !== undefined) {
        // force the cookie to be insecure since the proxy is over http
        proxyRes.headers['set-cookie'] = cookie[0].replace(/;Secure/, '')
    }
});

var app = express();
// enable the live reload
app.use(require('connect-livereload')());

// our compiled css gets moved to /target/webapp/css so use it there
app.use('/search/catalog/', express.static(__dirname + '/target/webapp/'));
app.use('/search/catalog/', express.static(__dirname + '/src/main/webapp'));

app.use(morgan('dev'));

//if we're mocking, it is being run by grunt
app.use(function (req, res) {
    proxy.web(req, res, { target: 'https://localhost:8993' });
});

exports = module.exports = app;

exports.use = function() {
	app.use.apply(app, arguments);
};