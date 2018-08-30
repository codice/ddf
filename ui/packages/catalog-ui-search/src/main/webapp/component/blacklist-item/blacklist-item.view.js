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
/*global require, setTimeout*/
var Marionette = require('marionette')
var template = require('./blacklist-item.hbs')
var CustomElements = require('js/CustomElements')
var user = require('component/singletons/user-instance')
var wreqr = require('wreqr')

module.exports = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('blacklist-item'),
  events: {
    'click .item-show': 'removeFromBlacklist',
    'click .item-details': 'navigateToItem',
  },
  removeFromBlacklist: function() {
    this.$el.toggleClass('is-destroyed', true)
    setTimeout(
      function() {
        user
          .get('user')
          .get('preferences')
          .get('resultBlacklist')
          .remove(this.model.id)
        user
          .get('user')
          .get('preferences')
          .savePreferences()
      }.bind(this),
      250
    )
  },
  navigateToItem: function() {
    wreqr.vent.trigger('router:navigate', {
      fragment: 'metacards/' + this.model.id,
      options: {
        trigger: true,
      },
    })
  },
})
