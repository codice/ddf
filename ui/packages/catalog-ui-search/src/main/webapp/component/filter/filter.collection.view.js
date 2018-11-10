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
const _ = require('underscore')
const $ = require('jquery')
const CustomElements = require('../../js/CustomElements.js')
const FilterModel = require('./filter')
const FilterBuilderModel = require('../filter-builder/filter-builder.js')
const Sortable = require('sortablejs')

module.exports = Marionette.CollectionView.extend({
  getChildView: function(item) {
    switch (item.type) {
      case 'filter':
        return this.options['filter-builder'].filterView
      case 'filter-builder':
        return this.options['filter-builder'].constructor
    }
  },
  tagName: CustomElements.register('filter-collection'),
  onBeforeRenderCollection: function() {
    this.sortable = Sortable.create(this.el, {
      handle: 'div.filter-rearrange',
      animation: 250,
      draggable: '>*',
      disabled: this.options.isForm && !this.options.isFormBuilder,
      onEnd: function() {
        _.forEach(
          this.$el.children(
            `${CustomElements.getNamespace()}filter-builder` +
              ',' +
              `${CustomElements.getNamespace()}filter`
          ),
          (element, index) => {
            this.collection
              .get(element.getAttribute('data-id'))
              .set('sortableOrder', index)
          }
        )
      }.bind(this),
    })
  },
  childViewOptions: function() {
    return {
      isForm: this.options.isForm || false,
      isFormBuilder: this.options.isFormBuilder || false,
      isSortable: !this.sortable.options.disabled,
    }
  },
  initialize: function() {
    this.listenTo(this.collection, 'remove', this.handleMinusButton)
    this.listenTo(this.collection, 'add', this.handleMinusButton)
    this.handleMinusButton()
  },
  addFilter: function(filterModel) {
    filterModel = filterModel || new FilterModel()
    this.collection.push(filterModel)
    return this.children.last()
  },
  addFilterBuilder: function(filterBuilderModel) {
    filterBuilderModel = filterBuilderModel || new FilterBuilderModel()
    this.collection.push(filterBuilderModel)
    return this.children.last()
  },
  turnOnEditing: function() {
    this.children.forEach(function(childView) {
      childView.turnOnEditing()
    })
  },
  turnOffEditing: function() {
    this.children.forEach(function(childView) {
      childView.turnOffEditing()
    })
  },
  handleMinusButton: function() {
    this.$el.toggleClass('can-delete', this.collection.length > 1)
  },
})
