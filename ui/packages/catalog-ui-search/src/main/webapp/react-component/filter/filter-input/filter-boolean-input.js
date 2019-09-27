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
import { readableColor, transparentize } from 'polished'

const BooleanSpan = styled.span`
  font-size: ${({ theme }) => theme.minimumFontSize};
  ${({ value, theme }) =>
    value ? `color: ${theme.primaryColor}` : null} font-weight: bolder;
`

const Root = styled.button`
  padding: 0 10px;
  background: ${({ theme }) =>
    transparentize(0.95, readableColor(theme.backgroundContent))};
`

const BooleanInput = props => {
  const [value, setValue] = useState(props.value === true)

  useEffect(
    () => {
      props.onChange(value)
    },
    [value]
  )

  return (
    <Root onClick={() => setValue(!value)}>
      <BooleanSpan value={value}>{value.toString()}</BooleanSpan>
    </Root>
  )
}

export default BooleanInput
