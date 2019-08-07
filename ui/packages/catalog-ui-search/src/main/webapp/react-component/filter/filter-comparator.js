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
import Dropdown from '../dropdown'
import { Menu, MenuItem } from '../menu'

import {
  geometryComparators,
  dateComparators,
  stringComparators,
  numberComparators,
  booleanComparators,
} from '../../component/filter/comparators'

const Root = styled.div`
  display: inline-block;
  vertical-align: middle;
  margin-right: ${({ theme }) => theme.minimumSpacing};
  height: ${({ theme }) => theme.minimumButtonSize};
  line-height: ${({ theme }) => theme.minimumButtonSize};
  *:focus {
    outline: none;
  }
`

const AnchorRoot = styled.div`
  padding: 0 ${({ theme }) => theme.minimumSpacing};
`

const Anchor = props => (
  <AnchorRoot onClick={props.onClick}>
    <span style={{ display: 'inline-block' }}>
      {props.comparator}
      &nbsp;
    </span>
    <span style={{ display: 'inline-block' }} className="fa fa-caret-down" />
  </AnchorRoot>
)

const typeToComparators = {
  STRING: stringComparators,
  DATE: dateComparators,
  LONG: numberComparators,
  DOUBLE: numberComparators,
  FLOAT: numberComparators,
  INTEGER: numberComparators,
  SHORT: numberComparators,
  LOCATION: geometryComparators,
  GEOMETRY: geometryComparators,
  BOOLEAN: booleanComparators,
}

const ComparatorMenu = styled(Menu)`
  max-height: 50vh;
`

class FilterComparator extends React.Component {
  render() {
    if (!this.props.editing) {
      return <Root>{this.props.comparator}</Root>
    }
    let comparators = typeToComparators[this.props.type]
    if (
      this.props.attribute === 'anyGeo' ||
      this.props.attribute === 'anyText'
    ) {
      comparators = comparators.filter(comparator => comparator !== 'IS EMPTY')
    }

    return (
      <Root>
        <Dropdown anchor={<Anchor comparator={this.props.comparator} />}>
          <ComparatorMenu
            value={this.props.comparator}
            onChange={this.props.onChange}
          >
            {comparators.map(comparator => (
              <MenuItem
                style={{ paddingLeft: '2rem' }}
                value={comparator}
                key={comparator}
                title={comparator}
              />
            ))}
          </ComparatorMenu>
        </Dropdown>
      </Root>
    )
  }
}

export default FilterComparator
