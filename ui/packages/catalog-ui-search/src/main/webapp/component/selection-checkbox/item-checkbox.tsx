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

const isSelected = ({ selectionInterface, model }: any) => {
  const selectedResults = selectionInterface.getSelectedResults()
  return Boolean(selectedResults.get(model.id))
}

const getClassName = ({ selectionInterface, model }: any) => {
  return isSelected({ selectionInterface, model })
    ? 'fa fa-check-square-o'
    : 'fa fa-square-o'
}

export const ItemCheckbox = ({ selectionInterface, model }: any) => {
  const [className, setClassName] = useState(
    getClassName({ selectionInterface, model })
  )

  const { listenTo } = useBackbone()
  useEffect(() => {
    listenTo(
      selectionInterface.getSelectedResults(),
      'update add remove reset',
      () => {
        setClassName(getClassName({ selectionInterface, model }))
      }
    )
  }, [])

  return (
    <Root
      onClick={e => {
        e.stopPropagation()
        if (isSelected({ selectionInterface, model })) {
          selectionInterface.removeSelectedResult(model)
        } else {
          selectionInterface.addSelectedResult(model)
        }
      }}
    >
      <span className={className} />
    </Root>
  )
}
