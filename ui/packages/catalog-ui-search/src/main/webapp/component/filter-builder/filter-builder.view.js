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
/*global define, alert, setTimeout*/
var Marionette = require('marionette')
var Backbone = require('backbone')
var $ = require('jquery')
var _ = require('underscore')
var template = require('./filter-builder.hbs')
var CustomElements = require('../../js/CustomElements.js')
var FilterBuilderModel = require('./filter-builder')
var FilterModel = require('../filter/filter.js')
var FilterCollectionView = require('../filter/filter.collection.view.js')
var DropdownModel = require('../dropdown/dropdown.js')
var FilterView = require('../filter/filter.view.js')
var cql = require('../../js/cql.js')
var DropdownView = require('../dropdown/dropdown.view.js')
var CQLUtils = require('../../js/CQLUtils.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('filter-builder'),
  attributes: function() {
    return { 'data-id': this.model.cid }
  },
  events: {
    'click > .filter-header > .contents-buttons .getValue': 'printValue',
    'click > .filter-header > .filter-remove': 'delete',
    'click > .filter-header > .contents-buttons .add-filter': 'addFilter',
    'click > .filter-header > .contents-buttons .add-filterBuilder':
      'addFilterBuilder',
  },
  modelEvents: {},
  regions: {
    filterOperator: '.filter-operator',
    filterContents: '.contents-filters',
  },
  initialize: function() {
    this.listenTo(this.model, 'change:operator', this.updateOperatorDropdown)
    if (this.options.isForm === true) {
      if (this.options.isFormBuilder !== true) {
        this.turnOffFieldAdditions()
      }
      this.turnOffNesting()
      this.turnOffRootOperator()
    }
  },
  onBeforeShow: function() {
    this.$el.toggleClass('is-sortable', this.options.isSortable || false)
    this.filterOperator.show(
      DropdownView.createSimpleDropdown({
        list: [
          {
            label: 'AND',
            value: 'AND',
          },
          {
            label: 'OR',
            value: 'OR',
          },
          {
            label: 'NOT AND',
            value: 'NOT AND',
          },
          {
            label: 'NOT OR',
            value: 'NOT OR',
          },
        ],
        defaultSelection: ['AND'],
      })
    )
    this.listenTo(
      this.filterOperator.currentView.model,
      'change:value',
      this.handleOperatorUpdate
    )
    this.filterContents.show(
      new FilterCollectionView({
        collection: new Backbone.Collection([this.createFilterModel()], {
          comparator: 'sortableOrder',
        }),
        'filter-builder': this,
        isForm: this.options.isForm || false,
        isFormBuilder: this.options.isFormBuilder || false,
      })
    )
  },
  updateOperatorDropdown: function() {
    this.filterOperator.currentView.model.set('value', [
      this.model.get('operator'),
    ])
  },
  handleOperatorUpdate: function() {
    this.model.set(
      'operator',
      this.filterOperator.currentView.model.get('value')[0]
    )
  },
  delete: function() {
    this.model.destroy()
  },
  addFilter: function() {
    var FilterView = this.filterContents.currentView.addFilter(
      this.createFilterModel()
    )
    this.handleEditing()
    return FilterView
  },
  addFilterBuilder: function() {
    const numFilters = this.filterContents.currentView
      ? this.filterContents.currentView.collection.length
      : 0
    var FilterBuilderView = this.filterContents.currentView.addFilterBuilder(
      new FilterBuilderModel({ sortableOrder: numFilters + 1 })
    )
    this.handleEditing()
    return FilterBuilderView
  },
  filterView: FilterView,
  printValue: function() {
    alert(this.transformToCql())
  },
  getValue: function() {
    var operator = this.model.get('operator')
    var text = '('
    this.filterContents.currentView.children.forEach(function(
      childView,
      index
    ) {
      if (index > 0) {
        if (operator === 'NONE') {
          text += ' AND NOT '
        } else {
          text += ' ' + operator + ' '
        }
      } else if (operator === 'NONE') {
        text += ' NOT '
      }
      text += childView.getValue()
    })
    text += ')'
    return text
  },
  transformToCql: function() {
    this.deleteInvalidFilters()
    var filter = this.getFilters()
    if (filter.filters.length === 0) {
      return '("anyText" ILIKE \'%\')'
    } else {
      return CQLUtils.transformFilterToCQL(filter)
    }
  },
  getFilters: function() {
    var operator = this.model.get('operator')
    if (operator === 'NONE') {
      return {
        type: 'NOT',
        filters: [
          {
            type: 'AND',
            filters: this.filterContents.currentView.children.map(function(
              childView
            ) {
              return childView.getFilters()
            }),
          },
        ],
      }
    } else {
      return {
        type: operator,
        filters: this.filterContents.currentView.children
          .map(function(childView) {
            return childView.getFilters()
          })
          .filter(function(filter) {
            return filter
          }),
      }
    }
  },
  deleteInvalidFilters: function() {
    this.filterContents.currentView.children.forEach(function(childView) {
      if(childView.model.attributes.comparator === 'EMPTY'){
        return false
      }
      childView.deleteInvalidFilters()
    })
    if (this.filterContents.currentView.children.length === 0) {
      this.delete()
    }
  },
  setFilters: function(filters) {
    setTimeout(
      function() {
        if (this.filterContents) {
          this.filterContents.currentView.collection.reset()
          filters.forEach(
            function(filter) {
              if (filter.filters) {
                var filterBuilderView = this.addFilterBuilder()
                filterBuilderView.setFilters(filter.filters)
                filterBuilderView.model.set('operator', filter.type)
              } else {
                var filterView = this.addFilter()
                filterView.setFilter(filter)
              }
            }.bind(this)
          )
          this.handleEditing()
        }
      }.bind(this),
      0
    )
  },
  revert: function() {
    this.$el.removeClass('is-editing')
  },
  serializeData: function() {
    return {
      cql: 'anyText ILIKE ""',
    }
  },
  deserialize: function(cql) {
    if (!cql.filters) {
      cql = {
        filters: [cql],
        type: 'AND',
      }
    }
    this.model.set('operator', cql.type)
    this.setFilters(cql.filters)
  },
  handleEditing: function() {
    var isEditing = this.$el.hasClass('is-editing')
    if (isEditing) {
      this.turnOnEditing()
    } else {
      this.turnOffEditing()
    }
  },
  sortCollection: function() {
    this.filterContents.currentView.collection.sort()
  },
  turnOnEditing: function() {
    this.$el.addClass('is-editing')
    this.filterOperator.currentView.turnOnEditing()
    this.filterContents.currentView.turnOnEditing()
  },
  turnOffEditing: function() {
    this.$el.removeClass('is-editing')
    this.filterOperator.currentView.turnOffEditing()
    this.filterContents.currentView.turnOffEditing()
  },
  turnOffNesting: function() {
    this.$el.addClass('hide-nesting')
  },
  turnOffRootOperator: function() {
    this.$el.addClass('hide-root-operator')
  },
  turnOffFieldAdditions: function() {
    this.$el.addClass('hide-field-button')
  },
  createFilterModel: function() {
    const numFilters = this.filterContents.currentView
      ? this.filterContents.currentView.collection.length
      : 0
    return new FilterModel({
      sortableOrder: numFilters + 1,
      isResultFilter: Boolean(this.model.get('isResultFilter')),
    })
  },
})
