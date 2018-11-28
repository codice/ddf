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
/*global require*/
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const moment = require('moment')
import * as React from 'react'
import DateComponent from '../../react-component/container/input-wrappers/date'

module.exports = Marionette.LayoutView.extend({
  template() {
    const { from = '', to = '' } = this.getStartingValue() || {}
    this.fromValue = from
    this.toValue = to
    return (
      <React.Fragment>
        <DateComponent
          label="From"
          placeholder="Limit search to after this time."
          value={from}
          onChange={this.handleFromUpdate.bind(this)}
        />
        <DateComponent
          label="To"
          placeholder="Limit search to before this time."
          value={to}
          onChange={this.handleToUpdate.bind(this)}
        />
      </React.Fragment>
    )
  },
  tagName: CustomElements.register('between-time'),
  handleFromUpdate(value) {
    this.fromValue = value
    this.updateModelValue()
  },
  handleToUpdate(value) {
    this.toValue = value
    this.updateModelValue()
  },
  getViewValue() {
    return this.swapRange
      ? `${this.toValue}/${this.fromValue}`
      : `${this.fromValue}/${this.toValue}`
  },
  parseValue(value) {
    if (value === null || value === undefined || !value.includes('/')) {
      return
    }
    const dates = value.split('/')
    const from = dates[0]
    const to = dates[1]
    return {
      from,
      to,
    }
  },
  getModelValue() {
    const currentValue = this.model.toJSON().value[0]
    return this.parseValue(currentValue)
  },
  updateModelValue() {
    this.checkRangeValidity()
    if (this.model !== undefined) {
      this.model.setValue([this.getViewValue()])
    }
  },
  checkRangeValidity() {
    /* Can't swap fromValue and toValue directly because they will be out
    of sync with what's displayed in the date pickers. */
    const from = moment(this.fromValue, moment.ISO_8601)
    const to = moment(this.toValue, moment.ISO_8601)
    this.swapRange = from.isValid() && to.isValid() && from.isAfter(to)
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
  isValid() {
    const value = this.getModelValue()
    const from = moment(value.from, moment.ISO_8601)
    const to = moment(value.to, moment.ISO_8601)
    return from.isValid() && to.isValid() && from.isBefore(to)
  },
})
