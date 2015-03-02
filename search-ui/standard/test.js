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
var express = require('express'),
    http = require('http'),
    faye = require('faye'),
    fs = require('node-fs'),
    path = require('path'),
    server = require('./server-impl');

var app = express();
// uncomment to get some debugging
//app.use(express.logger());
//enable the live reload
//app.use(require('connect-livereload')());
//enable body parsing to make it easy to get at http request data
app.use(express.bodyParser());

// our compiled css gets moved to /target/webapp/css so use it there
app.use('/css',express.static(__dirname + '/target/webapp/css'));
app.use('/lib',express.static(__dirname + '/target/webapp/lib'));
app.use(express.static(__dirname + '/src/main/webapp'));

//if we're mocking, it is being run by grunt
console.log('setting up mock query endpoint');
app.all('/services/catalog/sources', server.mockRequest);
app.all('/services/store/config', server.mockRequest);
app.all('/services/platform/config/ui', server.mockRequest);
app.all('/services/user', server.mockRequest);

var bayeux = new faye.NodeAdapter({mount: '/cometd', timeout: 60});
app.listen = function() {
    var server = http.createServer(this);
    bayeux.attach(server);

    bayeux.getClient().subscribe('/service/user', function(message) {
        if (!message) {
            bayeux.getClient().publish('/service/user', {
                "successful": true,
                "user": {
                    "username": "Anonymous",
                    "isAnonymous": "true"
                }
            });
        }
    });

    bayeux.getClient().subscribe('/service/query', function(message) {
        if (message && message.id && message.id.match(/[a-f0-9]*-/)) {
            bayeux.getClient().publish('/' + message.id, JSON.parse(fs.readFileSync(
                path.resolve('.', 'src/test/resources', 'query.json'), {'encoding': 'utf8'})));
        }
    });

    return server.listen.apply(server, arguments);
};

exports = module.exports = app;

exports.use = function() {
	app.use.apply(app, arguments);
};