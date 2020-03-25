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
import { useState, useEffect } from 'react'
import styled from 'styled-components'
import { useBackbone } from './useBackbone.hook'

const Root = styled.div`
  display: inline-block;
  width: auto;
  cursor: pointer;
`

const isSelected = ({ selectionInterface }: any) => {
  const currentResults = selectionInterface.getActiveSearchResults()
  const selectedResults = selectionInterface.getSelectedResults()
  return (
    currentResults.length > 0 &&
    selectedResults.length >= currentResults.length &&
    currentResults.every(
      (currentResult: any) => selectedResults.get(currentResult) !== undefined
    )
  )
}

const getClassName = ({ selectionInterface }: any) => {
  return isSelected({ selectionInterface })
    ? 'fa fa-check-square-o'
    : 'fa fa-square-o'
}

export const ItemCheckboxAll = ({ selectionInterface }: any) => {
  const [className, setClassName] = useState(
    getClassName({ selectionInterface })
  )

  const { listenTo } = useBackbone()
  useEffect(() => {
    listenTo(
      selectionInterface.getSelectedResults(),
      'update add remove reset',
      () => {
        setClassName(getClassName({ selectionInterface }))
      }
    )
    listenTo(
      selectionInterface.getActiveSearchResults(),
      'update add remove reset',
      () => {
        setClassName(getClassName({ selectionInterface }))
      }
    )
  }, [])

  return (
    <Root
      className={'checkbox-container'}
      onClick={e => {
        e.stopPropagation()
        if (isSelected({ selectionInterface })) {
          selectionInterface.clearSelectedResults()
        } else {
          const currentResults = selectionInterface.getActiveSearchResults()
          selectionInterface.setSelectedResults(currentResults.models)
        }
      }}
    >
      <span className={className} />
    </Root>
  )
}
