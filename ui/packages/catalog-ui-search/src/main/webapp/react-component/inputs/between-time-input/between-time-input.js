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
import styled from 'styled-components'
import DateInput from '../date-input'

const Label = styled.span`
  font-weight: bolder;
`

const Root = styled.div`
  display: flex;
  flex-direction: column;
`

const InputContainer = styled.div`
  margin-bottom: ${({ theme }) => theme.mediumSpacing};
`

const BetweenTime = props => {
  const [from, setFrom] = useState(props.from)
  const [to, setTo] = useState(props.to)

  useEffect(
    () => {
      props.onChange({ from, to })
    },
    [from, to]
  )

  return (
    <Root>
      <InputContainer>
        <Label>From</Label>
        <DateInput
          placeholder="Limit search to after this time."
          value={from}
          onChange={setFrom}
          format={props.format}
          timeZone={props.timeZone}
        />
      </InputContainer>
      <InputContainer>
        <Label>To</Label>
        <DateInput
          placeholder="Limit search to before this time."
          value={to}
          onChange={setTo}
          format={props.format}
          timeZone={props.timeZone}
        />
      </InputContainer>
    </Root>
  )
}

export default BetweenTime
