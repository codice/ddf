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
import { hot } from 'react-hot-loader'

import withListenTo, { WithBackboneProps } from '../backbone-container'

import View from './presentation'

const moment = require('moment')
const momentTimezone = require('moment-timezone')
const Common = require('../../js/Common')
const Property = require('../../component/property/property')
const user = require('../../component/singletons/user-instance')

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

enum TimeZoneSign {
  POSITIVE = '+',
  NEGATIVE = '-',
}

const getTimeZoneFor = (sign: TimeZoneSign, value: number) => {
  if (sign === TimeZoneSign.POSITIVE) return Common.getTimeZones()[value]

  return Common.getTimeZones()[`${sign}${value}`]
}

const generateTimeZones = (sign: TimeZoneSign, rangeLimit: number) =>
  Array(rangeLimit)
    .fill(rangeLimit)
    .map((_: any, index: number) => ({
      label: `${sign}${index + 1}:00`,
      value: getTimeZoneFor(sign, index + 1),
    }))

const TimeZones = [
  ...generateTimeZones(TimeZoneSign.POSITIVE, 12),
  {
    label: 'UTC, +00:00',
    value: Common.getTimeZones()['UTC'],
  },
  ...generateTimeZones(TimeZoneSign.NEGATIVE, 12),
]

type UserPreferences = {
  get: (key: string) => any
  set: ({}) => void
  savePreferences: () => void
}

type State = {
  currentTime: string
  timeFormatModel: Model
  timeZoneModel: Model
}

type Model = {
  get: (key: string) => any
}

const getUserPreferences = (): UserPreferences =>
  user.get('user').get('preferences')

const savePreferences = (model: {}) => {
  const nullOrUndefinedValues = !Object.values(model).every(value => !!value)
  if (nullOrUndefinedValues) return

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

class TimeSettingsContainer extends React.Component<WithBackboneProps, State> {
  timer: any
  constructor(props: WithBackboneProps) {
    super(props)

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

    this.state = {
      currentTime: getCurrentTime(),
      timeFormatModel,
      timeZoneModel,
    }
  }

  componentDidMount = () => {
    this.props.listenTo(
      this.state.timeFormatModel,
      'change:value',
      (model: Model) =>
        savePreferences({
          dateTimeFormat: model.get('value').shift(),
        })
    )
    this.props.listenTo(
      this.state.timeZoneModel,
      'change:value',
      (model: Model) =>
        savePreferences({ timeZone: model.get('value').shift() })
    )

    const updateCurrentTimeClock = () =>
      this.setState({ currentTime: getCurrentTime() })

    this.timer = setInterval(updateCurrentTimeClock, 50)
  }

  componentWillUnmount = () => clearInterval(this.timer)

  render = () => (
    <View
      {...this.props}
      timeZoneModel={this.state.timeZoneModel}
      timeFormatModel={this.state.timeFormatModel}
      currentTime={this.state.currentTime}
    />
  )
}

export default hot(module)(withListenTo(TimeSettingsContainer))
