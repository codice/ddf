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
import React, { useState, useEffect } from 'react'
import DateInput from '../../../inputs/date-input'
import withListenTo from '../../../backbone-container'
import user from '../../../../component/singletons/user-instance'
import { getTimeZone, getDateFormat } from './dateUtils'

const FilterDateInput = props => {
  const [timeZone, setTimeZone] = useState(getTimeZone())
  const [dateFormat, setDateFormat] = useState(getDateFormat())
  const [date, setDate] = useState(props.value)

  useEffect(() => {
    props.listenTo(user.getPreferences(), 'change:timeZone', () => {
      setTimeZone(getTimeZone())
    })

    props.listenTo(user.getPreferences(), 'change:dateTimeFormat', () => {
      setDateFormat(getDateFormat())
    })
  }, [])

  useEffect(
    () => {
      props.onChange(date)
    },
    [date]
  )

  return (
    <DateInput
      format={dateFormat}
      timeZone={timeZone}
      value={date}
      placeholder={props.placeholder}
      onChange={value => setDate(value.toISOString() || '')}
    />
  )
}

export default withListenTo(FilterDateInput)
