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
const Backbone = require('backbone')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./query-adhoc.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const user = require('../singletons/user-instance.js')
const Common = require('../../js/Common.js')
const properties = require('../../js/properties.js')
const CQLUtils = require('../../js/CQLUtils.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
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
  focus: function() {
    this.textField.currentView.focus()
  },
  initialize: function() {
    this.model = this.model._cloneOf
      ? store.getQueryById(this.model._cloneOf)
      : this.model
  },
  onBeforeShow: function() {
    this.setupTextField()
    this.turnOnEditing()
  },
  setupTextField: function() {
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
  turnOnEditing: function() {
    this.$el.addClass('is-editing')
    this.regionManager.forEach(function(region) {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing()
      }
    })
    this.focus()
  },
  edit: function() {
    this.$el.addClass('is-editing')
    this.turnOnEditing()
  },
  cancel: function() {
    this.$el.removeClass('is-editing')
    this.onBeforeShow()
  },
  saveToModel: function() {
    const text = this.textField.currentView.model.getValue()[0];
    let cql;
    if (text.length === 0) {
      cql = CQLUtils.generateFilter('ILIKE', 'anyText', '*')
    } else {
      cql = CQLUtils.generateFilter('ILIKE', 'anyText', text)
    }
    this.model.set('cql', CQLUtils.transformFilterToCQL(cql))
  },
  save: function() {
    this.$el.find('form')[0].submit()
    this.saveToModel()
  },
  isValid: function() {
    return this.textField.currentView.isValid()
  },
  setDefaultTitle: function() {
    let title = this.textField.currentView.model.getValue()[0];
    if (title.length === 0) {
      title = '*'
    }
    this.model.set('title', title)
  },
})
