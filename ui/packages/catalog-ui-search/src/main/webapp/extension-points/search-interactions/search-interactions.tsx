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
import { hot } from 'react-hot-loader'
import ExtensionPoints from '../extension-points'
const { Menu, MenuItem } = require('../../react-component/menu')

import SearchInteractionsContainer from './search-interactions.container'
import {
  Props as PresentationProps,
  CustomSearchFormDropdown,
  Divider,
  SearchFormMenuItem,
  ResetMenuItem,
} from './search-interactions.presentation'

export type SearchInteractionProps = {
  model: any
  onClose: () => void
}

const SearchInteractions = (props: SearchInteractionProps) => (
  <SearchInteractionsContainer model={props.model} onClose={props.onClose}>
    {(props: PresentationProps) => {
      return (
        <Menu onChange={(formId: string) => props.triggerQueryForm(formId)}>
          {ExtensionPoints.queryForms.map(form => {
            return (
              <SearchFormMenuItem
                key={form.id}
                value={form.id}
                title={form.title}
                selected={props.model.get('type') === form.id}
              />
            )
          })}
          <Divider />
          <MenuItem
            key={'formSelector'}
            value={'formSelector'}
            title="Change the form used to construct the search."
            data-help="Change the form used to construct the search."
            onClick={() => {}}
          >
            <CustomSearchFormDropdown model={props.model} />
          </MenuItem>
          <Divider />
          <ResetMenuItem value="reset" onClick={() => props.triggerReset()} />
        </Menu>
      )
    }}
  </SearchInteractionsContainer>
)

export default hot(module)(SearchInteractions)
