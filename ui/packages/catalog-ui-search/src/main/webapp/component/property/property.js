/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

const _ = require('underscore')
const Backbone = require('backbone')
const moment = require('moment')
const CQLUtils = require('../../js/CQLUtils.js')
module.exports = Backbone.Model.extend({
  defaults: {
    value: [],
    values: {},
    enum: undefined,
    label: undefined,
    description: '',
    _initialValue: '',
    readOnly: false,
    validation: undefined,
    id: '',
    isEditing: false,
    bulk: false,
    multivalued: false,
    type: 'STRING',
    calculatedType: 'text',
    hasChanged: false,
    showValidationIssues: true,
    showLabel: true,
    onlyEditing: false,
    initializeToDefault: false,
    required: false,
    showRequiredWarning: false,
    transformValue: true,
  },
  setDefaultValue: function() {
    if (this.get('initializeToDefault')) {
      this.set('value', this.get('multivalued') ? [] : [this.getDefaultValue()])
    }
  },
  getDefaultValue: function() {
    switch (this.getCalculatedType()) {
      case 'boolean':
        return true
      case 'date':
        return new Date().toISOString()
      default:
        return ''
    }
  },
  //transform incoming value so later comparisons are easier
  transformValue: function() {
    const currentValue = this.getValue()
    switch (this.getCalculatedType()) {
      case 'thumbnail':
      case 'location':
        return
      case 'date':
        currentValue.sort()
        this.setValue(
          currentValue.map(function(dateValue) {
            if (dateValue) {
              return moment(dateValue).toISOString()
            } else {
              return dateValue
            }
          })
        )
        break
      case 'number':
        currentValue.sort()
        this.setValue(
          currentValue.map(function(value) {
            return Number(value) //handle cases of unnecessary number padding -> 22.0000
          })
        )
        break
      default:
        return
    }
  },
  initialize: function() {
    this._setCalculatedType()
    this.setDefaultValue()
    if (this.get('transformValue')) {
      this.transformValue()
    }
    this._setInitialValue()
    this.setDefaultLabel()
    this.listenTo(this, 'change:value', this.updateHasChanged)
  },
  setDefaultLabel: function() {
    if (!this.get('label')) {
      this.set('label', this.get('id'))
    }
  },
  hasChanged: function() {
    const currentValue = this.getValue()
    currentValue.sort()
    switch (this.getCalculatedType()) {
      case 'location':
        return (
          JSON.stringify(this.getInitialValue()) !==
            JSON.stringify(currentValue) &&
          JSON.stringify(
            this.getInitialValue().map(function(val) {
              return _.omit(val, ['property'])
            })
          ) !==
            JSON.stringify(
              currentValue.map(function(val) {
                val = CQLUtils.generateFilter(undefined, 'anyGeo', val)
                if (val === undefined) {
                  return val
                }
                return _.omit(
                  CQLUtils.transformCQLToFilter(
                    CQLUtils.transformFilterToCQL(val)
                  ),
                  ['property']
                )
              })
            )
        )
      default:
        return (
          JSON.stringify(currentValue) !==
          JSON.stringify(this.getInitialValue())
        )
    }
  },
  updateHasChanged: function() {
    this.set('hasChanged', this.hasChanged())
  },
  getValue: function() {
    return this.get('value')
  },
  setLabel: function(label) {
    this.set('label', label)
  },
  setValue: function(val) {
    this.set('value', val)
  },
  getId: function() {
    return this.get('id')
  },
  getInitialValue: function() {
    return this.get('_initialValue')
  },
  isReadOnly: function() {
    return this.get('readOnly')
  },
  hasConflictingDefinitions: function() {
    return this.get('hasConflictingDefinition') === true
  },
  isEditing: function() {
    return this.get('isEditing')
  },
  isMultivalued: function() {
    return this.get('multivalued')
  },
  isRequired: function() {
    return this.get('required')
  },
  isHomogeneous: function() {
    return !this.get('bulk') || Object.keys(this.get('values')).length <= 1
  },
  isValid() {
    if (this.parents) {
      return this.parents.every(function(value) {
        return value.isValid()
      })
    } else {
      return true
    }
  },
  showRequiredWarning() {
    this.set('showRequiredWarning', true)
  },
  hideRequiredWarning() {
    this.set('showRequiredWarning', false)
  },
  isBlank: function() {
    return this.getValue().every(function(value) {
      return value == null || value.trim().length === 0
    })
  },
  onlyEditing: function() {
    return this.get('onlyEditing')
  },
  showLabel: function() {
    return this.get('showLabel')
  },
  showValidationIssues: function() {
    return this.get('showValidationIssues')
  },
  revert: function() {
    this.set({
      hasChanged: false,
      value: this.getInitialValue(),
    })
  },
  _setInitialValue: function() {
    this.set('_initialValue', this.getValue())
  },
  _setCalculatedType: function() {
    let calculatedType

    switch (this.get('type')) {
      case 'DATE':
        calculatedType = 'date'
        break
      case 'TIME':
        calculatedType = 'time'
        break
      case 'BINARY':
        calculatedType = 'thumbnail'
        break
      case 'LOCATION':
        calculatedType = 'location'
        break
      case 'TEXTAREA':
        calculatedType = 'textarea'
        break
      case 'BOOLEAN':
        calculatedType = 'boolean'
        break
      case 'LONG':
      case 'DOUBLE':
      case 'FLOAT':
      case 'INTEGER':
      case 'SHORT':
        calculatedType = 'number'
        break
      case 'RANGE':
        calculatedType = 'range'
        break
      case 'GEOMETRY':
        calculatedType = 'geometry'
        break
      case 'AUTOCOMPLETE':
        calculatedType = 'autocomplete'
        break
      case 'COLOR':
        calculatedType = 'color'
        break
      case 'NEAR':
        calculatedType = 'near'
        break
      case 'PASSWORD':
        calculatedType = 'password'
        break
      case 'STRING':
      case 'XML':
      default:
        calculatedType = 'text'
        break
    }
    this.set('calculatedType', calculatedType)
  },
  getCalculatedType: function() {
    return this.get('calculatedType')
  },
})
