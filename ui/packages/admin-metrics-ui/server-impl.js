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
const URL = require('url'), httpProxy = require('http-proxy'), proxy = new httpProxy.RoutingProxy(), fs = require('node-fs'), path = require('path'), _ = require('lodash');

function stringFormat(format /* arg1, arg2... */) {
  if (arguments.length === 0) {
    return undefined
  }
  if (arguments.length === 1) {
    return format
  }
  const args = Array.prototype.slice.call(arguments, 1);
  return format.replace(/\{\{|\}\}|\{(\d+)\}/g, function(m, n) {
    if (m === '{{') {
      return '{'
    }
    if (m === '}}') {
      return '}'
    }
    return args[n]
  })
}

const server = {};

server.requestProxy = function(req, res) {
  'use strict'

  req.url = 'http://localhost:8181' + req.url
  const urlObj = URL.parse(req.url);
  req.url = urlObj.path
  // Buffer requests so that eventing and async methods will work
  // https://github.com/nodejitsu/node-http-proxy#post-requests-and-buffering
  const buffer = httpProxy.buffer(req);
  console.log('Proxying Request "' + req.url + '"')

  proxy.proxyRequest(req, res, {
    host: urlObj.hostname,
    port: urlObj.port || 80,
    buffer: buffer,
    changeOrigin: true,
  })
}

server.mockQueryServer = function(req, res) {
  const keyword = req.query.q;
  const resourceDir = path.resolve('.', 'src/test/resources');

  if (fs.existsSync(resourceDir)) {
    const files = fs.readdirSync(resourceDir);
    if (files.length === 0) {
      var message = stringFormat(
        "There was no file in the resource path '{0}'",
        resourceDir
      )
      res.status(404).send(message)
      res.end()
    } else if (files.length > 1) {
      const fileIdx = _.indexOf(files, keyword + '.json');
      if (fileIdx !== -1) {
        const resourcePath = path.resolve(resourceDir, files[fileIdx]);
        res.contentType('application/json')
        res.status(200).send(fs.readFileSync(resourcePath))
      } else {
        var message = stringFormat(
          "The specified query does not map to a cached file: '{0}'",
          keyword
        )
        res.status(404).send(message)
        res.end()
      }
    } else {
      var message = stringFormat('The specified resource does not exist.')
      res.status(404).send(message)
      res.end()
    }
  }
}

function getTestResource(name) {
  const resourceDir = path.resolve('.', 'src/test/resources');
  if (fs.existsSync(resourceDir)) {
    const resourcePath = path.resolve(resourceDir, name);
    return fs.readFileSync(resourcePath)
  }
  return undefined
}

function mockTestResource(name, res) {
  const resource = getTestResource(name);
  if (resource) {
    res.contentType('application/json')
    res.status(200).send(resource)
  } else {
    const message = stringFormat('The specified resource does not exist.');
    res.status(404).send(message)
    res.end()
  }
}

server.mockConfigStore = function(req, res) {
  mockTestResource('config.json', res)
}

server.mockSources = function(req, res) {
  mockTestResource('sources.json', res)
}

server.mockHandshake = function(req, res) {
  mockTestResource('handshake.json', res)
}

server.mockConnect = function(req, res) {
  mockTestResource('connect.json', res)
}

module.exports = server
