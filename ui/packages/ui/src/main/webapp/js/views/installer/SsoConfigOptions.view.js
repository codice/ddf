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
  'templates/installer/ssoConfigOptions.handlebars',
], function(Marionette, _, wreqr, $, template) {
  var SsoConfigOptions = Marionette.ItemView.extend({
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
      this.options = options.options
      this.type = options.type
      this.typeName = options.typeName

      this.optionElements = []
    },
    onRender: function() {
      this.dropdown = this.$el.find('.form-data')

      this.populateOptions()
      this.setDefaultOption()
    },
    populateOptions: function() {
      var self = this
      _.each(self.options, function(option) {
        var optionElement = self.el.ownerDocument.createElement('OPTION')
        optionElement.setAttribute('value', option.value)
        optionElement.innerText = option.label

        self.optionElements.push(optionElement)
        self.dropdown.append(optionElement)
      })
    },
    setDefaultOption: function() {
      var self = this
      _.each(self.optionElements, function(optionElement) {
        if (optionElement.getAttribute('value') === self.value) {
          optionElement.setAttribute('selected', '')
        }
      })
    },
    serializeData: function() {
      return {
        name: this.name,
        description: this.description,
      }
    },
    updateValue: function(event) {
      var updatedValue = event.currentTarget.value

      this.value = event.currentTarget.value
      wreqr.vent.trigger('ssoConfigModified')
    },
    getConfig: function() {
      return {
        id: this.id,
        value: this.value,
      }
    },
    hasErrors: function() {
      return false // can only select values from given options
    },
  })

  return SsoConfigOptions
})
