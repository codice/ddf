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
import TextField from '../../../text-field'
import styled from 'styled-components'

const Input = styled(TextField)`
  width: ${({ theme }) => `calc(8*${theme.mediumSpacing})`};
`
const floatRegex = /^-?\d*(\.\d*)?$|^$/

const isFloat = value => {
  return value.match(floatRegex)
}

const getNumber = value => {
  const num = parseFloat(value)
  return isNaN(num) ? '' : num
}

export const FloatInput = props => {
  const [value, setValue] = useState(getNumber(props.value))

  useEffect(
    () => {
      props.onChange(getNumber(value))
    },
    [value]
  )

  return (
    <Input
      value={value}
      onChange={newVal => {
        if (isFloat(newVal)) {
          setValue(newVal)
        }
      }}
      className={props.className}
    />
  )
}
export default FloatInput
