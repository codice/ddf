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
import React, { useState, useEffect, useRef } from 'react'
import TextField from '../../text-field'
import styled from 'styled-components'
import PropTypes from 'prop-types'

const Root = styled.div`
  min-width: ${({ theme }) => `calc(10 * ${theme.mediumFontSize})`};
  width: ${({ width, theme }) =>
    `calc(${width}px + 4*${theme.mediumFontSize})`};
  max-width: ${({ theme }) => `calc(45 * ${theme.mediumFontSize})`};
`

const Input = styled(TextField)`
  font-size: ${({ theme }) => theme.mediumFontSize};
  width: 100%;
`

const Ruler = styled.div`
  top: -9999px;
  left: -9999px;
  position: absolute;
  white-space: nowrap;
  font-size: ${({ theme }) => theme.mediumFontSize};
`

const TextInput = ({ onChange, placeholder, value }) => {
  const ref = useRef(null)
  const [width, setWidth] = useState(0)
  useEffect(
    () => {
      setWidth(ref.current.offsetWidth)
    },
    [value]
  )

  return (
    <Root width={width}>
      <Input value={value} placeholder={placeholder} onChange={onChange} />
      <Ruler ref={ref}>{value}</Ruler>
    </Root>
  )
}

TextInput.propTypes = {
  /** The current selected value. */
  value: PropTypes.string,

  /** Value change handler. */
  onChange: PropTypes.func.isRequired,
}

export default TextInput
