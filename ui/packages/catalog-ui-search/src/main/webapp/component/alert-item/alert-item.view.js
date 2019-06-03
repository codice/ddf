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

const wreqr = require('../../js/wreqr.js')
const Marionette = require('marionette')
const template = require('./alert-item.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const Common = require('../../js/Common.js')
const user = require('../singletons/user-instance.js')

module.exports = Marionette.ItemView.extend({
  template,
  tagName: CustomElements.register('alert-item'),
  modelEvents: {},
  events: {
    'click .alert-details': 'expandAlert',
    'click .alert-delete': 'removeModel',
  },
  initialize() {
    const modelJSON = this.model.toJSON()
    this.listenTo(store.get('workspaces'), 'remove', this.render)
    const workspace = store
      .get('workspaces')
      .filter(workspace => workspace.get('queries').get(modelJSON.queryId))[0]
    let query
    if (workspace) {
      query = workspace.get('queries').get(modelJSON.queryId)
      this.listenTo(workspace, 'change', this.render)
      this.listenTo(workspace, 'destroy', this.render)
    }
    if (query) {
      this.listenTo(query, 'change', this.render)
    }
  },
  removeModel() {
    this.$el.toggleClass('is-destroyed', true)
    setTimeout(() => {
      this.model.collection.remove(this.model)
      user
        .get('user')
        .get('preferences')
        .savePreferences()
    }, 250)
  },
  expandAlert() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    this.$el.trigger('closeSlideout.' + CustomElements.getNamespace())
    wreqr.vent.trigger('router:navigate', {
      fragment: 'alerts/' + this.model.id,
      options: {
        trigger: true,
      },
    })
  },
  serializeData() {
    const modelJSON = this.model.toJSON()
    const workspace = store
      .get('workspaces')
      .filter(workspace => workspace.get('queries').get(modelJSON.queryId))[0]
    let query
    if (workspace) {
      query = workspace.get('queries').get(modelJSON.queryId)
    }
    return {
      amount: modelJSON.metacardIds.length,
      when: Common.getMomentDate(modelJSON.when),
      queryName: query ? query.get('title') : 'Unknown Search',
      workspaceName: workspace ? workspace.get('title') : 'Unknown Workspace',
    }
  },
})
