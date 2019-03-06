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
import styled from '../../styles/styled-components'
import { hot } from 'react-hot-loader'
import { Props, IsButton } from '.'

const Interactions = styled.div`
  text-align: right;
`

const InteractionsButton = styled.button`
  ${props => IsButton(props.theme)};
  width: ${props => props.theme.minimumButtonSize};
  height: ${props => props.theme.minimumButtonSize};
  vertical-align: top;
`

const Warning = styled(InteractionsButton)`
  display: none;
`

const Remove = styled(InteractionsButton)`
  display: none;
`
const Show = styled(InteractionsButton)`
  position: relative;
  display: inline-block !important;
  vertical-align: middle;
`

//const ShowIcon = styled<Props, 'span'>('span')`
const ShowIcon = styled.span`
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translateX(-50%) translateY(-50%);
  display: inline;
`

const ContentShow = ({ visibility }: Props) => {
  const className = `fa ${visibility.show ? 'fa-eye' : 'fa-eye-slash'}`
  return <ShowIcon className={className} />
}

const render = (props: Props) => {
  const {updateLayerShow} = props.actions
  return (
    <Interactions>
      <Warning data-help="View map layer warnings." title="warning">
        <span className=" fa fa-warning" />
      </Warning>
      <Remove
        data-help="Remove map layer from user preferences."
        title="Remove map layer from user preferences."
      >
        <span className="fa fa-minus" />
      </Remove>
      <Show
        data-help="Toggle layer visibility."
        title="Toggle layer visibility."
        onClick={updateLayerShow}
      >
        <ContentShow {...props} />
      </Show>
    </Interactions>
  )
}

export default hot(module)(render)
