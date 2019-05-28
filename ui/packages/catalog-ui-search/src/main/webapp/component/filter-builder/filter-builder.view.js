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
const template = require('./filter-builder.hbs')
const CustomElements = require('../../js/CustomElements.js')
const FilterCollectionView = require('../filter/filter.collection.view.js')
const FilterView = require('../filter/filter.view.js')
const cql = require('../../js/cql.js')
const DropdownView = require('../dropdown/dropdown.view.js')
const CQLUtils = require('../../js/CQLUtils.js')

import { serialize, deserialize } from './filter-serialization'

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('filter-builder'),
  events: {
    'click > .filter-header > .contents-buttons .getValue': 'printValue',
    'click > .filter-header > .filter-remove': 'delete',
    'click > .filter-header > .contents-buttons .add-filter': 'addFilter',
    'click > .filter-header > .contents-buttons .add-filterBuilder':
      'addFilterBuilder',
  },
  regions: {
    filterOperator: '.filter-operator',
    filterContents: '.contents-filters',
  },
  initialize() {
    const {
      filter,
      isResultFilter = false,
      isForm,
      isFormBuilder,
    } = this.options
    if (this.model === undefined) {
      this.model = deserialize(filter, isResultFilter)
    }
    this.isResultFilter = isResultFilter
    this.collection = this.model.get('filters')
    this.$el.attr('data-id', this.model.cid)
    this.listenTo(this.model, 'change:operator', this.updateOperatorDropdown)
    if (isForm === true) {
      if (isFormBuilder !== true) {
        this.turnOffFieldAdditions()
      }
      this.turnOffNesting()
      this.turnOffRootOperator()
    }
  },
  onBeforeShow() {
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
        defaultSelection: [this.model.get('operator') || 'AND'],
      })
    )
    this.listenTo(
      this.filterOperator.currentView.model,
      'change:value',
      this.handleOperatorUpdate
    )
    this.filterContents.show(
      new FilterCollectionView({
        collection: this.collection,
        'filter-builder': this,
        isForm: this.options.isForm || false,
        isFormBuilder: this.options.isFormBuilder || false,
        suggester: this.options.suggester,
      })
    )
  },
  updateOperatorDropdown() {
    this.filterOperator.currentView.model.set('value', [
      this.model.get('operator'),
    ])
  },
  handleOperatorUpdate() {
    this.model.set(
      'operator',
      this.filterOperator.currentView.model.get('value')[0]
    )
  },
  delete() {
    this.model.destroy()
  },
  addFilter(filter) {
    this.collection.push({
      isResultFilter: this.isResultFilter,
    })
    this.handleEditing()
  },
  addFilterBuilder() {
    this.collection.push({
      filterBuilder: true,
      isResultFilter: this.isResultFilter,
    })
    this.handleEditing()
  },
  filterView: FilterView,
  printValue() {
    alert(this.transformToCql())
  },
  transformToCql() {
    this.deleteInvalidFilters()
    const filter = this.getFilters()
    if (filter.filters.length === 0) {
      return '("anyText" ILIKE \'%\')'
    } else {
      return CQLUtils.transformFilterToCQL(filter)
    }
  },
  getFilters() {
    return serialize(this.model)
  },
  deleteInvalidFilters() {
    const collection = this.collection.filter(
      model => model.get('isValid') !== false
    )

    this.collection.reset(collection, { silent: true })

    if (collection.length === 0) {
      this.delete()
    }
  },
  revert() {
    this.$el.removeClass('is-editing')
  },
  serializeData() {
    return {
      cql: 'anyText ILIKE ""',
    }
  },
  handleEditing() {
    const isEditing = this.$el.hasClass('is-editing')
    if (isEditing) {
      this.turnOnEditing()
    } else {
      this.turnOffEditing()
    }
  },
  sortCollection() {
    this.collection.sort()
  },
  turnOnEditing() {
    this.$el.addClass('is-editing')
    this.filterOperator.currentView.turnOnEditing()
    this.filterContents.currentView.turnOnEditing()
  },
  turnOffEditing() {
    this.$el.removeClass('is-editing')
    this.filterOperator.currentView.turnOffEditing()
    this.filterContents.currentView.turnOffEditing()
  },
  turnOffNesting() {
    this.$el.addClass('hide-nesting')
  },
  turnOffRootOperator() {
    this.$el.addClass('hide-root-operator')
  },
  turnOffFieldAdditions() {
    this.$el.addClass('hide-field-button')
  },
})
