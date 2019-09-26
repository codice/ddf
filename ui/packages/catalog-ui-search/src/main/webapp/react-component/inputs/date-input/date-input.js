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
import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { Button, buttonTypeEnum } from '../../presentation/button'
import Dropdown from '../../dropdown'
import DateTimePicker from './datepicker'
import TextField from '../../text-field'
import { formatDate, parseInput } from './dateHelper'
import moment from 'moment-timezone'

const Input = styled(TextField)`
  display: inline-block;
  height: ${({ theme }) => theme.minimumButtonSize};
  width: ${({ theme }) => `calc(17 * ${theme.mediumFontSize})`};
`

const CalendarButton = styled(Button)`
  border-radius: ${({ theme }) => theme.borderRadius};
  font-size: ${({ theme }) => theme.largeFontSize};
  height: ${({ theme }) =>
    `calc(${theme.minimumButtonSize} + ${theme.borderRadius})`};
`

const Anchor = styled.div`
  display: flex;
  flex-direction: row;
`

const DateInput = props => {
  const [value, setValue] = useState(moment(props.value || ''))
  const [input, setInput] = useState('')

  useEffect(
    () => {
      props.onChange(value)
      setInput(formatDate(value, props.timeZone, props.format))
    },
    [value, props.format, props.timeZone]
  )

  return (
    <Dropdown
      anchor={
        <Anchor>
          <Input
            onBlur={() =>
              setValue(parseInput(input, props.timeZone, props.format, value))
            }
            onChange={setInput}
            value={input}
            type="text"
            placeholder={props.placeholder || props.format}
            onClick={e => e.stopPropagation()}
          />
          <CalendarButton
            buttonType={buttonTypeEnum.primary}
            icon="fa fa-calendar"
          />
        </Anchor>
      }
    >
      <DateTimePicker
        value={value}
        onChange={setValue}
        format={props.format}
        timeZone={props.timeZone}
      />
    </Dropdown>
  )
}

export default DateInput
