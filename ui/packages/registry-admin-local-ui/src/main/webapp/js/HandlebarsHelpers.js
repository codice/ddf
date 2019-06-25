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

define(['icanhaz', 'underscore', 'handlebars'], function(ich, _, Handlebars) {
  'use strict'

  // The module to be exported
  var helper,
    helpers = {
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
        if (value && value !== '' && _.isString(value)) {
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
      debug: function() {
        console.log('Current Context')
        console.log('====================')
        console.log(this)
      },
    }

  // Export helpers
  for (helper in helpers) {
    if (helpers.hasOwnProperty(helper)) {
      ich.addHelper(helper, helpers[helper])
    }
  }
})
