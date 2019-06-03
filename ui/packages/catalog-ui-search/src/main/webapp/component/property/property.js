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
  setDefaultValue() {
    if (this.get('initializeToDefault')) {
      this.set('value', this.get('multivalued') ? [] : [this.getDefaultValue()])
    }
  },
  getDefaultValue() {
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
  transformValue() {
    const currentValue = this.getValue()
    switch (this.getCalculatedType()) {
      case 'thumbnail':
      case 'location':
        return
      case 'date':
        currentValue.sort()
        this.setValue(
          currentValue.map(dateValue => {
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
          currentValue.map((
            value //handle cases of unnecessary number padding -> 22.0000
          ) => Number(value))
        )
        break
      default:
        return
    }
  },
  initialize() {
    this._setCalculatedType()
    this.setDefaultValue()
    if (this.get('transformValue')) {
      this.transformValue()
    }
    this._setInitialValue()
    this.setDefaultLabel()
    this.listenTo(this, 'change:value', this.updateHasChanged)
  },
  setDefaultLabel() {
    if (!this.get('label')) {
      this.set('label', this.get('id'))
    }
  },
  hasChanged() {
    const currentValue = this.getValue()
    currentValue.sort()
    switch (this.getCalculatedType()) {
      case 'location':
        return (
          JSON.stringify(this.getInitialValue()) !==
            JSON.stringify(currentValue) &&
          JSON.stringify(
            this.getInitialValue().map(val => _.omit(val, ['property']))
          ) !==
            JSON.stringify(
              currentValue.map(val => {
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
  updateHasChanged() {
    this.set('hasChanged', this.hasChanged())
  },
  getValue() {
    return this.get('value')
  },
  setLabel(label) {
    this.set('label', label)
  },
  setValue(val) {
    this.set('value', val)
  },
  getId() {
    return this.get('id')
  },
  getInitialValue() {
    return this.get('_initialValue')
  },
  isReadOnly() {
    return this.get('readOnly')
  },
  hasConflictingDefinitions() {
    return this.get('hasConflictingDefinition') === true
  },
  isEditing() {
    return this.get('isEditing')
  },
  isMultivalued() {
    return this.get('multivalued')
  },
  isRequired() {
    return this.get('required')
  },
  isHomogeneous() {
    return !this.get('bulk') || Object.keys(this.get('values')).length <= 1
  },
  isValid() {
    if (this.parents) {
      return this.parents.every(value => value.isValid())
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
  isBlank() {
    return this.getValue().every(
      value => value == null || value.trim().length === 0
    )
  },
  onlyEditing() {
    return this.get('onlyEditing')
  },
  showLabel() {
    return this.get('showLabel')
  },
  showValidationIssues() {
    return this.get('showValidationIssues')
  },
  revert() {
    this.set({
      hasChanged: false,
      value: this.getInitialValue(),
    })
  },
  _setInitialValue() {
    this.set('_initialValue', this.getValue())
  },
  _setCalculatedType() {
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
  getCalculatedType() {
    return this.get('calculatedType')
  },
})
