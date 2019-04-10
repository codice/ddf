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
/*global define*/
import * as React from 'react'
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const Common = require('../../js/Common.js')
const PagingView = require('../paging/paging.view.js')
const DropdownView = require('../dropdown/result-display/dropdown.result-display.view.js')
const ResultFilterDropdownView = require('../dropdown/result-filter/dropdown.result-filter.view.js')
const DropdownModel = require('../dropdown/dropdown.js')
const cql = require('../../js/cql.js')
const ResultSortDropdownView = require('../dropdown/result-sort/dropdown.result-sort.view.js')
const user = require('../singletons/user-instance.js')
const ResultStatusView = require('../result-status/result-status.view.js')
require('../../behaviors/selection.behavior.js')
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import ResultItemCollection from '../result-item/result-item.collection'
const {
  SelectAllToggle,
} = require('../selection-checkbox/selection-checkbox.view.js')

function mixinBlackListCQL(originalCQL) {
  var blackListCQL = {
    filters: [
      {
        filters: user
          .get('user')
          .get('preferences')
          .get('resultBlacklist')
          .map(function(blacklistItem) {
            return {
              property: '"id"',
              type: '!=',
              value: blacklistItem.id,
            }
          }),
        type: 'AND',
      },
    ],
    type: 'AND',
  }
  if (originalCQL) {
    blackListCQL.filters.push(originalCQL)
  }
  return blackListCQL
}

