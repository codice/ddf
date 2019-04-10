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

var Marionette = require('marionette')
var template = require('./relative-time.hbs')
var CustomElements = require('../../js/CustomElements.js')
var PropertyView = require('../property/property.view.js')
var Property = require('../property/property.js')
var CQLUtils = require('../../js/CQLUtils.js')
var Common = require('../../js/Common.js')
import { serialize, deserialize } from './serial'

/*
    For specifying a relative time.  It shows a number field and a units field.
    Supports passing in a model that will have it's value field auto updated and synced.
    Supports not passing in a model and instead passing in a value option that is a valid period such as:
    "RELATIVE(PT1.5H)"
    Supports not passing in anything and simply letting the values default.  The values can be grabbed whenever needed by calling
    getViewValue.  This will return the values like so:
    "RELATIVE(PT1.5H)"
*/
module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('relative-time'),
  regions: {
    basicTimeRelativeValue: '.relative-value',
    basicTimeRelativeUnit: '.relative-unit',
  },
  onBeforeShow: function() {
    this.setupTimeRelative()
    this.turnOnEditing()
    this.listenTo(
      this.basicTimeRelativeUnit.currentView.model,
      'change:value',
      this.updateModelValue
    )
    this.listenTo(
      this.basicTimeRelativeValue.currentView.model,
      'change:value',
      this.updateModelValue
    )
    this.updateModelValue()
  },
  turnOnEditing: function() {
    this.$el.addClass('is-editing')
    this.regionManager.forEach(function(region) {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing()
      }
    })
  },
  getViewValue: function() {
    let last = this.basicTimeRelativeValue.currentView.model.getValue()[0]
    if (last === '') {
      last = 0
    }
    const unit = this.basicTimeRelativeUnit.currentView.model.getValue()[0]
    return serialize({ last, unit })
  },
  parseValue(value) {
    return deserialize(value)
  },
  getModelValue() {
    const currentValue = this.model.toJSON().value[0]
    return this.parseValue(currentValue)
  },
  updateModelValue() {
    if (this.model === undefined) {
      return
    }
    this.model.setValue([this.getViewValue()])
  },
  getOptionsValue() {
    return this.parseValue(this.options.value)
  },
  getStartingValue() {
    if (this.model !== undefined) {
      return this.getModelValue()
    } else if (this.options !== undefined) {
      return this.getOptionsValue()
    }
  },
  setupTimeRelative: function() {
    const { last = 1, unit = 'h' } = this.getStartingValue() || {}
    this.basicTimeRelativeValue.show(
      new PropertyView({
        model: new Property({
          value: [last],
          id: 'Last',
          placeholder: 'Limit searches to between the present and this time.',
          type: 'INTEGER',
        }),
      })
    )
    this.basicTimeRelativeUnit.show(
      new PropertyView({
        model: new Property({
          value: [unit],
          enum: [
            {
              label: 'Minutes',
              value: 'm',
            },
            {
              label: 'Hours',
              value: 'h',
            },
            {
              label: 'Days',
              value: 'd',
            },
            {
              label: 'Months',
              value: 'M',
            },
            {
              label: 'Years',
              value: 'y',
            },
          ],
          id: 'Unit',
        }),
      })
    )
  },
  isValid: function() {
    const value = this.getModelValue()
    return value.last !== undefined && value.unit !== undefined
  },
})
