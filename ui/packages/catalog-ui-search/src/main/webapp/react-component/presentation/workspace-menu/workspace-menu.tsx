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
import MarionetteRegionContainer from '../../container/marionette-region-container'
import SaveButton from '../save-button'

type Props = {
  currentWorkspace: Backbone.Model
  saved: boolean
}

const StyledSaveButton = styled.div`
  display: block;
`

const Root = styled<{ saved: boolean }, 'div'>('div')`
  width: 100%;
  overflow: hidden;
  position: relative;
  height: 100%;
  white-space: nowrap;
  display: flex;
  justify-content: flex-start;

  > .content-title,
  > .content-adhoc,
  > .content-interactions,
  > ${StyledSaveButton /* sc-selector*/} {
    overflow: hidden;
    height: 100%;
  }

  > .content-title {
    text-overflow: ellipsis;
  }

  > .content-interactions,
  > ${StyledSaveButton /* sc-selector */} {
    flex-shrink: 0;
  }

  > .content-interactions,
  > .content-adhoc {
    min-width: ${props => props.theme.minimumButtonSize};
    height: 100%;
    text-align: center;
  }

  > .content-adhoc {
    flex-shrink: 20;
  }
`

const render = (props: Props) => {
  const { currentWorkspace, saved } = props
  return (
    <Root saved={saved}>
      <MarionetteRegionContainer
        className="content-title"
        view={TitleView}
        gaseous={false}
      />
      <StyledSaveButton>
        <SaveButton
          isSaved={saved}
          onClick={() => {
            currentWorkspace.save()
          }}
        />
      </StyledSaveButton>
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
        gaseous={false}
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
        gaseous={false}
      />
    </Root>
  )
}

export default hot(module)(render)
