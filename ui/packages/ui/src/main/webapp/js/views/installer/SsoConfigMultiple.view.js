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
define([
  'backbone.marionette',
  'underscore',
  'js/wreqr.js',
  'jquery',
  'templates/installer/ssoConfigMultiple.handlebars',
  'templates/installer/deletableEntry.handlebars',
], function(Marionette, _, wreqr, $, template, deletableEntryTemplate) {
  /* Displays metatype entry as multiple DeletableEntries */
  var SsoConfigMultiple = Marionette.ItemView.extend({
    template: template,
    tagName: 'tr',
    className: 'table-entry',
    events: {
      'click .plus-button': 'addValue',
      'click .minus-button': 'removeValue',
      'change .form-data': 'updateValue',
    },
    initialize: function(options) {
      this.name = options.name
      this.values = options.value
      if (typeof this.values === 'undefined') {
        this.values = options.defaultValue
      }
      this.description = options.description
      this.id = options.id
      this.cardinality = options.cardinality
      this.type = options.type
      this.typeName = options.typeName

      // wrap values in array if not already
      if (!Array.isArray(this.values)) {
        this.values = [this.values]
      }

      this.isValid = this.validateValues()
    },
    serializeData: function() {
      return {
        name: this.name,
        description: this.description,
      }
    },
    onRender: function() {
      this.entriesElement = this.$el.find('.entry-value-multiple-container')
      this.populateValues()
    },
    populateValues: function() {
      var self = this
      _.each(self.values, function(value) {
        self.addValue(value, true)
      })
    },
    addValue: function(value, isInitialization) {
      // default arguments
      value = value || ''
      isInitialization = isInitialization || false

      if (typeof value === 'object') {
        // if the given value is an event, set the new value to '' for a new blank entry
        value = ''
      }

      var entry = new DeletableEntry({
        name: name,
        value: value,
        type: this.type,
        typeName: this.typeName,
      })
      entry.render()

      this.entriesElement[0].append(entry.el)

      if (!isInitialization) {
        this.values.push(value)
        wreqr.vent.trigger('ssoConfigModified')
      }
    },
    updateValue: function(event) {
      var target = event.currentTarget
      var entry = target.parentElement
      var value = target.value
      var oldValue = target.getAttribute('oldValue')

      this.values[this.values.indexOf(oldValue)] = value

      target.setAttribute('oldValue', value)

      wreqr.vent.trigger('ssoConfigModified')
    },
    removeValue: function(event) {
      var removeButton = event.currentTarget
      var entry = removeButton.parentElement
      var inputField = entry.firstChild
      var inputValue = inputField.value

      this.values.splice(this.values.indexOf(inputValue), 1)
      this.entriesElement[0].removeChild(entry)

      wreqr.vent.trigger('ssoConfigModified')
    },
    validateValue: function(value) {
      switch (this.typeName) {
        case 'String':
          return typeof value === 'string'
        case 'Integer':
          return !Number.isNaN(parseInt(value))
      }
    },
    validateValues: function() {
      if (!Array.isArray(this.values)) {
        return false
      }

      var isValid = true
      var self = this
      _.each(self.values, function(value) {
        if (!self.validateValue(value)) {
          isValid = false
        }
      })

      return isValid
    },
    getConfig: function() {
      return {
        id: this.id,
        value: this.values,
      }
    },
    hasErrors: function() {
      return !this.validateValues(this.type, this.values)
    },
  })

  /* Displays metatype entry as a deltable text field */
  var DeletableEntry = Marionette.ItemView.extend({
    template: deletableEntryTemplate,
    tagName: 'div',
    className: 'deletable-entry',
    events: {
      'change .form-data': 'checkAndSetValid',
    },
    initialize: function(options) {
      this.name = options.name
      this.value = options.value
      this.type = options.type
      this.typeName = options.typeName

      this.isValid = this.validateValue()
    },
    serializeData: function() {
      return {
        value: this.value,
        typeName: this.typeName,
      }
    },
    onRender: function() {
      this.errorElement = this.$el.find('.error-message')

      if (this.isValid) {
        this.hideError()
      } else {
        this.showError()
      }
    },
    checkAndSetValid: function(event) {
      this.value = event.currentTarget.value
      this.isValid = this.validateValue()

      if (this.isValid) {
        this.hideError()
      } else {
        this.showError()
      }
    },
    validateValue: function() {
      switch (this.typeName) {
        case 'String':
          return typeof this.value === 'string'
        case 'Integer':
          return !Number.isNaN(parseInt(this.value))
      }
    },
    hideError: function() {
      this.errorElement[0].setAttribute('hidden', '')
    },
    showError: function() {
      this.errorElement[0].removeAttribute('hidden')
    },
    hasErrors: function() {
      return !this.isValid
    },
  })

  return SsoConfigMultiple
})
