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
import * as React from 'react'
import * as moment from 'moment'
const momentTimezone = require('moment-timezone')

import withListenTo, { WithBackboneProps } from '../backbone-container'

import View from '../../presentation/time-settings'

import * as Common from '../../../js/Common'
import * as Property from '../../../component/property/property'
import * as user from '../../../component/singletons/user-instance'

const TimeZones = [
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
]

const TimeFormat = [
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
]

type UserPreferences = {
  get: (key: string) => any
  set: ({}) => void
  savePreferences: () => void
}

const getUserPreferences = (): UserPreferences =>
  user.get('user').get('preferences')

const timeZoneModel = new Property({
  label: 'Time Zone',
  value: [getUserPreferences().get('timeZone')],
  enum: TimeZones,
})

const timeFormatModel = new Property({
  label: 'Time Format',
  value: [getUserPreferences().get('dateTimeFormat')],
  enum: TimeFormat,
})

const savePreferences = (model: {}) => {
  const preferences = getUserPreferences()

  preferences.set(model)

  preferences.savePreferences()
}

const getCurrentDateTimeFormat = () =>
  getUserPreferences().get('dateTimeFormat').datetimefmt
const getCurrentTimeZone = () => getUserPreferences().get('timeZone')

const getCurrentTime = (
  format: string = getCurrentDateTimeFormat(),
  timeZone: string = getCurrentTimeZone()
) => momentTimezone.tz(moment(), timeZone).format(format)

export default withListenTo(
  class extends React.Component<WithBackboneProps, { currentTime: string }> {
    timer: any
    constructor(props: WithBackboneProps) {
      super(props)

      this.state = { currentTime: getCurrentTime() }
    }

    componentDidMount = () => {
      this.props.listenTo(timeFormatModel, 'change:value', () =>
        savePreferences({
          dateTimeFormat: timeFormatModel.get('value').shift(),
        })
      )
      this.props.listenTo(timeZoneModel, 'change:value', () =>
        savePreferences({ timeZone: timeZoneModel.get('value').shift() })
      )

      const updateCurrentTimeClock = () =>
        this.setState({ currentTime: getCurrentTime() })

      this.timer = setInterval(updateCurrentTimeClock, 50)
    }

    componentWillUnmount = () => clearInterval(this.timer)

    render = () => (
      <View
        {...this.props}
        timeZoneModel={timeZoneModel}
        timeFormatModel={timeFormatModel}
        currentTime={this.state.currentTime}
      />
    )
  }
)
