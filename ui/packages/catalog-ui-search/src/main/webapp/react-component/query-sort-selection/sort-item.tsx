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
// @ts-ignore
import EnumInput from '../inputs/enum-input'
import { isDirectionalSort } from './sort-selection-helpers'
import { SortItemType, Option } from './sort-selections'

type Props = {
  sortItem: SortItemType
  attributeOptions: Option[]
  directionOptions: Option[]
  updateAttribute: (attribute: string) => void
  updateDirection: (direction: string) => void
  onRemove: () => void
  showRemove?: boolean
}

const RemoveSortButton = styled.button`
  height: ${props => props.theme.minimumButtonSize};
  width: ${props => props.theme.minimumButtonSize};
`

const SortButtonContainer = styled.div`
  display: inline-block;
  margin-right: 1.5rem;
  position: absolute;
  right: 0px;
  top: 50%;
  transform: translateY(-50%);
`

const SortInputContainer = styled.div`
  display: block;
  width: 100%;
  white-space: normal;
  position: relative;
  padding: 0.625rem 1.5rem;
`

const SortItem = ({
  sortItem,
  attributeOptions,
  directionOptions,
  updateAttribute,
  updateDirection,
  onRemove,
  showRemove,
}: Props) => (
  <>
    <div
      className={`sort-properties ${
        showRemove ? 'global-bracket is-right' : ''
      }`}
    >
      <SortInputContainer>
        <EnumInput
          value={sortItem.attribute.label}
          suggestions={attributeOptions}
          onChange={updateAttribute}
        />
      </SortInputContainer>
      {isDirectionalSort(sortItem.attribute.value) && (
        <SortInputContainer>
          <EnumInput
            value={sortItem.direction}
            suggestions={directionOptions}
            onChange={updateDirection}
          />
        </SortInputContainer>
      )}
    </div>
    {showRemove && (
      <SortButtonContainer>
        <RemoveSortButton className="is-negative" onClick={onRemove}>
          <span className="fa fa-minus" />
        </RemoveSortButton>
      </SortButtonContainer>
    )}
  </>
)

export default SortItem
