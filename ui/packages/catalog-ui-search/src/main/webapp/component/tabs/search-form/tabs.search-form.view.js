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

const Marionette = require('marionette')
const TabsView = require('../tabs.view')
const SearchFormTabsModel = require('./tabs.search-form')
const store = require('../../../js/store.js')
const Query = require('../../../js/model/Query.js')

module.exports = TabsView.extend({
  selectionInterface: store,
  setDefaultModel: function() {
    this.model = new SearchFormTabsModel()
  },
  initialize: function() {
    this.setDefaultModel()
    TabsView.prototype.initialize.call(this)
  },
  determineContent: function() {
    var activeTab = this.model.getActiveView()
    this.tabsContent.show(
      new activeTab({
        model: this.options.queryModel || new Query.Model(),
      })
    )
  },
})
