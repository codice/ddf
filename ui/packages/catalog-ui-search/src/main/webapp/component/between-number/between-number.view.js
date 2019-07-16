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

const _ = require('underscore')
const InputView = require('../input/input.view')
import * as React from 'react'
import NumberComponent from '../../react-component/container/input-wrappers/number'
module.exports = InputView.extend({
  template () {
    const {min = '', max = ''} =  {} || {}
    this.minValue = min
    this.maxValue = max
    return (
        <React.Fragment>
            <NumberComponent
                value={min}
                onChange={this.handleMinUpdate.bind(this)}
                placeholder='0'
            />
            <span className="label">TO</span>
            <NumberComponent
                value={max}
                onChange={this.handleMaxUpdate.bind(this)}
                placeholder='2'
            />
        </React.Fragment>
    )
  },
  className: 'between-numbers',
  getCurrentValue() {
    console.log( this.model.toJSON().value[0]);
    return [this.model.toJSON.value[0]];
  },
  onAttach() {
    const width = this.$el
      .find('.label')
      .last()
      .outerWidth()
    this.$el.find('.label').css('width', `calc(50% - ${width / 2}px)`)
    InputView.prototype.onAttach.call(this)
  },
  updateModelValue() {
    if (this.model !== undefined) {
      this.model.setValue([this.getCurrentValue()])
    }
  },
  handleMinUpdate(value){
      this.minValue = value
      this.updateModelValue
  },
  handleMaxUpdate(value){
      this.maxValue = value
      this.updateModelValue
  },
  serializeData() {
    const value = this.model.getValue() || {
      min: 0,
      max: 2,
    }
    return _.extend(this.model.toJSON(), {
      min: value.min,
      max: value.max,
    })
  },
})
