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

import InputBoolean from './input-boolean.presentation'
import * as React from 'react'

var Marionette = require('marionette')
var template = require('./input-boolean.hbs')
var InputView = require('../input.view')

module.exports = InputView.extend({
  template(props) {
    const { placeholder, id } = props.property
    const { value, cid } = props
    return (
      <InputBoolean placeholder={placeholder} id={id} cid={cid} value={value} />
    )
  },
  getCurrentValue: function() {
    return this.$el.find('input').is(':checked')
  },
  handleValue: function() {
    this.$el.find('input').attr('checked', Boolean(this.model.getValue()))
  },
  events: {
    'click label': 'triggerCheckboxClick',
  },
  triggerCheckboxClick: function() {
    this.$el.find('input').click()
  },
})
