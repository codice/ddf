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

const TabsView = require('../tabs.view')
const QueryTabsModel = require('./tabs-query')
const store = require('../../../js/store.js')

const QueryTabsView = TabsView.extend({
  className: 'is-query',
  setDefaultModel() {
    this.model = new QueryTabsModel()
  },
  initialize(options) {
    if (options.model === undefined) {
      this.setDefaultModel()
    }
    this.listenTo(store.get('content'), 'change:query', this.handleQuery)
    this.determineAvailableContent()
    TabsView.prototype.initialize.call(this)
  },
  handleQuery() {
    this.determineAvailableContent()
    this.determineContent()
  },
  determineTabForExistingQuery() {
    const activeTab = this.model.getActiveView()
    this.tabsContent.show(
      new activeTab({
        model: this.model.getAssociatedQuery(),
      })
    )
  },
  determineTabForNewQuery() {
    const activeTabName = this.model.get('activeTab')
    if (activeTabName !== 'Search') {
      this.model.set('activeTab', 'Search')
    }
    this.determineTabForExistingQuery()
  },
  determineContent() {
    const currentQuery = store.get('content').get('query')
    if (currentQuery) {
      if (currentQuery._cloneOf) {
        this.determineTabForExistingQuery()
      } else {
        this.determineTabForNewQuery()
      }
    }
  },
  determineAvailableContent() {
    const currentQuery = store.get('content').get('query')
    this.$el.toggleClass('is-new', currentQuery && !currentQuery._cloneOf)
  },
})

module.exports = QueryTabsView
