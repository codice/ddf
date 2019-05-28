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
const template = require('./query-adhoc.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const PropertyView = require('../property/property.view.js')
const properties = require('../../js/properties.js')
const CQLUtils = require('../../js/CQLUtils.js')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('query-adhoc'),
  modelEvents: {},
  events: {
    'click .editor-edit': 'turnOnEditing',
    'click .editor-cancel': 'cancel',
    'click .editor-save': 'save',
  },
  regions: {
    textField: '.properties-text',
  },
  ui: {},
  focus() {
    this.textField.currentView.focus()
  },
  initialize() {
    this.model = this.model._cloneOf
      ? store.getQueryById(this.model._cloneOf)
      : this.model
  },
  onBeforeShow() {
    this.setupTextField()
    this.turnOnEditing()
  },
  setupTextField() {
    this.textField.show(
      PropertyView.getPropertyView({
        id: 'Text',
        value: [this.options.text !== undefined ? this.options.text : ''],
        label: '',
        type: 'STRING',
        showValidationIssues: false,
        showLabel: false,
        placeholder: 'Search ' + properties.branding + ' ' + properties.product,
      })
    )
    this.textField.currentView.$el.keyup(event => {
      switch (event.keyCode) {
        case 13:
          this.$el.trigger('saveQuery.' + CustomElements.getNamespace())
          break
        default:
          break
      }
    })
    this.listenTo(
      this.textField.currentView.model,
      'change:value',
      this.saveToModel
    )
  },
  turnOnEditing() {
    this.$el.addClass('is-editing')
    this.regionManager.forEach(region => {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing()
      }
    })
    this.focus()
  },
  edit() {
    this.$el.addClass('is-editing')
    this.turnOnEditing()
  },
  cancel() {
    this.$el.removeClass('is-editing')
    this.onBeforeShow()
  },
  saveToModel() {
    const text = this.textField.currentView.model.getValue()[0]
    let cql
    if (text.length === 0) {
      cql = CQLUtils.generateFilter('ILIKE', 'anyText', '*')
    } else {
      cql = CQLUtils.generateFilter('ILIKE', 'anyText', text)
    }
    this.model.set('cql', CQLUtils.transformFilterToCQL(cql))
  },
  save() {
    this.$el.find('form')[0].submit()
    this.saveToModel()
  },
  isValid() {
    return this.textField.currentView.isValid()
  },
  setDefaultTitle() {
    let title = this.textField.currentView.model.getValue()[0]
    if (title.length === 0) {
      title = '*'
    }
    this.model.set('title', title)
  },
})
