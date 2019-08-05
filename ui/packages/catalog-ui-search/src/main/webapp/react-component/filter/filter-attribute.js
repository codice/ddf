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
import withListenTo from '../../react-component/container/backbone-container'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import styled from 'styled-components'
import { getFilteredAttributeList } from './filterHelper'
const DropdownView = require('../../component/dropdown/dropdown.view.js')

const Root = styled.div`
  display: inline-block;
  vertical-align: middle;
  margin-right: ${({ theme }) => theme.minimumSpacing};
  height: ${({ theme }) => theme.minimumButtonSize};
  line-height: ${({ theme }) => theme.minimumButtonSize};
  intrigue-dropdown.is-editing .dropdown-text {
    width: auto !important;
    max-width: 300px;
  }
`

const areEqual = (prevProps, nextProps) => {
  return prevProps.editing === nextProps.editing
}

const Component = React.memo(props => {
  const component = DropdownView.createSimpleDropdown({
    list: getFilteredAttributeList(props.includedAttributes),
    defaultSelection: [props.attribute],
    hasFiltering: true,
  })
  props.listenTo(component.model, 'change:value', () => {
    props.onChange(component.model.get('value')[0])
  })
  if (props.editing) {
    component.turnOnEditing()
  } else {
    component.turnOffEditing()
  }
  return (
    <Root data-help="Property to compare against">
      <MarionetteRegionContainer view={component} />
    </Root>
  )
}, areEqual)

export default withListenTo(Component)
