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

const user = require('./user-instance.js')
const Backbone = require('backbone')

module.exports = new (Backbone.Collection.extend({
  initialize: function() {
    const uploads = user
      .get('user')
      .get('preferences')
      .get('uploads')
    const alerts = user
      .get('user')
      .get('preferences')
      .get('alerts')
    this.add(uploads.models)
    this.add(alerts.models)
    this.listenTo(uploads, 'add', this.add)
    this.listenTo(uploads, 'remove', this.remove)
    this.listenTo(alerts, 'add', this.add)
    this.listenTo(alerts, 'remove', this.remove)
  },
  comparator: function(model) {
    return -model.getTimeComparator()
  },
  hasUnseen: function() {
    return this.some(function(notification) {
      return notification.get('unseen')
    })
  },
  setSeen: function() {
    this.forEach(function(notification) {
      notification.set('unseen', false)
    })
  },
}))()
