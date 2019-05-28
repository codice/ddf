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
const Backbone = require('backbone')
const properties = require('../properties.js')
require('backbone-associations')

module.exports = Backbone.AssociatedModel.extend({
  defaults() {
    return {
      count: 0,
      elapsed: 0,
      hits: 0,
      id: 'undefined',
      successful: undefined,
      top: 0,
      fromcache: 0,
      cacheHasReturned: properties.isCacheDisabled,
      cacheSuccessful: true,
      cacheMessages: [],
      hasReturned: false,
      messages: [],
    }
  },
  initialize() {
    if (this.get('successful') !== undefined) {
      this.set('hasReturned', true)
    } else {
      this.listenToOnce(this, 'change:successful', this.setHasReturned)
    }
  },
  setHasReturned() {
    this.set('hasReturned', true)
  },
  setCacheHasReturned() {
    this.set('cacheHasReturned', true)
  },
  updateMessages(messages, id, status) {
    if (this.id === id) {
      this.set('messages', messages)
    }
    if (id === 'cache') {
      this.set({
        cacheHasReturned: true,
        cacheSuccessful: status ? status.successful : false,
        cacheMessages: messages,
      })
    }
  },
  updateStatus(results) {
    let top = 0
    let fromcache = 0
    results.forEach(result => {
      if (
        result
          .get('metacard')
          .get('properties')
          .get('source-id') === this.id
      ) {
        top++
        if (!result.get('uncached')) {
          fromcache++
        }
      }
    })
    this.set({
      top,
      fromcache,
    })
  },
})
