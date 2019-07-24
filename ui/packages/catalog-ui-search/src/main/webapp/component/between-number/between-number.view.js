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
const CustomElements = require('../../js/CustomElements.js')
import * as React from 'react'
import NumberComponent from '../../react-component/container/input-wrappers/number'
import { isNumber } from 'util'

module.exports = Marionette.LayoutView.extend({
  template() {
    const { min = '', max = '' } = this.getStartingValue() || {}
    this.minValue = min
    this.maxValue = max
    return (
      <React.Fragment>
        <NumberComponent
          value={min}
          onChange={this.handleMinUpdate.bind(this)}
          placeholder="0"
        />
        <span className="label">TO</span>
        <NumberComponent
          value={max}
          onChange={this.handleMaxUpdate.bind(this)}
          placeholder="2"
        />
      </React.Fragment>
    )
  },
  tagName: CustomElements.register('between-numbers'),
  handleMinUpdate(value) {
    this.minValue = value
    this.updateModelValue()
  },
  handleMaxUpdate(value) {
    this.maxValue = value
    this.updateModelValue()
  },
  getModelValue() {
    return this.model.toJSON().value[0]
  },
  getViewValue() {
    return {
      min: this.minValue,
      max: this.maxValue,
    }
  },
  updateModelValue() {
    if (this.model !== undefined) {
      this.model.setValue([this.getViewValue()])
    }
  },
  getStartingValue() {
    if (this.model !== undefined) {
      return this.getModelValue()
    }
  },
  validateType(value) {
    let typeOfValues = this.model.attributes.typeOfValues
    if (
      typeOfValues === 'INTEGER' ||
      typeOfValues === 'LONG' ||
      typeOfValues === 'SHORT'
    ) {
      if (value % 1 != 0) {
        return false
      }
    }
    return true
  },
  isValid() {
    const value = this.getModelValue()
    let isValid = true

    this.$el
      .find('intrigue-input.is-number')
      .toggleClass('has-validation-issues', false)

    if (value === undefined) {
      this.$el
        .find('intrigue-input.is-number')
        .toggleClass('has-validation-issues', true)
      isValid = false
    }
    if (!(this.validateType(value.min) && isNumber(value.min))) {
      this.$el
        .find('intrigue-input.is-number:eq(0)')
        .toggleClass('has-validation-issues', true)
      this.$el
        .find('intrigue-input.is-number:eq(0)')
        .find('.for-error')
        .text(
          'Incorrect input type. ' +
            this.model.attributes.typeOfValues +
            ' required.'
        )
      isValid = false
    }
    if (!(this.validateType(value.max) && isNumber(value.max))) {
      this.$el
        .find('intrigue-input.is-number:eq(1)')
        .toggleClass('has-validation-issues', true)
      this.$el
        .find('intrigue-input.is-number:eq(1)')
        .find('.for-error')
        .text(
          'Incorrect input type. ' +
            this.model.attributes.typeOfValues +
            ' required.'
        )
      isValid = false
    }
    return isValid
  },
})
