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
const $ = require('jquery')
const template = require('./filter-comparator.hbs')
const CustomElements = require('../../js/CustomElements.js')
const metacardDefinitions = require('../singletons/metacard-definitions.js')

import {
  geometryComparators,
  geometryComparatorsAnyGeo,
  dateComparators,
  stringComparators,
  stringComparatorsAnyText,
  numberComparators,
  booleanComparators,
} from '../filter/comparators'

module.exports = Marionette.ItemView.extend({
  template,
  tagName: CustomElements.register('filter-comparator'),
  className: 'is-action-list',
  modelEvents: {
    change: 'render',
  },
  events: {
    'click .choice': 'handleChoice',
  },
  ui: {},
  initialize() {},
  onRender() {
    this.handleValue()
  },
  handleValue() {
    this.$el.find('[data-value]').removeClass('is-selected')
    this.$el
      .find('[data-value="' + this.model.get('comparator') + '"]')
      .addClass('is-selected')
  },
  handleChoice(e) {
    const value = $(e.currentTarget).attr('data-value')
    this.model.set('comparator', value)
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  serializeData() {
    let comparators
    switch (metacardDefinitions.metacardTypes[this.model.get('type')].type) {
      case 'LOCATION':
      case 'GEOMETRY':
        comparators =
          this.model.get('type') === 'anyGeo'
            ? geometryComparatorsAnyGeo
            : geometryComparators
        if (comparators.indexOf(this.model.get('comparator')) === -1) {
          this.model.set('comparator', comparators[0])
        }
        return comparators
      case 'DATE':
        if (dateComparators.indexOf(this.model.get('comparator')) === -1) {
          this.model.set('comparator', dateComparators[0])
        }
        return dateComparators
      case 'BOOLEAN':
        if (booleanComparators.indexOf(this.model.get('comparator')) === -1) {
          this.model.set('comparator', booleanComparators[0])
        }
        return booleanComparators
      case 'LONG':
      case 'DOUBLE':
      case 'FLOAT':
      case 'INTEGER':
      case 'SHORT':
        if (numberComparators.indexOf(this.model.get('comparator')) === -1) {
          this.model.set('comparator', numberComparators[0])
        }
        return numberComparators
      default:
        comparators =
          this.model.get('type') === 'anyText'
            ? stringComparatorsAnyText
            : stringComparators
        if (comparators.indexOf(this.model.get('comparator')) === -1) {
          this.model.set('comparator', comparators[0])
        }
        if (this.model.get('isResultFilter')) {
          // if this view is being used as an ad hoc search results filter
          // (as opposed to a filter saved on a search), don't include
          // complex comparators like NEAR
          comparators = comparators.filter(comparator => comparator !== 'NEAR')
        }
        return comparators
    }
  },
})
