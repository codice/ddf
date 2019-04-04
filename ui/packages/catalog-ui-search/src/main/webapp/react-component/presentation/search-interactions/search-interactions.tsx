/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import * as React from 'react'
import MenuAction from '../menu-action'
import styled from '../../styles/styled-components'
import { readableColor } from 'polished'
import { hot } from 'react-hot-loader'

type Props = {
  id: string
  editSearch: () => void
  deleteSearch: (id: string) => void
  runSearch: () => void
}

const Root = styled<{}, 'div'>('div')`
  width: 100%;
  color: ${props => readableColor(props.theme.background)};
`

const render = (props: Props) => {
  const { id, editSearch, deleteSearch, runSearch } = props
  return (
    <Root className="composed-menu">
      <MenuAction
        help="Edit your search"
        icon="fa fa-pencil"
        onClick={(_e, context) => {
          editSearch()
          context.closeAndRefocus()
        }}
      >
        Edit
      </MenuAction>
      <MenuAction
        help="Delete your search"
        icon="fa fa-trash-o"
        onClick={(_e, context) => {
          deleteSearch(id)
          context.closeAndRefocus()
        }}
      >
        Delete
      </MenuAction>
      <MenuAction
        help="Run your search"
        icon="fa fa-play"
        onClick={(_e, context) => {
          runSearch()
          context.closeAndRefocus()
        }}
      >
        Run
      </MenuAction>
    </Root>
  )
}

export default hot(module)(render)
