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
const TitleView = require('component/content-title/content-title.view')
const DropdownModel = require('component/dropdown/dropdown')
const WorkspaceInteractionsView = require('component/dropdown/workspace-interactions/dropdown.workspace-interactions.view')
const DropdownQueryView = require('component/dropdown/query/dropdown.query.view')
const WorkspaceSaveView = require('component/save/workspace/workspace-save.view')
import MarionetteRegionContainer from '../../container/marionette-region-container'

type Props = {
  currentWorkspace: Backbone.Model
  saved: boolean
}

const Root = styled<{ saved: boolean }, 'div'>('div')`
  width: 100%;
  overflow: hidden;
  position: relative;
  height: calc(2 * ${props => props.theme.minimumLineSize});
  white-space: nowrap;

  > div {
    display: inline-block;
    vertical-align: top;
  }

  > .content-title {
    height: 100%;
    text-overflow: ellipsis;
    width: auto;
    transition: padding ${props => props.theme.coreTransitionTime} ease-in-out;
    padding-right: calc(
      ${props => (props.saved ? '2' : '3')} *
        ${props => props.theme.minimumButtonSize}
    );
    max-width: 100%;
    ${props =>
      props.saved
        ? `
      transition-delay: ${props.theme.multiple(
        10,
        props.theme.coreTransitionTime,
        's'
      )};
    `
        : ''};
  }

  > .content-interactions,
  > .content-save,
  > .content-adhoc {
    transform: translateX(calc(-3 * ${props => props.theme.minimumButtonSize}));
    transition: width
        ${props => props.theme.multiple(2, props.theme.coreTransitionTime, 's')}
        ease-out,
      margin
        ${props => props.theme.multiple(2, props.theme.coreTransitionTime, 's')}
        ease-out,
      transform
        ${props => props.theme.multiple(1, props.theme.coreTransitionTime, 's')}
        ease-out;
    width: ${props => props.theme.minimumButtonSize};
    height: 100%;
    text-align: center;
  }

  > .content-save {
    position: relative;
    overflow: hidden;
    width: ${props => props.theme.minimumButtonSize};
    ${props =>
      props.saved
        ? `
    opacity: 1;
    width: 0%;
    transition-delay: ${props.theme.multiple(
      10,
      props.theme.coreTransitionTime,
      's'
    )};
    `
        : ''};
  }

  > .content-adhoc {
    width: auto;
  }

  > .content-interactions,
  > .content-adhoc {
    ${props =>
      props.saved
        ? `
      transition-delay: ${props.theme.multiple(
        10,
        props.theme.coreTransitionTime,
        's'
      )};
      transform: translateX(calc(-2 * ${props.theme.minimumButtonSize}));
    `
        : ''};
  }
`

const render = (props: Props) => {
  const { currentWorkspace, saved } = props
  return (
    <Root saved={saved}>
      <MarionetteRegionContainer className="content-title" view={TitleView} />
      <MarionetteRegionContainer
        className="content-save"
        view={WorkspaceSaveView}
        viewOptions={() => {
          return {
            model: currentWorkspace,
          }
        }}
      />
      <MarionetteRegionContainer
        className="content-interactions is-button"
        view={WorkspaceInteractionsView}
        viewOptions={() => {
          return {
            model: new DropdownModel(),
            modelForComponent: currentWorkspace,
            dropdownCompanionBehaviors: {
              navigation: {},
            },
          }
        }}
      />
      <MarionetteRegionContainer
        className="content-adhoc is-button"
        view={DropdownQueryView}
        viewOptions={() => {
          return {
            model: new DropdownModel(),
            modelForComponent: currentWorkspace,
          }
        }}
      />
    </Root>
  )
}

export default hot(module)(render)
