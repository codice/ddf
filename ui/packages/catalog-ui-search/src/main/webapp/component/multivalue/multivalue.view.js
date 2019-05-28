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
const template = require('./multivalue.hbs')
const CustomElements = require('../../js/CustomElements.js')
const ValueCollection = require('../value/value.collection.view.js')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('multivalue'),
  events: {
    'click .multivalue-add': 'addNewValue',
  },
  modelEvents: {
    'change:isEditing': 'handleEdit',
  },
  regions: {
    values: '.multivalue-values',
  },
  initialize() {
    this.handleMultivalue()
    this.handleEdit()
  },
  handleEdit() {
    this.$el.toggleClass('is-editing', this.model.get('isEditing'))
  },
  onBeforeShow() {
    this.values.show(ValueCollection.generateValueCollectionView(this.model))
  },
  handleMultivalue() {
    this.$el.toggleClass('is-multivalued', this.model.get('multivalued'))
  },
  serializeData() {
    return {
      numberOfValues: Object.keys(this.model.get('value')).length,
    }
  },
  addNewValue() {
    this.values.currentView.addNewValue(this.model)
  },
  isValid() {
    if (this.model.get('enumCustom')) {
      return true
    }
    return this.values.currentView.children.every(valueView => {
      const inputView = valueView.input.currentView
      return inputView.isValid()
    })
  },
})
