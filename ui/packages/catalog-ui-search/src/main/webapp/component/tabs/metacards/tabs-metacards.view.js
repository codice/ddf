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

const _ = require('underscore')
const TabsView = require('../tabs.view')
const MetacardsTabsModel = require('./tabs-metacards')
const store = require('../../../js/store.js')
const properties = require('../../../js/properties.js')

function getTypes(results) {
  const types = {}
  results.forEach(result => {
    const tags = result
      .get('metacard')
      .get('properties')
      .get('metacard-tags')
    if (result.isWorkspace()) {
      types.workspace = true
    } else if (result.isResource()) {
      types.resource = true
    } else if (result.isRevision()) {
      types.revision = true
    } else if (result.isDeleted()) {
      types.deleted = true
    }
    if (result.isRemote()) {
      types.remote = true
    }
  })
  return Object.keys(types)
}

const MetacardsTabsView = TabsView.extend({
  className: 'is-metacards',
  setDefaultModel() {
    this.model = new MetacardsTabsModel()
  },
  selectionInterface: store,
  initialize(options) {
    this.selectionInterface = options.selectionInterface || store
    if (options.model === undefined) {
      this.setDefaultModel()
    }
    this.determineAvailableContent()
    TabsView.prototype.initialize.call(this)
    const debounceDetermineContent = _.debounce(this.handleMetacardChange, 200)
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'update',
      debounceDetermineContent
    )
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'add',
      debounceDetermineContent
    )
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'remove',
      debounceDetermineContent
    )
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'reset',
      debounceDetermineContent
    )
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'refreshdata',
      debounceDetermineContent
    )
  },
  handleMetacardChange() {
    this.determineAvailableContent()
    this.determineContent()
  },
  determineContentFromType() {
    const activeTabName = this.model.get('activeTab')
    const types = getTypes(this.selectionInterface.getSelectedResults())
    if (
      types.indexOf('revision') >= 0 &&
      ['Archive'].indexOf(activeTabName) >= 0
    ) {
      this.model.set('activeTab', 'Details')
    } else if (
      types.indexOf('deleted') >= 0 &&
      types.length > 1 &&
      ['Archive'].indexOf(activeTabName) >= 0
    ) {
      this.model.set('activeTab', 'Details')
    }
    if (
      types.indexOf('remote') >= 0 &&
      ['Archive'].indexOf(activeTabName) >= 0
    ) {
      this.model.set('activeTab', 'Details')
    }
    if (
      properties.isEditingRestricted() &&
      ['Archive'].indexOf(activeTabName) >= 0
    ) {
      this.model.set('activeTab', 'Details')
    }
    const activeTab = this.model.getActiveView()
    this.tabsContent.show(
      new activeTab({
        selectionInterface: this.selectionInterface,
      })
    )
  },
  determineContent() {
    if (this.selectionInterface.getSelectedResults().length > 1) {
      this.determineContentFromType()
    }
  },
  determineAvailableContent() {
    if (this.selectionInterface.getSelectedResults().length > 1) {
      const types = getTypes(this.selectionInterface.getSelectedResults())
      this.$el.toggleClass('is-mixed', types.length > 1)
      this.$el.toggleClass('is-workspace', types.indexOf('workspace') >= 0)
      this.$el.toggleClass('is-resource', types.indexOf('resource') >= 0)
      this.$el.toggleClass('is-revision', types.indexOf('revision') >= 0)
      this.$el.toggleClass('is-deleted', types.indexOf('deleted') >= 0)
      this.$el.toggleClass('is-remote', types.indexOf('remote') >= 0)
    }
  },
})

module.exports = MetacardsTabsView
