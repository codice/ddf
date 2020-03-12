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
import { getFilteredAttributeList } from './filterHelper'
import EnumInput from '../inputs/enum-input'

const Root = styled.div`
  display: inline-block;
  vertical-align: middle;
  margin-right: ${({ theme }) => theme.minimumSpacing};
`

const FilterAttributeDropdown = ({
  onChange,
  includedAttributes,
  editing,
  value,
  supportedAttributes,
}) => {
  return (
    <Root>
      {editing ? (
        <EnumInput
          value={value}
          suggestions={getFilteredAttributeList(includedAttributes)}
          onChange={onChange}
          supportedAttributes={supportedAttributes}
        />
      ) : (
        value
      )}
    </Root>
  )
}

export default FilterAttributeDropdown
