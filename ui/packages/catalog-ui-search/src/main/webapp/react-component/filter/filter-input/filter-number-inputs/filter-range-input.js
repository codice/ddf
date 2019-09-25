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
import React from 'react'
import styled from 'styled-components'
import IntegerInput from './filter-integer-input'
import FloatInput from './filter-float-input'

const Float = styled(FloatInput)`
  width: ${({ theme }) => `calc(4*${theme.mediumSpacing})`};
`

const Integer = styled(IntegerInput)`
  width: ${({ theme }) => `calc(4*${theme.mediumSpacing})`};
`

const Root = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
`

const Label = styled.div`
  padding: 0 ${({ theme }) => theme.minimumSpacing};
  font-weight: bolder;
`

const serialize = (lower, upper) => ({ lower, upper })

const FilterRangeInput = ({ isInteger, onChange, value }) => {
  const Input = isInteger ? Integer : Float
  return (
    <Root>
      <Input
        value={value.lower}
        onChange={lower => onChange(serialize(lower, value.upper))}
      />
      <Label>TO</Label>
      <Input
        value={value.upper}
        onChange={upper => onChange(serialize(value.lower, upper))}
      />
    </Root>
  )
}

export default FilterRangeInput
