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
import styled from '../../styles/styled-components'

export const Divider = () => <div className="is-divider composed-menu" />

const InteractionIcon = styled.div`
  text-align: center;
  width: ${props => props.theme.minimumButtonSize};
  display: inline-block;
  line-height: ${props => props.theme.minimumButtonSize};
  height: ${props => props.theme.minimumButtonSize};
`

const InteractionText = styled.div`
  line-height: ${props => props.theme.minimumButtonSize};
  height: ${props => props.theme.minimumButtonSize};
  display: inline-block;
`

const Interaction = styled.div`
  line-height: ${props => props.theme.minimumButtonSize};
  height: ${props => props.theme.minimumButtonSize};
  white-space: nowrap;
  padding: ${props => `0px ${props.theme.minimumSpacing}`};
  cursor: pointer;
  overflow: hidden;
`

type MetacardInteractionProps = {
  help: string
  icon: string
  text: string
  onClick: (props: any) => void
  children?: any
}

export const MetacardInteraction = (props: MetacardInteractionProps) => {
  return (
    <Interaction data-help={props.help} onClick={() => props.onClick(props)}>
      <InteractionIcon className={props.icon} />
      <InteractionText>{props.text}</InteractionText>
      {props.children}
    </Interaction>
  )
}
