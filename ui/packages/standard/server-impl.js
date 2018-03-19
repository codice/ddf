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
var URL = require('url'),
    httpProxy = require('http-proxy'),
    proxy = new httpProxy.RoutingProxy(),
    request = require('request'),
    fs = require('node-fs'),
    path = require('path'),
    _ = require('lodash');

var server = {};

server.requestProxy = function (req, res) {
    req.url = "https://localhost:8993" + req.url;
    var urlObj = URL.parse(req.url);
    req.url = urlObj.path;
    // Buffer requests so that eventing and async methods still work
    // https://github.com/nodejitsu/node-http-proxy#post-requests-and-buffering
    var buffer = httpProxy.buffer(req);
    console.log('Proxying Request "' + req.url + '"');

    process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";
    proxy.proxyRequest(req, res, {
        host: urlObj.hostname,
        port: urlObj.port || 80,
        buffer: buffer,
        changeOrigin: true,
        secure: false,
        target: {
            https: true
        }
    });
};

function getTestResource(name) {
    var resourceDir = path.resolve('.', 'src/test/resources');
    if (fs.existsSync(resourceDir)) {
        var resourcePath = path.resolve(resourceDir, name);
        return fs.readFileSync(resourcePath, {'encoding':'utf8'});
    }
    return undefined;
}

function mockTestResource (name, res) {
    var resource = getTestResource(name);
    if (resource) {
        sendJson(resource, res);
    } else {
        res.status(404).send('The specified resource does not exist.');
        res.end();
    }
}

function sendJson (data, res) {
    res.contentType('application/json');
    res.status(200).send(data);
}

server.mockRequest = function (req, res) {
    var filename = _.last(URL.parse(req.url).pathname.split('/')) + '.json';
    if (process.env.SAUCE_ACCESS_KEY && filename === 'config.json') {
        // Disable the large single image map tile due to limited bandwidth over
        // Sauce Connect tunnel
        var resource = JSON.parse(getTestResource(filename));
        resource.imageryProviders[0].url = 'http://localhost:8888/images/noimage.png';
        sendJson(resource, res);
    } else {
        mockTestResource(filename, res);
    }
};

module.exports = server;
