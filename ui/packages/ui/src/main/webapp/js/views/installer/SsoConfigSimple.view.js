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
  'templates/installer/ssoConfigSimple.handlebars',
], function(Marionette, _, wreqr, $, template) {
  /* Displays metatype entry as a text field */
  var SsoConfigSimple = Marionette.ItemView.extend({
    template: template,
    tagName: 'tr',
    className: 'table-entry',
    events: {
      'change .form-data': 'updateValue',
    },
    initialize: function(options) {
      this.name = options.name
      this.value = options.value
      if (typeof this.value === 'undefined') {
        this.value = (options.defaultValue || [])[0]
      }
      this.description = options.description
      this.id = options.id
      this.cardinality = options.cardinality
      this.type = options.type
      this.typeName = options.typeName

      this.isValid = this.validateValue()
    },
    serializeData: function() {
      return {
        name: this.name,
        value: this.value,
        description: this.description,
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
    updateValue: function(event) {
      this.value = event.currentTarget.value.toString()
      this.isValid = this.validateValue()

      wreqr.vent.trigger('ssoConfigModified')

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
    getConfig: function() {
      return {
        id: this.id,
        value: this.value,
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

  return SsoConfigSimple
})
