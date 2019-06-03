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

import React from 'react'
const _ = require('lodash')
const $ = require('jquery')
const InputView = require('../input.view')
const user = require('../../singletons/user-instance.js')
const moment = require('moment-timezone')
require('eonasdan-bootstrap-datetimepicker')

module.exports = InputView.extend({
  template(data) {
    return (
      <React.Fragment>
        <div className="if-editing">
          <div className="input-group time">
            <input type="text" placeholder={data.property.placeholder} />
            <span className="input-group-addon">
              <span className="fa fa-clock-o" />
            </span>
          </div>
        </div>
        <div className="if-viewing">
          <label>{data.humanReadableTime}</label>
        </div>
      </React.Fragment>
    )
  },
  events: {
    'click .input-revert': 'revert',
    'dp.change .input-group.time': 'handleRevert',
    'dp.show .input-group.time': 'handleOpen',
    'dp.hide .input-group.time': 'removeResizeHandler',
  },
  serializeData() {
    return _.extend(this.model.toJSON(), {
      cid: this.cid,
      humanReadableTime: this.model.getValue()
        ? this.getUserReadableTime(this.model.getValue())
        : this.model.getValue(),
    })
  },
  initialize() {
    this.listenTo(
      user.get('user').get('preferences'),
      'change:timeZone change:dateTimeFormat',
      this.initializeTimePicker
    )
    InputView.prototype.initialize.call(this)
  },
  onRender() {
    this.initializeTimePicker()
    InputView.prototype.onRender.call(this)
  },
  initializeTimePicker() {
    this.onDestroy()
    this.$el.find('.input-group.time').datetimepicker({
      format: this.getTimeFormat(),
      timeZone: this.getTimeZone(),
      widgetParent: 'body',
      keyBinds: null,
    })
  },
  handleValue() {
    this.$el
      .find('.input-group.time')
      .data('DateTimePicker')
      .date(this.getUserReadableTime(this.model.getValue()))
  },
  handleOpen() {
    this.updatePosition()
    this.addResizeHandler()
  },
  updatePosition() {
    const datepicker = $('body').find('.bootstrap-datetimepicker-widget:last')
    const inputCoordinates = this.$el
      .find('.input-group.time')[0]
      .getBoundingClientRect()
    const top = datepicker.hasClass('bottom')
      ? inputCoordinates.top + inputCoordinates.height
      : inputCoordinates.top - datepicker.outerHeight()
    datepicker.css({
      top: top + 'px',
      bottom: 'auto',
      left: inputCoordinates.left + 'px',
      width: inputCoordinates.width + 'px',
    })
  },
  addResizeHandler() {
    $(window).on('resize.datePicker', this.updatePosition.bind(this))
  },
  removeResizeHandler() {
    $(window).off('resize.datePicker')
  },
  getCurrentValue() {
    const currentValue = this.$el.find('input').val()
    if (currentValue) {
      return moment
        .tz(currentValue, this.getTimeFormat(), this.getTimeZone())
        .toISOString()
    } else {
      return null
    }
  },
  listenForChange() {
    this.$el.on('dp.change click input change keyup', () => {
      this.model.set('value', this.getCurrentValue())
      this.validate()
    })
  },
  isValid() {
    const currentValue = this.$el.find('input').val()
    return currentValue != null && currentValue !== ''
  },
  onDestroy() {
    const datetimepicker = this.$el
      .find('.input-group.time')
      .data('DateTimePicker')
    if (datetimepicker) {
      datetimepicker.destroy()
    }
  },
  getTimeFormat() {
    return user
      .get('user')
      .get('preferences')
      .get('dateTimeFormat')['timefmt']
  },
  getTimeZone() {
    return user
      .get('user')
      .get('preferences')
      .get('timeZone')
  },
  getUserReadableTime(time) {
    return moment.tz(time, this.getTimeZone()).format(this.getTimeFormat())
  },
})
