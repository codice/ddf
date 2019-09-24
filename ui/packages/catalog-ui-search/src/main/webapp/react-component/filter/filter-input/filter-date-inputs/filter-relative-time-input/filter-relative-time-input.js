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
import RelativeTimeInput from '../../../../inputs/relative-time-input'
import { deserialize, serialize } from './relativeTimeHelper'
import styled from 'styled-components'

const Component = styled(RelativeTimeInput)`
  width: ${({ theme }) => `calc(14*${theme.mediumSpacing})`};
`

const FilterRelativeTimeInput = props => {
  const [value, setValue] = useState(
    deserialize(props.value) || { last: '', unit: '' }
  )
  useEffect(
    () => {
      props.onChange(serialize(value))
    },
    [value]
  )
  return <Component last={value.last} unit={value.unit} onChange={setValue} />
}

export default FilterRelativeTimeInput
