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
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
const DropdownModel = require('../../component/dropdown/dropdown.js')

const FilterComparatorDropdownView = require('../../component/dropdown/filter-comparator/dropdown.filter-comparator.view.js')
import styled from 'styled-components'

const Root = styled.div`
  display: inline-block;
  vertical-align: middle;
  margin-right: ${({ theme }) => theme.minimumSpacing};
  height: ${({ theme }) => theme.minimumButtonSize};
  line-height: ${({ theme }) => theme.minimumButtonSize};
`

const FilterComparator = ({ comparator, modelForComponent, editing }) => {
  const filterDropdownModel = new DropdownModel({
    value: comparator,
  })
  const component = new FilterComparatorDropdownView({
    model: filterDropdownModel,
    modelForComponent: modelForComponent,
  })
  if (editing) {
    component.turnOnEditing()
  } else {
    component.turnOffEditing()
  }
  return (
    <Root
      data-help="How to compare the value for this property against
        the provided value."
    >
      <MarionetteRegionContainer view={component} />
    </Root>
  )
}

export default FilterComparator