var ResultSelector = Marionette.LayoutView.extend({
  template() {
    var resultFilter = user
      .get('user')
      .get('preferences')
      .get('resultFilter')
    if (resultFilter) {
      resultFilter = cql.simplify(cql.read(resultFilter))
    }
    resultFilter = mixinBlackListCQL(resultFilter)
    var filteredResults = this.model
      .get('result')
      .get('results')
      .generateFilteredVersion(resultFilter)
    var collapsedResults = filteredResults.collapseDuplicates()
    collapsedResults.updateSorting(
      user
        .get('user')
        .get('preferences')
        .get('resultSort')
    )
    this.handleFiltering(collapsedResults)
    return (
      <React.Fragment>
        <div className="resultSelector-menu">
          <div class="checkbox-container" />
          <div className="resultSelector-menu-action menu-resultFilter" />
          <div className="resultSelector-menu-action menu-resultSort" />
          <div className="resultSelector-menu-action menu-resultDisplay" />
        </div>
        <MarionetteRegionContainer
          className="resultSelector-status"
          view={ResultStatusView}
          key={Math.random()}
          viewOptions={{ model: collapsedResults }}
        />
        <div className="resultSelector-status" />
        <div className="resultSelector-new">
          <button className="is-positive merge">
            <span>Update with new data?</span>
          </button>
          <button className="is-negative ignore">
            <span className="fa fa-times" />
          </button>
        </div>
        <div className="resultSelector-searching is-critical-animation">
          <div className="searching-bit is-critical-animation" />
          <div className="searching-bit" />
          <div className="searching-bit" />
          <div className="searching-bit" />
          <div className="searching-bit" />
        </div>
        <ResultItemCollection
          className="resultSelector-list"
          key={Math.random()}
          results={collapsedResults}
          selectionInterface={this.options.selectionInterface}
        />
        <MarionetteRegionContainer
          key={Math.random()}
          className="resultSelector-paging"
          view={PagingView}
          viewOptions={{
            model: collapsedResults,
            selectionInterface: this.options.selectionInterface,
          }}
        />
      </React.Fragment>
    )
  },
  tagName: CustomElements.register('result-selector'),
  events: {
    'click > .resultSelector-new .merge': 'mergeNewResults',
    'click > .resultSelector-new .ignore': 'ignoreNewResults',
  },
  behaviors: function() {
    return {
      selection: {
        selectionInterface: this.options.selectionInterface,
        selectionSelector: `${CustomElements.getNamespace()}result-item`,
      },
    }
  },
  regions: {
    resultDisplay: '.menu-resultDisplay',
    resultFilter: '.menu-resultFilter',
    resultSort: '.menu-resultSort',
    checkboxContainer: '.checkbox-container',
  },
  initialize: function(options) {
    if (!this.model.get('result')) {
      if (options.tieredSearchIds) {
        this.model.startTieredSearch(options.tieredSearchIds)
      } else {
        this.model.startSearch()
      }
    }
    this.model.get('result').set('currentlyViewed', true)
    this.options.selectionInterface.setCurrentQuery(this.model)
    this.startListeningToFilter()
    this.startListeningToSort()
    this.startListeningToResult()
    this.startListeningToMerged()
    this.startListeningToStatus()
  },
  mergeNewResults: function() {
    this.model.get('result').mergeNewResults()
  },
  ignoreNewResults: function() {
    this.$el.toggleClass('ignore-new', true)
  },
  handleMerged: function() {
    this.$el.toggleClass('ignore-new', false)
    this.$el.toggleClass('has-unmerged', this.model.get('result').isUnmerged())
  },
  startListeningToMerged: function() {
    this.listenTo(this.model.get('result'), 'change:merged', this.handleMerged)
  },
  handleStatus: function() {
    this.$el.toggleClass('is-searching', this.model.get('result').isSearching())
  },
  startListeningToStatus: function() {
    this.listenTo(
      this.model.get('result'),
      'sync request error',
      this.handleStatus
    )
  },
  startListeningToBlacklist: function() {
    this.listenTo(
      user
        .get('user')
        .get('preferences')
        .get('resultBlacklist'),
      'add remove update reset',
      this.render
    )
  },
  startListeningToResult: function() {
    this.listenTo(this.model.get('result'), 'reset:results', this.render)
  },
  startListeningToFilter: function() {
    this.listenTo(
      user.get('user').get('preferences'),
      'change:resultFilter',
      this.render
    )
  },
  startListeningToSort: function() {
    this.listenTo(
      user.get('user').get('preferences'),
      'change:resultSort',
      this.render
    )
  },
  scrollIntoView: function(metacard) {
    var result = this.$el.find(
      '.resultSelector-list ' +
        resultItemSelector +
        '[data-resultid="' +
        metacard.id +
        metacard.get('properties>source-id') +
        '"]'
    )
    if (result && result.length > 0) {
      //result[0].scrollIntoView();
    }
  },
  onRender() {
    this.showResultDisplayDropdown()
    this.showResultFilterDropdown()
    this.showResultSortDropdown()
    this.showCheckbox()
    this.handleMerged()
    this.handleStatus()
    let resultCountOnly =
      this.model.get('result').get('resultCountOnly') === true
    this.regionManager.forEach(region => {
      region.currentView.$el.toggleClass('is-hidden', resultCountOnly)
    })
  },
  handleFiltering: function(resultCollection) {
    this.$el.toggleClass('has-filter', resultCollection.amountFiltered !== 0)
  },
  showResultFilterDropdown: function() {
    this.resultFilter.show(
      new ResultFilterDropdownView({
        model: new DropdownModel(),
      })
    )
  },
  showResultSortDropdown: function() {
    this.resultSort.show(
      new ResultSortDropdownView({
        model: new DropdownModel(),
      })
    )
  },
  showCheckbox: function() {
    this.checkboxContainer.show(
      new SelectAllToggle({
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
  showResultDisplayDropdown: function() {
    this.resultDisplay.show(
      DropdownView.createSimpleDropdown({
        list: [
          {
            label: 'List',
            value: 'List',
          },
          {
            label: 'Gallery',
            value: 'Grid',
          },
        ],
        defaultSelection: [
          user
            .get('user')
            .get('preferences')
            .get('resultDisplay'),
        ],
      })
    )
    this.stopListening(this.resultDisplay.currentView.model)
    this.listenTo(
      this.resultDisplay.currentView.model,
      'change:value',
      function() {
        var prefs = user.get('user').get('preferences')
        var value = this.resultDisplay.currentView.model.get('value')[0]
        prefs.set('resultDisplay', value)
        prefs.savePreferences()
      }
    )
  },
  onDestroy: function() {
    Common.queueExecution(
      function() {
        if (this.model.get('result')) {
          this.model.get('result').set('currentlyViewed', false)
        }
      }.bind(this)
    )
  },
})

module.exports = ResultSelector
