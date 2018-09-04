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
import { Button, buttonTypeEnum } from '../button'
import { hot } from 'react-hot-loader'

type SaveButtonProps = {
  isSaved: boolean
}

const Root = styled<SaveButtonProps, 'div'>('div')`
  display: inline-block;
  height: 100%;
  width: ${props => props.theme.minimumButtonSize};
  transition: ${props => {
    if (props.isSaved) {
      return `transform 0s linear ${props.theme.multiple(
        11,
        props.theme.coreTransitionTime,
        's'
      )}`
    } else {
      return 'none'
    }
  }};
  transform: scale(${props => (props.isSaved ? 0 : 1)});
`

const SaveIcon = styled<SaveButtonProps, 'span'>('span')`
  color: ${props => props.theme.positiveColor};
  opacity: ${props => (props.isSaved ? 0 : 1)};
  transform: scale(${props => (props.isSaved ? 2 : 1)});
  transition: opacity
      ${props => props.theme.multiple(2, props.theme.coreTransitionTime, 's')}
      ease-out 0s,
    transform
      ${props => props.theme.multiple(2, props.theme.coreTransitionTime, 's')}
      ease-out 0s;
`

const CheckIcon = styled<SaveButtonProps, 'span'>('span')`
  visibility: visible;
  transition: transform
      ${props => props.theme.multiple(2, props.theme.coreTransitionTime, 's')}
      ease-out ${props => props.theme.coreTransitionTime},
    opacity
      ${props => props.theme.multiple(2, props.theme.coreTransitionTime, 's')}
      ease-out ${props => props.theme.coreTransitionTime},
    color
      ${props => props.theme.multiple(2, props.theme.coreTransitionTime, 's')}
      ease-out
      ${props => props.theme.multiple(8, props.theme.coreTransitionTime, 's')};
  color: ${props =>
    props.isSaved ? 'transparent' : props.theme.positiveColor};
  transform: scale(${props => (props.isSaved ? 1 : 2)});
  opacity: ${props => (props.isSaved ? 1 : 0)};
  margin: auto;
  position: absolute;
  line-height: inherit;
  left: 0px;
  text-align: center;
  width: 100%;
`

export default hot(module)(function SaveButton({
  isSaved,
  onClick,
}: SaveButtonProps & React.HTMLProps<HTMLButtonElement>) {
  return (
    <Root isSaved={isSaved}>
      <Button
        buttonType={buttonTypeEnum.neutral}
        onClick={onClick}
        tabIndex={isSaved ? -1 : 0}
      >
        <SaveIcon className="fa fa-floppy-o" isSaved={isSaved} />
        <CheckIcon className="fa fa-check" isSaved={isSaved} />
      </Button>
    </Root>
  )
})
