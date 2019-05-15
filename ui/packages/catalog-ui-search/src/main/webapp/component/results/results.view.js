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
const resultsTemplate = require('./results.hbs')
const CustomElements = require('../../js/CustomElements.js')
const QuerySelectDropdown = require('../dropdown/query-select/dropdown.query-select.view.js')
const DropdownModel = require('../dropdown/dropdown.js')
const store = require('../../js/store.js')
const ResultSelectorView = require('../result-selector/result-selector.view.js')
const WorkspaceExploreView = require('../workspace-explore/workspace-explore.view.js')

let selectedQueryId

const ResultsView = Marionette.LayoutView.extend({
  setDefaultModel: function() {
    this.model = store.getCurrentQueries()
  },
  template: resultsTemplate,
  tagName: CustomElements.register('results'),
  regions: {
    resultsEmpty: '.results-empty',
    resultsSelect: '.results-select',
    resultsList: '.results-list',
  },
  initialize: function(options) {
    if (options.model === undefined) {
      this.setDefaultModel()
    }
  },
  getPreselectedQuery: function() {
    if (this.model.length === 1) {
      return this.model.first().id
    } else if (this.model.get(store.getCurrentQuery())) {
      return store.getCurrentQuery().id
    } else if (this.model.get(selectedQueryId)) {
      return selectedQueryId
    } else {
      return undefined
    }
  },
  onBeforeShow: function() {
    this._resultsSelectDropdownModel = new DropdownModel({
      value: this.getPreselectedQuery(),
    })
    this.resultsSelect.show(
      new QuerySelectDropdown({
        model: this._resultsSelectDropdownModel,
        dropdownCompanionBehaviors: {
          navigation: {},
        },
      })
    )
    this.listenTo(
      this._resultsSelectDropdownModel,
      'change:value',
      this.updateResultsList
    )
    this.listenTo(
      store.get('content'),
      'change:currentQuery',
      this.handleCurrentQuery
    )
    this.resultsEmpty.show(new WorkspaceExploreView())
    this.updateResultsList()
    this.handleEmptyQueries()
    this.listenTo(this.model, 'add', this.handleEmptyQueries)
    this.listenTo(this.model, 'remove', this.handleEmptyQueries)
    this.listenTo(this.model, 'update', this.handleEmptyQueries)
  },
  handleCurrentQuery: function() {
    this._resultsSelectDropdownModel.set('value', store.getCurrentQuery().id)
  },
  updateResultsList: function() {
    const queryId = this._resultsSelectDropdownModel.get('value')
    if (queryId) {
      selectedQueryId = queryId
      this.resultsList.show(
        new ResultSelectorView({
          model: store.getCurrentQueries().get(queryId),
          selectionInterface: this.options.selectionInterface,
        })
      )
    } else {
      this.resultsList.empty()
    }
  },
  handleEmptyQueries: function() {
    this.$el.toggleClass('is-empty', this.model.isEmpty())
    if (this.model.length === 1) {
      this._resultsSelectDropdownModel.set('value', this.model.first().id)
    }
  },
})

module.exports = ResultsView
