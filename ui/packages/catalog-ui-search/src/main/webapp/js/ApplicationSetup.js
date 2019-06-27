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

;(function injectStyles() {
  if (process.env.NODE_ENV !== 'production') {
    require('../dev/styles/styles.less') // include styles for dev guide components
  } else {
    require('../styles/styles.less') // production styles only
  }
})()

const $ = require('jquery')
$.ajaxSetup({
  cache: false,
  headers: {
    'X-Requested-With': 'XMLHttpRequest',
  },
})

if (process.env.NODE_ENV !== 'production') {
  $('html').addClass('is-development')
  if (module.hot) {
    require('react-hot-loader')
    $('html').addClass('is-hot-reloading')
  }
}

window.CESIUM_BASE_URL = './cesium/assets'

const Backbone = require('backbone')
const Marionette = require('marionette')
const properties = require('./properties.js')
const announcement = require('../component/announcement/index.jsx')
require('./Marionette.Region.js')
require('./requestAnimationFramePolyfill.js')
require('./HandlebarsHelpers.js')
require('./ApplicationHelpers.js')
require('./Autocomplete.js')
require('./backbone.customFunctions.js')
require('./extensions/backbone.listenTo.tsx')
require('./extensions/marionette.onFirstRender.js')
require('./extensions/marionette.renderer.render.js')
require('./extensions/marionette.ItemView.attachElContent.js')
require('./extensions/marionette.View.isMarionetteComponent.js')
require('./extensions/marionette.View.remove.js')

let getShortErrorMessage = function(error) {
  let extraMessage = error instanceof Error ? error.name : String(error)

  if (extraMessage.length === 0) {
    return extraMessage
  }

  // limit to 20 characters so we do not worry about overflow
  if (extraMessage.length > 20) {
    extraMessage = extraMessage.substr(0, 20) + '...'
  }

  return ' - ' + extraMessage
}

let getErrorResponse = function(event, jqxhr, settings, throwError) {
  if (
    jqxhr.getResponseHeader('content-type') === 'application/json' &&
    jqxhr.responseText.startsWith('<') &&
    jqxhr.responseText.indexOf('ACSURL') > -1 &&
    jqxhr.responseText.indexOf('SAMLRequest') > -1
  ) {
    return { title: 'Logged out', message: 'Please refresh page to log in' }
  } else if (
    settings.url.indexOf('./internal/catalog/sources') > -1 &&
    settings.type === 'GET'
  ) {
    return {
      title: properties.i18n['sources.polling.error.title'],
      message: properties.i18n['sources.polling.error.message'],
    }
  } else if (
    settings.url.indexOf('./internal/workspaces') > -1 &&
    settings.type === 'PUT'
  ) {
    return {
      title: 'Error Saving Workspace',
      message: 'Unable to save workspace on server',
    }
  } else if (jqxhr.responseJSON !== undefined) {
    return { title: 'Error', message: jqxhr.responseJSON.message }
  } else {
    switch (jqxhr.status) {
      case 403:
        return { title: 'Forbidden', message: 'Not Authorized' }
      case 405:
        return {
          title: 'Error',
          message: 'Method not allowed. Please try refreshing your browser',
        }
      default:
        return {
          title: 'Error',
          message: 'Unknown Error' + getShortErrorMessage(throwError),
        }
    }
  }
}

$(window.document).ajaxError((event, jqxhr, settings, throwError) => {
  if (settings.customErrorHandling) {
    // Do nothing if caller is handling their own error
    return
  }

  console.error(event, jqxhr, settings, throwError)
  const response = getErrorResponse(event, jqxhr, settings, throwError)

  if (
    properties.disableUnknownErrorBox &&
    response.message.substring(0, 13) === 'Unknown Error'
  ) {
    return
  }

  announcement.announce({
    title: response.title,
    message: response.message,
    type: 'error',
  })
})

//in here we drop in any top level patches, etc.
const toJSON = Backbone.Model.prototype.toJSON
Backbone.Model.prototype.toJSON = function(options) {
  const originalJSON = toJSON.call(this, options)
  if (options && options.additionalProperties !== undefined) {
    const backboneModel = this
    options.additionalProperties.forEach(property => {
      originalJSON[property] = backboneModel[property]
    })
  }
  return originalJSON
}
const clone = Backbone.Model.prototype.clone
Backbone.Model.prototype.clone = function() {
  const cloneRef = clone.call(this)
  cloneRef._cloneOf = this.id || this.cid
  return cloneRef
}
const associationsClone = Backbone.AssociatedModel.prototype.clone
Backbone.AssociatedModel.prototype.clone = function() {
  const cloneRef = associationsClone.call(this)
  cloneRef._cloneOf = this.id || this.cid
  return cloneRef
}
const associationsSet = Backbone.AssociatedModel.prototype.set
Backbone.AssociatedModel.prototype.set = function(key, value, options) {
  if (typeof key === 'object') {
    options = value
  }
  if (options && options.withoutSet === true) {
    return this
  }
  return associationsSet.apply(this, arguments)
}

// https://github.com/marionettejs/backbone.marionette/issues/3077
// monkey-patch Marionette for compatibility with jquery 3+.
// jquery removed the .selector method, which was used by the original
// implementation here.
Marionette.Region.prototype.reset = function() {
  this.empty()
  this.el = this.options.el
  delete this.$el
  return this
}

require('@connexta/icons/icons/codice.font')
