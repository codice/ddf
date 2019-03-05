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
import { GrabCursor, IsButton, HighlightBehavior } from '../../styles/mixins'
import { Order, Visibility, Actions } from '../../container/layer-item'

type Props = {
  name: string
  order: Order
  visibility: Visibility
  actions: Actions
}

const Root = styled.div`
  display: block;
  white-space: nowrap;
  width: 100%;
  overflow: hidden;
  position: relative;
`

const RearrangeButton = styled.button`
  ${props => IsButton(props.theme)};
  ${HighlightBehavior({ initialOpacity: 0 })};
  z-index: 1;
  position: absolute;
  left: 0px;
  margin-left: 0px;
  height: calc(0.5 * ${props => props.theme.minimumButtonSize});
  line-height: calc(0.5 * ${props => props.theme.minimumButtonSize});
`
const RearrangeDown = styled(RearrangeButton)`
  top: calc(100% - 0.5 * ${props => props.theme.minimumButtonSize});
`
const RearrangeUp = styled(RearrangeButton)`
  top: 0px;
`
const RearrangeIcon = styled.span`
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translateX(-50%) translateY(-50%);
`

const DragButton = styled.button`
  ${props => IsButton(props.theme)};
  ${GrabCursor};
  vertical-align: middle;
  position: absolute;
  left: 0px;
  margin-left: 0px;
  top: 0px;
  height: 100%;
`

const LayerPropertiesRoot = styled.div`
  display: inline-block;
  vertical-align: middle;
  padding: 0 ${props => props.theme.mediumSpacing};
  margin-left: ${props => props.theme.minimumButtonSize};
  width: calc(100% - ${props => props.theme.minimumButtonSize});
`

const LayerName = styled.div`
  line-height: ${props => props.theme.minimumButtonSize};
  overflow: hidden;
  text-overflow: ellipsis;
`

const LayerAlpha = styled.input`
  display: inline-block !important;
  vertical-align: middle;
`

const LayerInteractions = styled.div`
  text-align: right;
`

const LayerInteractionsButton = styled.button`
  ${props => IsButton(props.theme)};
  width: ${props => props.theme.minimumButtonSize};
  height: ${props => props.theme.minimumButtonSize};
  vertical-align: top;
`

const LayerWarning = styled(LayerInteractionsButton)`
  display: none;
`

const LayerRemove = styled(LayerInteractionsButton)`
  display: none;
`
const LayerShow = styled(LayerInteractionsButton)`
  position: relative;
  display: inline-block !important;
  vertical-align: middle;
`

const LayerShowIcon = styled.span`
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translateX(-50%) translateY(-50%);
`
const Faeye = styled(LayerShowIcon)`
  display: none;
`
const Faeyeslash = styled(LayerShowIcon)`
  display: inline;
`

const render = (props: Props) => {
  const { name } = props
  return (
    <Root>
      <RearrangeUp>
        <RearrangeIcon className="fa fa-angle-up" />
      </RearrangeUp>
      <RearrangeDown>
        <RearrangeIcon className="fa fa-angle-down" />
      </RearrangeDown>
      <DragButton>
        <span className="fa fa-arrows-v" />
      </DragButton>

      <LayerPropertiesRoot>
        <LayerName title={name}>Layer</LayerName>

        <div className="layer-visibility">
          <LayerAlpha min="0" max="1" step="0.01" type="range" />
        </div>
        <LayerInteractions>
          <LayerWarning data-help="View map layer warnings." title="warning">
            <span className=" fa fa-warning" />
          </LayerWarning>
          <LayerRemove
            data-help="Remove map layer from user preferences."
            title="Remove map layer from user preferences."
          >
            <span className="fa fa-minus" />
          </LayerRemove>
          <LayerShow
            data-help="Toggle layer visibility."
            title="Toggle layer visibility."
          >
            <Faeye className="fa fa-eye" />
            <Faeyeslash className="fa fa-eye-slash" />
          </LayerShow>
        </LayerInteractions>
      </LayerPropertiesRoot>
    </Root>
  )
}

export default hot(module)(render)
