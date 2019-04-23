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
import { hot } from 'react-hot-loader'
import UnsavedIndicator from '../unsaved-indicator'

const StyledUnsavedIndicator = styled.span`
  display: inline-block;
`

type Props = {
  title?: string
  onChange: (value: string) => void
  saved: boolean
}

const Root = styled<{}, 'div'>('div')`
  width: 100%;
  height: 100%;
  position: relative;

  .title-display {
    display: inline-block;
    padding: 0px 18px 0px 10px;
    height: calc(100% - 4px);
    transition: padding ${props => props.theme.coreTransitionTime} ease-out;
    transition-delay: ${props => props.theme.coreTransitionTime};
    visibility: hidden;
    font-size: ${props => props.theme.largeFontSize};
    line-height: inherit;
    border: none;
    font-family: inherit;
    margin: 0px;
    overflow: hidden;
  }

  .title-display:empty::before {
    content: 'Workspace Title';
  }

  .title-saved {
    line-height: ~'calc(2*${props => props.theme.minimumLineSize} - 6px)';
    color: ${props => props.theme.warningColor};
    position: absolute;
    padding-right: 0px;
    height: calc(100% - 6px);
    right: 0px;
    top: 3px;
    background: ${props => props.theme.backgroundNavigation};
    transform: scale(1);
    opacity: 1;
    transition: padding ${props => props.theme.coreTransitionTime} ease-out
        ${props => props.theme.coreTransitionTime},
      transform ${props => props.theme.coreTransitionTime} ease-out,
      opacity ${props => props.theme.coreTransitionTime} ease-out;
  }

  input {
    color: inherit;
    padding-top: 3px;
    position: absolute;
    left: 1px;
    top: 2px;
    display: inline-block;
    vertical-align: top;
    height: calc(100% - 4px);
    min-width: 1px;
    background: ${props => props.theme.backgroundNavigation} !important;
    border: none !important;
    font-size: ${props => props.theme.largeFontSize};
    overflow: hidden;
    text-overflow: ellipsis;
    width: 100%;
    cursor: pointer;
    transition: padding ${props => props.theme.coreTransitionTime} ease-out;
    transition-delay: ${props => props.theme.coreTransitionTime};
    padding-right: 15px;
  }

  input:focus {
    cursor: text;
    text-decoration: none;
    padding-right: 25px;
    transition-delay: 0s;
  }

  input:focus + .title-display {
    padding: 0px 30px 0px 25px;
    transition-delay: 0s;
  }

  input:focus + .title-display + .title-saved {
    padding-right: 5px;
    transition-delay: 0s;
  }

  ${StyledUnsavedIndicator /* sc-selector */} {
    position: absolute;
    right: 5px;
    top: -5px;
  }
`

const render = (props: Props) => {
  const { title, onChange, saved } = props
  return (
    <Root>
      <input
        placeholder="Workspace Title"
        value={title}
        data-help="This is the title of the workspace you are currently in.
          If you have permission, you can click here to start editing the title."
        onChange={(e: React.FormEvent<HTMLInputElement>) => {
          onChange(e.currentTarget.value)
        }}
      />
      <pre className="title-display">{title}</pre>
      <StyledUnsavedIndicator>
        <UnsavedIndicator shown={!saved} />
      </StyledUnsavedIndicator>
    </Root>
  )
}

export default hot(module)(render)
