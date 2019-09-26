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
import React, { useEffect, useRef } from 'react'
import $ from 'jquery'
import styled from 'styled-components'
import moment from 'moment-timezone'
import { findDOMNode } from 'react-dom'

/* Dependent on the eonasdan-bootstrap-datetimepicker dependency */

const Root = styled.div`
  border-radius: ${({ theme }) => theme.borderRadius};
  overflow: hidden;
  margin: auto;
`

function isSameDay(date1, date2) {
  return (
    date1.year() === date2.year() && date1.dayOfYear() === date2.dayOfYear()
  )
}

const DateTimePicker = props => {
  const datePicker = useRef(null)

  useEffect(() => {
    const value = props.value && props.value.isValid() ? props.value : moment()
    const element = findDOMNode(datePicker.current)
    $(element).datetimepicker({
      format: props.format,
      timeZone: props.timeZone,
      keyBinds: null,
      inline: true,
      defaultDate: value,
    })
    $(element).on('dp.change', e => {
      if (isSameDay(e.oldDate, e.date)) {
        props.onChange(e.date)
      } else {
        props.onChange(e.date.startOf('day'))
      }
    })
    props.onChange(value)
  }, [])

  return <Root ref={datePicker} />
}

export default DateTimePicker
