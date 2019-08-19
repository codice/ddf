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
import * as React from 'react'
import NumberComponent from '../../react-component/input-wrappers/number'
import { isNumber } from 'util'
import styled from 'styled-components'

const InputContainer = styled.div`
  vertical-align: top;
  display: flex;
  flex-direction: row;
  align-items: center;
  transform: translateY(-50%);
  width: 200px;
`

const Label = styled.div`
  align-self: center;
  justify-self: center;
  transform: translateY(150%);
`

const InputWrapper = styled.div`
  intrigue-property {
    padding: 0px;
  }

  .property-revert {
    transform: translateX(-150%);
  }
`

module.exports = Marionette.LayoutView.extend({
  template() {
    const { lower = 0, upper = 0 } = this.getStartingValue() || {}
    this.lowerValue = lower
    this.upperValue = upper
    return (
      <React.Fragment>
        <InputContainer>
          <InputWrapper className="range-input">
            <NumberComponent
              value={lower}
              onChange={this.handleLowerUpdate.bind(this)}
            />
          </InputWrapper>
          <Label className="label">TO</Label>
          <InputWrapper className="range-input">
            <NumberComponent
              value={upper}
              onChange={this.handleUpperUpdate.bind(this)}
            />
          </InputWrapper>
        </InputContainer>
      </React.Fragment>
    )
  },
  handleLowerUpdate(value) {
    this.lowerValue = value
    this.updateModelValue()
  },
  handleUpperUpdate(value) {
    this.upperValue = value
    this.updateModelValue()
  },
  getModelValue() {
    return this.model.toJSON().value[0]
  },
  getViewValue() {
    return {
      lower: this.lowerValue,
      upper: this.upperValue,
    }
  },
  onAttach() {
    const width = this.$el
      .find('.label')
      .last()
      .outerWidth()
    this.$el
      .find('.range-input, .param')
      .css('width', `calc(50% - ${width / 2}px)`)
  },
  updateModelValue() {
    if (this.model !== undefined) {
      this.model.setValue([this.getViewValue()])
    }
  },
  getStartingValue() {
    if (this.model !== undefined) {
      return this.getModelValue()
    } else if (this.options !== undefined) {
      return this.options.value
    }
  },
  validateType(value) {
    let typeOfValues = this.model.attributes.typeOfValues
    if (
      typeOfValues === 'INTEGER' ||
      typeOfValues === 'LONG' ||
      typeOfValues === 'SHORT'
    ) {
      return value % 1 === 0
    }
    return true
  },
  isValid() {
    const value = this.getModelValue()
    let isValid = true

    this.$el
      .find('intrigue-input.is-number')
      .toggleClass('has-validation-issues', false)

    if (value === undefined || !value) {
      this.$el
        .find('intrigue-input.is-number')
        .toggleClass('has-validation-issues', true)
      return false
    }
    if (!(this.validateType(value.lower) && isNumber(value.lower))) {
      this.$el
        .find('intrigue-input.is-number:eq(0)')
        .toggleClass('has-validation-issues', true)
      this.$el
        .find('intrigue-input.is-number:eq(0)')
        .find('.for-error')
        .text(this.model.attributes.typeOfValues + ' required.')
      isValid = false
    }
    if (!(this.validateType(value.upper) && isNumber(value.upper))) {
      this.$el
        .find('intrigue-input.is-number:eq(1)')
        .toggleClass('has-validation-issues', true)
      this.$el
        .find('intrigue-input.is-number:eq(1)')
        .find('.for-error')
        .text(this.model.attributes.typeOfValues + ' required.')
      isValid = false
    }
    return isValid
  },
})
