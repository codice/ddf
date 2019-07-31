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
const _ = require('underscore')
const CustomElements = require('../../js/CustomElements.js')
import * as React from 'react'
import Filter from '../../react-component/filter'

module.exports = Marionette.LayoutView.extend({
  template() {
    return (
      <Filter
        model={this.model}
        {...this.options}
        editing={this.$el.hasClass('is-editing')}
        onRemove={() => this.delete()}
      />
    )
  },
  tagName: CustomElements.register('filter'),
  attributes() {
    return { 'data-id': this.model.cid }
  },
<<<<<<< HEAD
=======
  events: {
    'click > .filter-remove': 'delete',
  },
  modelEvents: {},
  regions: {
    filterRearrange: '.filter-rearrange',
    filterAttribute: '.filter-attribute',
    filterComparator: '.filter-comparator',
    filterInput: '.filter-input',
  },
  initialize() {
    this.listenTo(this.model, 'change:type', this.updateTypeDropdown)
    this.listenTo(this.model, 'change:type', this.determineInput)
    this.listenTo(this.model, 'change:value', this.determineInput)
    this.listenTo(this.model, 'change:comparator', this.determineInput)
  },
  onBeforeShow() {
    this.$el.toggleClass('is-sortable', this.options.isSortable || true)
    const filteredAttributeList = metacardDefinitions.sortedMetacardTypes
      .filter(({ id }) => !properties.isHidden(id))
      .filter(({ id }) => !metacardDefinitions.isHiddenType(id))
      .filter(
        ({ id }) =>
          this.options.includedAttributes === undefined
            ? true
            : this.options.includedAttributes.includes(id)
      )
      .map(({ alias, id }) => ({
        label: alias || id,
        value: id,
        description: (properties.attributeDescriptions || {})[id],
      }))

    let defaultSelection = this.model.get('type') || 'anyText'
    if (
      this.options.includedAttributes &&
      !this.options.includedAttributes.includes(defaultSelection)
    ) {
      defaultSelection = this.options.includedAttributes[0]
    }
    this.filterAttribute.show(
      DropdownView.createSimpleDropdown({
        list: filteredAttributeList,
        defaultSelection: [defaultSelection],
        hasFiltering: true,
      })
    )
    this.listenTo(
      this.filterAttribute.currentView.model,
      'change:value',
      this.handleAttributeUpdate
    )
    this._filterDropdownModel = new DropdownModel({
      value: this.model.get('comparator') || 'CONTAINS',
    })
    this.filterComparator.show(
      new FilterComparatorDropdownView({
        model: this._filterDropdownModel,
        modelForComponent: this.model,
      })
    )
    this.model.set('type', defaultSelection)
    this.determineInput()
  },
  transformValue(value, comparator) {
    switch (comparator) {
      case 'NEAR':
        if (value[0].constructor !== Object) {
          value[0] = {
            value: value[0],
            distance: 2,
          }
        }
        break
      case 'RANGE':
        if (value[0] && value[0].constructor !== Object) {
          value[0] = {
            lower: value[0] || 0,
            upper: value[0] || 0,
          }
        }
        break
      case 'INTERSECTS':
      case 'DWITHIN':
        break
      default:
        if (value === null || value[0] === null) {
          value = ['']
          break
        }
        if (value[0].constructor === Object) {
          value[0] = value[0].value
        }
        break
    }
    return value
  },
  // With the relative date comparator being the same as =, we need to try and differentiate them this way
  updateTypeDropdown() {
    const attribute = this.model.get('type')
    if (attribute === 'anyGeo') {
      this.model.set('comparator', [geometryComparators[1]])
    } else if (attribute === 'anyText') {
      this.model.set('comparator', [stringComparators[1]])
    }
    this.filterAttribute.currentView.model.set('value', [attribute])
  },
  handleAttributeUpdate() {
    const previousAttributeType =
      metacardDefinitions.metacardTypes[this.model.get('type')].type
    this.model.set(
      'type',
      this.filterAttribute.currentView.model.get('value')[0]
    )
    const currentAttributeType =
      metacardDefinitions.metacardTypes[this.model.get('type')].type
    if (currentAttributeType !== previousAttributeType) {
      this.model.set('value', [''])
    }
  },
>>>>>>> changed view to use styled divs, added more unit tests to solr filter delegate and made isBetween throw new error for more clear message
  delete() {
    this.model.destroy()
  },
  turnOnEditing() {
    if (this.$el.hasClass('is-editing')) return
    this.$el.addClass('is-editing')
    this.render()
  },
  turnOffEditing() {
    if (!this.$el.hasClass('is-editing')) return
    this.$el.removeClass('is-editing')
    this.render()
  },
})
