/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/* eslint-disable */
var URL = require('url'),
  httpProxy = require('http-proxy'),
  proxy = new httpProxy.RoutingProxy(),
  fs = require('node-fs'),
  path = require('path'),
  _ = require('lodash')

function stringFormat(format /* arg1, arg2... */) {
  if (arguments.length === 0) {
    return undefined
  }
  if (arguments.length === 1) {
    return format
  }
  var args = Array.prototype.slice.call(arguments, 1)
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

var server = {}

server.requestProxy = function(req, res) {
  'use strict'

  req.url = 'https://localhost:8993' + req.url
  var urlObj = URL.parse(req.url)
  req.url = urlObj.path
  // Buffer requests so that eventing and async methods still work
  // https://github.com/nodejitsu/node-http-proxy#post-requests-and-buffering
  var buffer = httpProxy.buffer(req)
  console.log('Proxying Request "' + req.url + '"')

  process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0'
  proxy.proxyRequest(req, res, {
    host: urlObj.hostname,
    port: urlObj.port || 80,
    buffer: buffer,
    changeOrigin: true,
    secure: false,
    target: {
      https: true,
    },
  })
}

server.mockQueryServer = function(req, res) {
  var keyword = req.query.q
  var resourceDir = path.resolve('.', 'src/test/resources')

  if (fs.existsSync(resourceDir)) {
    var files = fs.readdirSync(resourceDir)
    if (files.length === 0) {
      var message = stringFormat(
        "There was no file in the resource path '{0}'",
        resourceDir
      )
      res.status(404).send(message)
      res.end()
    } else if (files.length > 1) {
      var fileIdx = _.indexOf(files, keyword + '.json')
      if (fileIdx !== -1) {
        var resourcePath = path.resolve(resourceDir, files[fileIdx])
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
  var resourceDir = path.resolve('.', 'src/test/resources')
  if (fs.existsSync(resourceDir)) {
    var resourcePath = path.resolve(resourceDir, name)
    return fs.readFileSync(resourcePath)
  }
  return undefined
}

function mockTestResource(name, res) {
  var resource = getTestResource(name)
  if (resource) {
    res.contentType('application/json')
    res.status(200).send(resource)
  } else {
    var message = stringFormat('The specified resource does not exist.')
    res.status(404).send(message)
    res.end()
  }
}

module.exports = server
