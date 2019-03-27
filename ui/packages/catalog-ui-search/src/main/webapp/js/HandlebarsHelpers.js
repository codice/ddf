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
/*global define*/
const _ = require('underscore')
const moment = require('moment')
const Handlebars = require('@connexta/ace/handlebars/runtime')
const Common = require('./Common.js')
const metacardDefinitions = require('../component/singletons/metacard-definitions.js')
const _get = require('lodash/get')
const $ = require('jquery')

function bind(options, callback) {
  options.data.root._view.listenTo(
    options.data.root._view.model,
    options.hash.event || 'change',
    callback
  )
  options.data.root._view.listenToOnce(
    options.data.root._view,
    'before:render',
    function() {
      options.data.root._view.stopListening(
        options.data.root._view.model,
        options.hash.event || 'change',
        callback
      )
    }
  )
  return _get(options.data.root, options.hash.key)
}

// The module to be exported
var helper,
  helpers = {
    /*
     * Handlebars Helper: Moment.js
     * @author: https://github.com/Arkkimaagi
     * Built for Assemble: the static site generator and
     * component builder for Node.js, Grunt.js and Yeoman.
     * http://assemble.io
     *
     * Copyright (c) 2013, Upstage
     * Licensed under the MIT license.
     */
    momentHelp: function(context, block) {
      var momentObj, date, i
      if (context && context.hash) {
        block = _.cloneDeep(context)
        context = undefined
      }
      momentObj = moment(context)
      for (i in block.hash) {
        if (momentObj[i]) {
          if (typeof momentObj[i] === 'function') {
            var func = momentObj[i]
            date = func.call(momentObj, block.hash[i])
          }
        } else {
          if (typeof console !== 'undefined') {
            console.log('moment.js does not support "' + i + '"')
          }
        }
      }
      return date
    },
    duration: function(context, block) {
      if (context && context.hash) {
        block = _.cloneDeep(context)
        context = 0
      }
      var duration = moment.duration(context)
      // Reset the language back to default before doing anything else
      duration = duration.lang('en')
      for (var i in block.hash) {
        if (duration[i]) {
          duration = duration[i](block.hash[i])
        } else {
          console.log('moment.js duration does not support "' + i + '"')
        }
      }
      return duration
    },
    fileSize: function(item) {
      return Common.getFileSize(item)
    },
    fileSizeGuaranteedInt: function(item) {
      return Common.getFileSizeGuaranteedInt(item)
    },
    isNotBlank: function(context, block) {
      if (context && context !== '') {
        return block.fn(this)
      } else {
        return block.inverse(this)
      }
    },
    is: function(value, test, options) {
      if (value === test) {
        return options.fn(this)
      } else {
        return options.inverse(this)
      }
    },
    isnt: function(value, test, options) {
      if (value !== test) {
        return options.fn(this)
      } else {
        return options.inverse(this)
      }
    },
    isAnd: function() {
      var args = _.flatten(arguments)
      var items = _.initial(args)
      var result = true
      var block = _.last(args)
      _.each(items, function(item, i) {
        if (i % 2 === 0) {
          if (item !== items[i + 1]) {
            result = false
          }
        }
      })
      if (result) {
        return block.fn(this)
      } else {
        return block.inverse(this)
      }
    },
    isUrl: function(value, options) {
      if (value !== null && value !== '' && _.isString(value)) {
        var protocol = value.toLowerCase().split('/')[0]
        if (protocol && (protocol === 'http:' || protocol === 'https:')) {
          return options.fn(this)
        }
      }
      return options.inverse(this)
    },
    gt: function(value, test, options) {
      if (value > test) {
        return options.fn(this)
      } else {
        return options.inverse(this)
      }
    },
    gte: function(value, test, options) {
      if (value >= test) {
        return options.fn(this)
      } else {
        return options.inverse(this)
      }
    },
    lt: function(value, test, options) {
      if (value < test) {
        return options.fn(this)
      } else {
        return options.inverse(this)
      }
    },
    lte: function(value, test, options) {
      if (value <= test) {
        return options.fn(this)
      } else {
        return options.inverse(this)
      }
    },
    ifAnd: function() {
      var args = _.flatten(arguments)
      var items = _.initial(args)
      var result = true
      var block = _.last(args)
      _.each(items, function(item) {
        if (!item) {
          result = false
        }
      })
      if (result) {
        return block.fn(this)
      } else {
        return block.inverse(this)
      }
    },
    ifOr: function() {
      var args = _.flatten(arguments)
      var items = _.initial(args)
      var result = false
      var block = _.last(args)
      _.each(items, function(item) {
        if (item) {
          result = true
        }
      })
      if (result) {
        return block.fn(this)
      } else {
        return block.inverse(this)
      }
    },
    ifNotAnd: function() {
      var args = _.flatten(arguments)
      var items = _.initial(args)
      var result = true
      var block = _.last(args)
      _.each(items, function(item) {
        if (!item) {
          result = false
        }
      })
      if (result) {
        return block.inverse(this)
      } else {
        return block.fn(this)
      }
    },
    ifNotOr: function() {
      var args = _.flatten(arguments)
      var items = _.initial(args)
      var result = false
      var block = _.last(args)
      _.each(items, function(item) {
        if (item) {
          result = true
        }
      })
      if (result) {
        return block.inverse(this)
      } else {
        return block.fn(this)
      }
    },
    propertyTitle: function(str) {
      if (_.isString(str)) {
        return _.chain(str)
          .words()
          .map(function(word) {
            return _.capitalize(word)
          })
          .join(' ')
      }
      return str
    },
    safeString: function(str) {
      if (_.isString(str)) {
        return new Handlebars.SafeString(str)
      }
      return str
    },
    splitDashes: function(str) {
      return str.split('-').join(' ')
    },
    encodeString: function(str) {
      if (_.isString(str)) {
        return encodeURIComponent(str)
      }
      return str
    },
    getImageSrc: function(img) {
      return Common.getImageSrc(img)
    },
    getResourceUrlFromThumbUrl: function(img) {
      return Common.getResourceUrlFromThumbUrl(img)
    },
    getAlias: function(field) {
      var definition = metacardDefinitions.metacardTypes[field]
      if (definition) {
        return definition.alias || definition.id
      } else {
        return field
      }
    },
    json: function(obj) {
      return JSON.stringify(obj)
    },
    ifUrl: function(value, options) {
      if (value && value.toString().substring(0, 4) === 'http') {
        return options.fn(this)
      } else {
        return options.inverse(this)
      }
    },
    bindInput: function(options) {
      var callback = function() {
        var $target = this.$el.find(options.hash.selector)
        var value = _get(this.serializeData(), options.hash.key)
        $target.each(function() {
          if ($(this).val() !== value) {
            $(this).val(value)
          }
        })
      }
      return bind(options, callback)
    },
    bindAttr: function(options) {
      var callback = function() {
        var $target = this.$el.find(options.hash.selector)
        var value = _get(this.serializeData(), options.hash.key)
        $target.attr(options.hash.attr, value)
      }
      return bind(options, callback)
    },
    bind: function(options) {
      var callback = function() {
        var $target = this.$el.find(options.hash.selector)
        var value = _get(this.serializeData(), options.hash.key)
        $target.html(Common.escapeHTML(value))
      }
      return bind(options, callback)
    },
    path: function() {
      var outArray = []
      for (var arg = 0; arg < arguments.length; arg++) {
        if (typeof arguments[arg] === 'object') {
          break
        }
        if (typeof arguments[arg] === 'number') {
          outArray[outArray.length - 1] =
            outArray[outArray.length - 1] + arguments[arg] + arguments[arg + 1]
          arg++
        } else {
          outArray.push(arguments[arg])
        }
      }
      return outArray
    },
  }
// Export helpers
for (helper in helpers) {
  if (helpers.hasOwnProperty(helper)) {
    Handlebars.registerHelper(helper, helpers[helper])
  }
}

module.exports = helpers
