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
import * as React from 'react'
import styled from 'styled-components'
import { HTMLAttributes } from '../../../react-component/html'
import NumberInput from './number-input'
import { DECIMAL_DEGREES_PRECISION } from '../coordinate-converter'

type Props = HTMLAttributes & {
  /** Decimal value */
  value: number
  /** Called on change */
  onChange: (value: number) => void
}
const DegreeInput = styled(NumberInput)`
  width: 8rem;
`
const LatitudeInput: React.SFC<Props> = props => (
  <DegreeInput
    maxValue={90}
    minValue={-90}
    decimalPlaces={DECIMAL_DEGREES_PRECISION}
    {...props}
  />
)
const LongitudeInput: React.SFC<Props> = props => (
  <DegreeInput
    maxValue={180}
    minValue={-180}
    decimalPlaces={DECIMAL_DEGREES_PRECISION}
    {...props}
  />
)

export { LatitudeInput, LongitudeInput }
