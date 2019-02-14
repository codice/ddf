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
/*global define, alert*/
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const template = require('./paging.hbs')
const _debounce = require('lodash/debounce')

module.exports = Marionette.ItemView.extend({
  tagName: CustomElements.register('paging'),
  template: template,
  initialize: function(options) {
    this.listenTo(this.model, 'reset', this.render)
    this.listenTo(this.getQuery().get('result'), 'change', this.render)
    this.listenTo(
      this.model.fullCollection,
      'add remove update',
      this.updateSelectionInterfaceComplete
    )
    this.updateSelectionInterface = _debounce(
      this.updateSelectionInterface,
      200,
      { leading: true, trailing: true }
    )
    this.updateSelectionInterfaceComplete = _debounce(
      this.updateSelectionInterfaceComplete,
      200,
      { leading: true, trailing: true }
    )
    this.updateSelectionInterfaceComplete()
  },
  updateSelectionInterfaceComplete: function() {
    this.options.selectionInterface.setCompleteActiveSearchResults(
      this.model.fullCollection.reduce(function(results, result) {
        results.push(result)
        if (result.duplicates) {
          results = results.concat(result.duplicates)
        }
        return results
      }, [])
    )
  },
  updateSelectionInterface: function() {
    this.options.selectionInterface.setActiveSearchResults(
      this.model.reduce(function(results, result) {
        results.push(result)
        if (result.duplicates) {
          results = results.concat(result.duplicates)
        }
        return results
      }, [])
    )
  },
  events: {
    'click .first': 'firstPage',
    'click .previous': 'previousPage',
    'click .next': 'nextPage',
    'click .last': 'lastPage',
    'click .server-previous': 'previousServerPage',
    'click .server-next': 'nextServerPage',
  },
  firstPage: function() {
    this.model.getFirstPage()
  },
  previousPage: function() {
    this.model.getPreviousPage()
    this.updateResultsRange()
  },
  nextPage: function() {
    this.model.getNextPage()
    this.updateResultsRange()
  },
  lastPage: function() {
    this.model.getLastPage()
  },
  previousServerPage: function() {
    this.getQuery().getPreviousServerPage()
  },
  nextServerPage: function() {
    this.getQuery().getNextServerPage()
  },
  onRender: function() {
    this.updateSelectionInterface()
  },
  serializeData: function() {
    var query = this.getQuery()
    var resultsCollection = this.model
    return {
      pages: query.getResultsRangeLabel(this.model),
      hasPreviousPage: resultsCollection.hasPreviousPage(),
      hasNextPage: resultsCollection.hasNextPage(),
      showNextServerPage:
        !resultsCollection.hasNextPage() && query.hasNextServerPage(),
      showPreviousServerPage:
        !resultsCollection.hasPreviousPage() && query.hasPreviousServerPage(),
    }
  },
  getQuery: function() {
    return this.options.selectionInterface.getCurrentQuery()
  },
  updateResultsRange: function() {
    this.$('.status').text(this.getQuery().getResultsRangeLabel(this.model))
  },
})
