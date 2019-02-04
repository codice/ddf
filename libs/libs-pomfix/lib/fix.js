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

var es = require('event-stream')
var combine = require('stream-combiner')
var p = require('path')

var notNull = function (o) { return o !== null }
var notUndefined = function (o) { return o !== undefined }

var processPom = function ($) {
  var descriptor = $('plugin descriptor')
    .map(function (i, el) {
      var descriptor = $(el).text().trim()
      var pattern = /mvn:(.*)\/(.*)\/(.*)\/xml\/features/
      var match = descriptor.match(pattern)

      if (match !== null) {
        return {
          group: match[1],
          artifact: match[2],
          version: match[3],
          type: 'xml',
          classifier: 'features'
        }
      }
    }).get()

  var artifactItem = $('plugin artifactItem')
    .map(function (i, el) {
      var value = function (tag) {
        return $(el).find(tag).first().text().trim() || undefined
      }

      return {
        group: value('groupId'),
        artifact: value('artifactId'),
        version: value('version'),
        scope: value('scope'),
        type: value('type')
      }
    }).get()

  return [].concat(descriptor).concat(artifactItem)
}

var processFeature = function ($) {
  var pattern = /.*mvn:([^/]+)\/([^/]+)\/([^/]+)(\/)?(.+)?/

  // find the implicit dependencies in the features.xml file
  return $('feature bundle')
    .map(function (i, el) {
      var bundle = $(el).text().trim().match(pattern)

      return {
        group: bundle[1],
        artifact: bundle[2],
        version: bundle[3],
        type: bundle[5]
      }
    }).get()
}

var makeDep = function ($) {
  var makeTag = function (tag) { return $('<' + tag + '>') }
  return function (dep) {
    var required = [
      makeTag('groupId').text(dep.group),
      makeTag('artifactId').text(dep.artifact)
    ]

    var optional = ['version', 'scope', 'classifier', 'type', 'optional']
      .map(function (tag) {
        if (dep[tag] !== undefined) {
          return makeTag(tag).text(dep[tag])
        }
      }).filter(notUndefined)

    return makeTag('dependency').append(required.concat(optional))
  }
}

module.exports = function (load, options) {
  var opts = options || {}

  var findMavenRoot = function (path) {
    var found = false

    return path
      .split(p.sep)
      .filter(function (dir) {
        if (dir === 'src') {
          found = true
        }

        return !found
      })
      .join(p.sep)
  }

  // filter out unneeded dependencies
  var exclude = function (dep) {
    var match = function (matchers, field) {
      if (field !== undefined) {
        var m = matchers.map(function (pattern) {
          return field.match(pattern)
        }).filter(notNull)
        if (m.length > 0) return m
      }
    }

    var map = function (obj, dep) {
      return Object.keys(obj || {}).reduce(function (prev, key) {
        var m = match(obj[key], dep[key])
        return (m !== undefined) ? prev.concat(m) : prev
      }, [])
    }

    if (map(opts.exclude, dep).length > 0) return false
    if (map(opts.include, dep).length > 0) return true

    return false // the default policy is to exclude any dep
  }

  var findDeps = function () {
    var findPom = function (path) {
      if (path.match('features.xml')) {
        return p.join(findMavenRoot(path), 'pom.xml')
      }
      return path
    }

    return es.through(function (entry) {
      var deps
      var path = entry.path
      var $ = load(path)

      if (path.match('pom.xml')) {
        deps = processPom($)
      } else if (path.match('features.xml')) {
        deps = processFeature($)
      }

      if (deps !== undefined && deps.length > 0) {
        this.emit('data', {
          path: findPom(path),
          deps: deps.filter(exclude)
        })
      }
    })
  }

  // inject deps into pom files
  var injectDeps = function () {
    // does the pom have a dependencies section?
    var hasDeps = function ($) {
      return $('project > dependencies').length === 1
    }

    // are two dependencies equal?
    var depEq = function (a) {
      return function (b) {
        return a.artifact === b.artifact && a.group === b.group
      }
    }

    // find if dependency already exists in a pom
    var find = function ($) {
      var self = depEq({
        artifact: $('project > artifactId').first().text(),
        group: $('project > groupId').first().text()
      })
      var deps = $('project > dependencies > dependency')
        .map(function (i, el) {
          return {
            artifact: $(el).find('artifactId').first().text(),
            group: $(el).find('groupId').first().text()
          }
        }).get()

      return function (dep) {
        return self(dep) || deps.filter(depEq(dep)).length > 0
      }
    }

    var addDeps = function ($, deps) {
      var findDep = find($)

      if (hasDeps($)) {
        return deps.filter(function (dep) {
          if (!findDep(dep)) {
            $('project > dependencies').append(makeDep($)(dep))
            return true
          }
        })
      } else {
        var dependencies = $('<dependencies>')
        dependencies.append(deps.map(makeDep($)))
        $('project').append(dependencies)
        return deps
      }
    }

    return es.through(function (entry) {
      var $ = load(entry.path)
      var deps = entry.deps
      var added = addDeps($, deps)

      if (added.length > 0) {
        this.emit('data', {
          path: entry.path,
          $: $,
          deps: added
        })
      }
    })
  }

  return combine([ findDeps(), injectDeps() ])
}
