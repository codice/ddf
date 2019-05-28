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
const template = require('./time-settings.hbs')
const user = require('../singletons/user-instance.js')
const CustomElements = require('../../js/CustomElements.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const Common = require('../../js/Common.js')
const moment = require('moment')

let counter = 0

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('time-settings'),
  regions: {
    propertyTimeZone: '.property-time-zone',
    propertyTimeFormat: '.property-time-format',
    propertyTimeCurrent: '.property-time-current',
  },
  animationFrameId: undefined,
  onBeforeShow() {
    this.setupTimeZone()
    this.setupResultCount()
    this.setupCurrentTime()
    this.repaintCurrentTime()
  },
  repaintCurrentTime() {
    this.animationFrameId = window.requestAnimationFrame(() => {
      if (counter % 5 === 0) {
        this.setupCurrentTime()
      }
      counter++
      this.repaintCurrentTime()
    })
  },
  setupCurrentTime() {
    this.propertyTimeCurrent.show(
      new PropertyView({
        model: new Property({
          label: 'Current Time (example)',
          value: [moment()],
          type: 'DATE',
        }),
      })
    )
  },
  setupTimeZone() {
    const timeZone = user
      .get('user')
      .get('preferences')
      .get('timeZone')

    this.propertyTimeZone.show(
      new PropertyView({
        model: new Property({
          label: 'Time Zone',
          value: [timeZone],
          enum: [
            {
              label: '-12:00',
              value: Common.getTimeZones()['-12'],
            },
            {
              label: '-11:00',
              value: Common.getTimeZones()['-11'],
            },
            {
              label: '-10:00',
              value: Common.getTimeZones()['-10'],
            },
            {
              label: '-09:00',
              value: Common.getTimeZones()['-9'],
            },
            {
              label: '-08:00',
              value: Common.getTimeZones()['-8'],
            },
            {
              label: '-07:00',
              value: Common.getTimeZones()['-7'],
            },
            {
              label: '-06:00',
              value: Common.getTimeZones()['-6'],
            },
            {
              label: '-05:00',
              value: Common.getTimeZones()['-5'],
            },
            {
              label: '-04:00',
              value: Common.getTimeZones()['-4'],
            },
            {
              label: '-03:00',
              value: Common.getTimeZones()['-3'],
            },
            {
              label: '-02:00',
              value: Common.getTimeZones()['-2'],
            },
            {
              label: '-01:00',
              value: Common.getTimeZones()['-1'],
            },
            {
              label: 'UTC, +00:00',
              value: Common.getTimeZones()['UTC'],
            },
            {
              label: '+01:00',
              value: Common.getTimeZones()['1'],
            },
            {
              label: '+02:00',
              value: Common.getTimeZones()['2'],
            },
            {
              label: '+03:00',
              value: Common.getTimeZones()['3'],
            },
            {
              label: '+04:00',
              value: Common.getTimeZones()['4'],
            },
            {
              label: '+05:00',
              value: Common.getTimeZones()['5'],
            },
            {
              label: '+06:00',
              value: Common.getTimeZones()['6'],
            },
            {
              label: '+07:00',
              value: Common.getTimeZones()['7'],
            },
            {
              label: '+08:00',
              value: Common.getTimeZones()['8'],
            },
            {
              label: '+09:00',
              value: Common.getTimeZones()['9'],
            },
            {
              label: '+10:00',
              value: Common.getTimeZones()['10'],
            },
            {
              label: '+11:00',
              value: Common.getTimeZones()['11'],
            },
            {
              label: '+12:00',
              value: Common.getTimeZones()['12'],
            },
          ],
        }),
      })
    )

    this.propertyTimeZone.currentView.turnOnEditing()
    this.listenTo(
      this.propertyTimeZone.currentView.model,
      'change:value',
      this.save
    )
  },
  setupResultCount() {
    const timeFormat = user
      .get('user')
      .get('preferences')
      .get('dateTimeFormat')

    this.propertyTimeFormat.show(
      new PropertyView({
        model: new Property({
          label: 'Time Format',
          value: [timeFormat],
          enum: [
            {
              label: 'ISO 8601',
              value: Common.getDateTimeFormats()['ISO'],
            },
            {
              label: '24 Hour Standard',
              value: Common.getDateTimeFormats()['24'],
            },
            {
              label: '12 Hour Standard',
              value: Common.getDateTimeFormats()['12'],
            },
          ],
        }),
      })
    )

    this.propertyTimeFormat.currentView.turnOnEditing()
    this.listenTo(
      this.propertyTimeFormat.currentView.model,
      'change:value',
      this.save
    )
  },
  save() {
    const preferences = user.get('user').get('preferences')
    preferences.set({
      dateTimeFormat: this.propertyTimeFormat.currentView.model.getValue()[0],
      timeZone: this.propertyTimeZone.currentView.model.getValue()[0],
    })
    preferences.savePreferences()
  },
  onDestroy() {
    window.cancelAnimationFrame(this.animationFrameId)
  },
})
