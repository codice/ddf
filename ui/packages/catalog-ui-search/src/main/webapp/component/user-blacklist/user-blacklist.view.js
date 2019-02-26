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
/*global require*/
import React from 'react'

var Marionette = require('marionette')
var CustomElements = require('../../js/CustomElements.js')
var BlacklistItemCollection = require('../blacklist-item/blacklist-item.collection.view.js')
var user = require('../singletons/user-instance.js')

import UserBlacklist from '../../../react-component/container/user-blacklist'

const UserBlacklistView = Marionette.LayoutView.extend({
  template() {
    return <UserBlacklist />
  },
})

function getBlacklist() {
  return user
    .get('user')
    .get('preferences')
    .get('resultBlacklist')
}

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('user-blacklist'),
  componentToShow: UserBlacklistView,
  regions: {
    blacklistResults: '.blacklist-results',
  },
  events: {
    'click > .blacklist-clear': 'clearBlacklist',
  },
  initialize: function() {
    this.listenTo(getBlacklist(), 'add remove reset update', this.handleEmpty)
    this.handleEmpty()
  },
  onBeforeShow: function() {
    this.blacklistResults.show(
      new BlacklistItemCollection({
        collection: user
          .get('user')
          .get('preferences')
          .get('resultBlacklist'),
      })
    )
  },
  clearBlacklist: function() {
    this.blacklistResults.currentView.children.forEach(function(view) {
      view.removeFromBlacklist()
    })
  },
  handleEmpty: function() {
    this.$el.toggleClass('is-empty', getBlacklist().length === 0)
  },
})
