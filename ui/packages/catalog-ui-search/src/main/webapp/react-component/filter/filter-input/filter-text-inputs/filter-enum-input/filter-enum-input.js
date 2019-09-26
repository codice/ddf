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
import EnumInput from '../../../../inputs/enum-input'
import { deserializeValue } from '../textFilterHelper'
import styled from 'styled-components'

const Root = styled.div`
  min-width: ${({ theme }) => `calc(19*${theme.minimumFontSize})`};
`

const FilterEnumInput = props => {
  const [value, setValue] = useState(deserializeValue(props.value))

  useEffect(
    () => {
      props.onChange(value)
    },
    [value]
  )

  return (
    <Root>
      <EnumInput
        allowCustom
        matchCase={props.matchCase}
        suggestions={props.suggestions}
        onChange={setValue}
        value={value}
      />
    </Root>
  )
}

export default FilterEnumInput
