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
import { Button, buttonTypeEnum } from '../button'
import { hot } from 'react-hot-loader'

type SaveButtonProps = {
  isSaved: boolean
}

const Root = styled<SaveButtonProps, 'div'>('div')`
  display: inline-block;
  vertical-align: top;
  overflow: hidden;
  height: 100%;
  width: ${props => (props.isSaved ? '0px' : props.theme.minimumButtonSize)};
  transition: ${props => {
    return `width ${
      props.theme.coreTransitionTime
    } linear ${props.theme.multiple(
      props.isSaved ? 11 : 0,
      props.theme.coreTransitionTime,
      's'
    )}`
  }};
`

const SaveIcon = styled<SaveButtonProps, 'span'>('span')`
  color: inherit;
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
  ${props => (!props.isSaved ? 'transition: none;' : '')}
  color: ${props => (props.isSaved ? 'transparent' : 'inherit')};
  transform: scale(${props => (props.isSaved ? 1 : 2)});
  opacity: ${props => (props.isSaved ? 1 : 0)};
  margin: auto;
  position: absolute;
  line-height: inherit;
  left: 0px;
  text-align: center;
  width: 100%;
`

const ModifiedButton = styled(Button)`
  height: 100%;
`

export default hot(module)(function SaveButton({
  isSaved,
  onClick,
}: SaveButtonProps & React.HTMLProps<HTMLButtonElement>) {
  return (
    <Root isSaved={isSaved}>
      <ModifiedButton
        buttonType={buttonTypeEnum.neutral}
        onClick={
          isSaved
            ? (e: React.MouseEvent) => {
                e.stopPropagation()
              }
            : onClick
        }
        tabIndex={isSaved ? -1 : 0}
      >
        <SaveIcon className="fa fa-floppy-o" isSaved={isSaved} />
        <CheckIcon className="fa fa-check" isSaved={isSaved} />
      </ModifiedButton>
    </Root>
  )
})
