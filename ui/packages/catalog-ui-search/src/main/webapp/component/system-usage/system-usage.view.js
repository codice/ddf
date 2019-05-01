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

var Marionette = require('marionette')
var template = require('./system-usage.hbs')
var CustomElements = require('../../js/CustomElements.js')
var properties = require('../../js/properties.js')
var user = require('../singletons/user-instance.js')
var preferences = user.get('user').get('preferences')
var $ = require('jquery')

function getSrc() {
  return (
    '<html class="is-iframe" style="font-size: ' +
    preferences.get('fontSize') +
    'px">' +
    '<link href="styles.' +
    document.querySelector('link[href*="styles."]').href.split('styles.')[1] +
    '" rel="stylesheet">' +
    properties.ui.systemUsageMessage +
    '</html>'
  )
}

function populateIframe(view) {
  var $iframe = view.$el.find('iframe')
  $iframe.ready(function() {
    $iframe.contents()[0].open()
    $iframe.contents()[0].write(getSrc())
    $iframe.contents()[0].close()
  })
}

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('system-usage'),
  events: {
    'click button': 'handleClick',
  },
  initialize: function() {},
  serializeData: function() {
    return {
      fontSize: preferences.get('fontSize'),
      properties: properties,
    }
  },
  handleClick: function() {
    if (
      !user.get('user').isGuestUser() &&
      properties.ui.systemUsageOncePerSession
    ) {
      var systemUsage = JSON.parse(window.sessionStorage.getItem('systemUsage'))
      systemUsage[user.get('user').get('username')] = 'true'
      window.sessionStorage.setItem('systemUsage', JSON.stringify(systemUsage))
    }
    this.$el.trigger(CustomElements.getNamespace() + 'close-lightbox')
  },
  onAttach: function() {
    if (user.fetched) {
      populateIframe(this)
    } else {
      user.once(
        'sync',
        function() {
          populateIframe(this)
        }.bind(this)
      )
    }
  },
})
