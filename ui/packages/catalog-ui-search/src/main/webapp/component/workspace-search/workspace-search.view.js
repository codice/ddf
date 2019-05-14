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
const CustomElements = require('../../js/CustomElements.js')
const template = require('./workspace-search.hbs')
const ResultsView = require('../results/results.view.js')
const SearchesView = require('../workspace-explore/workspace-explore.view.js')
const store = require('../../js/store.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('workspace-search'),
  regions: {
    searchResults: '> .search-results',
  },
  onBeforeShow: function() {
    if (store.getCurrentWorkspace()) {
      this.setupSearchResults()
    }
  },
  setupSearchResults: function() {
    this.searchResults.show(
      new ResultsView({
        selectionInterface: store,
      })
    )
  },
})
