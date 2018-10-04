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
/*global define, setTimeout*/
define([
  'marionette',
  'backbone',
  'underscore',
  'jquery',
  './query-adhoc.hbs',
  'js/CustomElements',
  'js/store',
  'component/property/property.view',
  'component/property/property',
  'component/singletons/user-instance',
  'js/Common',
  'properties',
], function(
  Marionette,
  Backbone,
  _,
  $,
  template,
  CustomElements,
  store,
  PropertyView,
  Property,
  user,
  Common,
  properties
) {
  return Marionette.LayoutView.extend({
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
          placeholder:
            'Search ' + properties.branding + ' ' + properties.product,
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
      var text = this.textField.currentView.model.getValue()[0]
      var cql
      if (text.length === 0) {
        cql = "anyText ILIKE '*'"
      } else {
        cql = "anyText ILIKE '" + text + "'"
      }
      this.model.set('cql', cql)
    },
    save: function() {
      this.$el.find('form')[0].submit()
      this.saveToModel()
    },
    isValid: function() {
      return this.textField.currentView.isValid()
    },
    setDefaultTitle: function() {
      var title = this.textField.currentView.model.getValue()[0]
      if (title.length === 0) {
        title = '*'
      }
      this.model.set('title', title)
    },
  })
})
