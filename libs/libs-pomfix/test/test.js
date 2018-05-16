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

var test = require('tap').test
var join = require('path').join

var util = require('../lib/util')
var fix = require('../lib/fix')

var options = {
  include: {
    group: [/^org\.codice/, /^d../]
  }
}

var run = function (prj) {
  var dir = join(__dirname, 'resources', prj)
  return util.find(dir)
    .pipe(util.read())
    .pipe(fix(util.load, options))
}

test('catch feature errors', function (t) {
  t.plan(3)

  run('features')
    .on('data', function (data) {
      t.equal(data.deps.length, 1)

      var dep = data.deps[0]
      t.equal(dep.group, 'ddf.catalog.security')
      t.equal(dep.artifact, 'catalog-security-ingestplugin')
    })
})

test('catch artifact-item errors', function (t) {
  t.plan(3)

  run('artifact-items')
    .on('data', function (data) {
      t.equal(data.deps.length, 1)

      var dep = data.deps[0]
      t.equal(dep.group, 'ddf.lib')
      t.equal(dep.artifact, 'grunt-port-allocator')
    })
})

test('catch descriptor errors', function (t) {
  t.plan(3)

  run('descriptors')
    .on('data', function (data) {
      t.equal(data.deps.length, 1)

      var dep = data.deps[0]
      t.equal(dep.group, 'ddf.features')
      t.equal(dep.artifact, 'install-profiles')
    })
})
