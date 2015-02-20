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
    request = require('request'),
    fs = require('node-fs'),
    path = require('path'),
    _ = require('lodash');

var server = {};

server.requestProxy = function (req, res) {
    req.pipe(request("http://localhost:8181" + req.url)).pipe(res);
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
    mockTestResource(_.last(URL.parse(req.url).pathname.split('/')) + '.json', res);
};

server.mockCometD = function (req, res) {
	//parse req body to figure out how to respond
    var json = [];
    _.each(req.body, function(dat) {
        if (dat.channel === '/meta/connect') {
            json.unshift(JSON.parse(getTestResource('connect.json')
                .replace('/e863f023-3b6f-4575-badf-a1f114e7b378', server.clientChannel)),
                {
                    "id": dat.id,
                    "advice": {
                        "interval": 0,
                        "reconnect": "retry",
                        "timeout": 30000
                    },
                    "channel": "/meta/connect"
                });
        } else {
            json.unshift({'id': dat.id, 'successful': true, 'channel': dat.channel});
            if (dat.subscription) {
                json[0].subscription = dat.subscription;
                if (dat.subscription.match(/[a-f0-9]*-/)) {
                    server.clientChannel = dat.subscription;
                }
            } else if (dat.channel === '/service/user') {
                json.unshift({
                    "data": {
                        "successful": true,
                        "user": {
                            "username": "Anonymous",
                            "isAnonymous": "true"}
                    },
                    "channel": "/service/user"});
            } else if (dat.channel === '/service/query') {
                json.unshift({'successful': 'true'});
            }
        }
    });
    sendJson(json, res);
};

module.exports = server;