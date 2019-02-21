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

import { hot } from 'react-hot-loader'
import * as React from 'react'
import styled from '../../styles/styled-components'
import { Button, buttonTypeEnum } from '../button'
import BlacklistItemContainer from '../../container/blacklist-item'

/* TODO: Add back in css transitions */

const Root = styled<Props, 'div'>('div')`
  > button {
    width: 100%;
  }
  .blacklist-empty {
    white-space: normal;
    padding: ${props => props.theme.minimumSpacing};
    text-align: center;
  }
`

type Props = {
  clearBlacklist: () => void
  blacklist: Backbone.Collection<Backbone.Model>
}

const render = (props: Props) => {
  return (
    <Root {...props}>
      {props.blacklist.length !== 0 ? (
        [
          <Button
            icon="fa fa-eye"
            buttonType={buttonTypeEnum.neutral}
            onClick={props.clearBlacklist}
            text="Unhide All"
          />,
          <div className="is-list has-list-highlighting">
            {props.blacklist.map(item => {
              return <BlacklistItemContainer key={item.id} item={item} />
            })}
          </div>,
        ]
      ) : (
        <div className="blacklist-empty is-large-font">Nothing hidden.</div>
      )}
    </Root>
  )
}

export default hot(module)(render)
