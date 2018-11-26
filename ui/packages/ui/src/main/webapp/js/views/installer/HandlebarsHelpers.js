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
define(['icanhaz', 'underscore', 'ace/handlebars'], function(
  ich,
  _,
  Handlebars
) {
  'use strict'

  // The module to be exported
  var helper,
    helpers = {
      fileSize: function(item) {
        var bytes = parseInt(item, 10)
        if (isNaN(bytes)) {
          return item
        }
        var size,
          index,
          type = ['bytes', 'KB', 'MB', 'GB', 'TB']
        if (bytes === 0) {
          return '0 bytes'
        } else {
          index = Math.floor(Math.log(bytes) / Math.log(1000))
          if (index > 4) {
            index = 4
          }

          size = (bytes / Math.pow(1000, index)).toFixed(index < 2 ? 0 : 1)
        }
        return size + ' ' + type[index]
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
      isUrl: function(value, options) {
        if (value && value !== '') {
          var protocol = value.toLowerCase().split('/')[0]
          if (protocol && (protocol === 'http:' || protocol === 'https:')) {
            return options.fn(this)
          }
        }
        return options.inverse(this)
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
        if (str && typeof str === 'string') {
          return str
            .split('-')
            .join(' ')
            .replace(/\w\S*/g, function(word) {
              return word.charAt(0).toUpperCase() + word.substr(1)
            })
        }
      },
      safeString: function(str) {
        if (str && typeof str === 'string') {
          return new Handlebars.SafeString(str)
        }
      },
    }

  // Export helpers
  for (helper in helpers) {
    if (helpers.hasOwnProperty(helper)) {
      ich.addHelper(helper, helpers[helper])
    }
  }
})
