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
import UnitsDropdown from './units-dropdown'
import TextField from '../../text-field'

const Label = styled.div`
  font-weight: bolder;
`

const InputContainer = styled.div`
  margin-bottom: ${({ theme }) => theme.minimumSpacing};
`

const LastInput = styled(TextField)`
  width: ${({ theme }) => `calc(8*${theme.mediumSpacing})`};
`

const serialize = (last, unit) => ({ last, unit })

const RelativeTime = props => {
  return (
    <div>
      <InputContainer>
        <Label>Last</Label>
        <LastInput
          type="number"
          value={props.last}
          onChange={value => props.onChange(serialize(value, props.unit))}
        />
      </InputContainer>
      <InputContainer>
        <Label>Units</Label>
        <UnitsDropdown
          value={props.unit}
          onChange={value => props.onChange(serialize(props.last, value))}
        />
      </InputContainer>
    </div>
  )
}

export default RelativeTime
