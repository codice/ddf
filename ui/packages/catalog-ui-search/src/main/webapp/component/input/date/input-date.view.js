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
/*global define, alert, window*/
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./input-date.hbs')
const CustomElements = require('../../../js/CustomElements.js')
const moment = require('moment-timezone')
const InputView = require('../input.view')
const Common = require('../../../js/Common.js')
const user = require('../../singletons/user-instance.js')
require('eonasdan-bootstrap-datetimepicker')

const timeOnlyFormat = 'HH:mm:ss.SSS'

function getDateFormat() {
  return user
    .get('user')
    .get('preferences')
    .get('dateTimeFormat')['datetimefmt']
}

function getTimeZone() {
  return user
    .get('user')
    .get('preferences')
    .get('timeZone')
}

module.exports = InputView.extend({
  template: template,
  events: {
    'dp.show .input-group.date': 'handleOpen',
    'dp.hide .input-group.date': 'removeResizeHandler',
  },
  serializeData: function() {
    const propertyJSON = _.extend(this.model.toJSON(), {
      cid: this.cid,
      humanReadableDate: this.model.getValue()
        ? user.getUserReadableDateTime(this.model.getValue())
        : this.model.getValue(),
    })
    if (propertyJSON.property.placeholder === undefined) {
      propertyJSON.property.placeholder = getDateFormat()
    }
    return propertyJSON
  },
  initialize: function() {
    this.listenTo(
      user.get('user').get('preferences'),
      'change:timeZone change:dateTimeFormat',
      this.initializeDatepicker
    )
    InputView.prototype.initialize.call(this)
  },
  hasSameTime: function(newDate, oldDate) {
    if (oldDate == null || newDate == null) {
      return false
    }
    const newTime = newDate.format(timeOnlyFormat)
    const oldTime = oldDate.format(timeOnlyFormat)
    if (newTime == oldTime) {
      return true
    }
    return false
  },
  onRender: function() {
    this.initializeDatepicker()
    InputView.prototype.onRender.call(this)
  },
  initializeDatepicker: function() {
    this.onDestroy()
    this.$el.find('.input-group.date').datetimepicker({
      format: getDateFormat(),
      timeZone: getTimeZone(),
      widgetParent: 'body',
      keyBinds: null,
    })
  },
  handleReadOnly: function() {
    this.$el.toggleClass('is-readOnly', this.model.isReadOnly())
  },
  handleValue: function() {
    this.$el
      .find('.input-group.date')
      .data('DateTimePicker')
      .date(user.getUserReadableDateTime(this.model.getValue()))
  },
  focus: function() {
    this.$el.find('input').select()
  },
  handleOpen: function() {
    this.updatePosition()
    this.addResizeHandler()
  },
  updatePosition: function() {
    let datepicker = $('body').find('.bootstrap-datetimepicker-widget:last')
    let inputCoordinates = this.$el
      .find('.input-group.date')[0]
      .getBoundingClientRect()
    let top = datepicker.hasClass('bottom')
      ? inputCoordinates.top + inputCoordinates.height
      : inputCoordinates.top - datepicker.outerHeight()
    datepicker.css({
      top: top + 'px',
      bottom: 'auto',
      left: inputCoordinates.left + 'px',
      width: inputCoordinates.width + 'px',
    })
  },
  addResizeHandler: function() {
    $(window).on('resize.datePicker', this.updatePosition.bind(this))
  },
  removeResizeHandler: function() {
    $(window).off('resize.datePicker')
  },
  getCurrentValue: function() {
    let currentValue = this.$el.find('input').val()
    if (currentValue) {
      return moment
        .tz(currentValue, getDateFormat(), getTimeZone())
        .toISOString()
    } else {
      return null
    }
  },
  listenForChange: function() {
    this.$el.on(
      'dp.change',
      function(e) {
        if (e.oldDate === null) {
          return
        }

        let datetimepicker = this.$el
          .find('.input-group.date')
          .data('DateTimePicker')

        let newValue = this.getCurrentValue()

        if (e.type === 'dp') {
          newValue = this.hasSameTime(e.date, e.oldDate)
            ? e.date.startOf('day')
            : e.date
        }

        newValue = moment
          .tz(newValue, getDateFormat(), getTimeZone())
          .format(getDateFormat())

        datetimepicker.viewDate(newValue)
        this.$el.find('input').val(newValue)
        this.validate()
      }.bind(this)
    )
    this.$el.on(
      'dp.change click input change keyup',
      function(e) {
        this.validate()
      }.bind(this)
    )
  },
  isValid: function() {
    let currentValue = this.$el.find('input').val()
    return currentValue != null && currentValue !== ''
  },
  onDestroy: function() {
    let datetimepicker = this.$el
      .find('.input-group.date')
      .data('DateTimePicker')
    if (datetimepicker) {
      datetimepicker.destroy()
    }
  },
})
