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

const template = require('./query-status-row.hbs');
const Marionette = require('marionette');
const CustomElements = require('../../js/CustomElements.js');
const user = require('../singletons/user-instance.js');

module.exports = Marionette.ItemView.extend({
  className: 'is-tr',
  tagName: CustomElements.register('query-status-row'),
  template: template,
  events: {
    'click button': 'triggerFilter',
  },
  modelEvents: {
    change: 'render',
  },
  triggerFilter: function() {
    user
      .get('user')
      .get('preferences')
      .set('resultFilter', '("source-id" = \'' + this.model.id + "')")
    user
      .get('user')
      .get('preferences')
      .savePreferences()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  serializeData: function() {
    const modelJSON = this.model.toJSON();
    modelJSON.fromremote = modelJSON.top - modelJSON.fromcache
    modelJSON.elapsed = modelJSON.elapsed / 1000
    modelJSON.anyHasReturned =
      modelJSON.hasReturned || modelJSON.cacheHasReturned
    modelJSON.anyHasNotReturned =
      !modelJSON.hasReturned || !modelJSON.cacheHasReturned
    return modelJSON
  },
})
