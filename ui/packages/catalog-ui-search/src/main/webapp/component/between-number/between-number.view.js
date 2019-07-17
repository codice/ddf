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
  isValid() {
    const value = this.getModelValue()
    if (value === undefined) {
      return false
    }
    return isNumber(this.minValue) && isNumber(this.maxValue)
  },
})
