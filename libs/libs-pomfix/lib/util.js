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

var fs = require('fs')
var cheerio = require('cheerio')
var beautify = require('vkbeautify')
var es = require('event-stream')
var readdirp = require('readdirp')

// global cache of parsed xml
var cache = {}

exports.load = function (key) {
  return cache[key]
}

// find all the feature/pom xml files
exports.find = function (dir, filter) {
  return readdirp({
    root: dir,
    fileFilter: ['pom.xml', 'features.xml', 'feature.xml'],
    directoryFilter: ['!target', '!node_modules']
      .concat(filter || [])
  })
}

var loadXml = function (data) {
  return cheerio.load(data, {
    xmlMode: true,
    decodeEntities: false
  })
}

exports.read = function () {
  return es.map(function (entry, cb) {
    var path = entry.fullPath

    fs.readFile(path, function (err, data) {
      if (err) {
        cb(err)
      } else {
        cache[path] = loadXml(data)
        cb(null, { path: path })
      }
    })
  })
}

exports.write = function () {
  return es.map(function (entry, cb) {
    var path = entry.path

    fs.writeFile(path, beautify.xml(entry.$.xml()), function (err, data) {
      if (err) {
        cb(err)
      } else {
        cb(null, entry)
      }
    })
  })
}
