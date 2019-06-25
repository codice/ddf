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
import WorkspaceInteractions from '../../container/workspace-interactions'
const QueryAddView = require('../../../component/query-add/query-add.view.js')
import MarionetteRegionContainer from '../../container/marionette-region-container'
import SaveButton from '../save-button'
import WorkspaceTitle from '../workspace-title'
import Dropdown from '../dropdown'
import NavigationBehavior from '../navigation-behavior'

type Props = {
  currentWorkspace: Backbone.Model
  saved: boolean
  branding: string
  product: string
}

const StyledSaveButton = styled.div`
  display: block;
`

const StyledWorkspaceTitle = styled.div`
  display: block;
`

const StyledDropdown = styled(Dropdown)`
  height: 100%;
  line-height: inherit;
`

const Icon = styled.span`
  display: inline-block;
  text-align: right;
  width: ${props => props.theme.minimumButtonSize};
`

const Root = styled<{ saved: boolean }, 'div'>('div')`
  width: 100%;
  overflow: hidden;
  position: relative;
  height: 100%;
  white-space: nowrap;
  display: flex;
  justify-content: flex-start;

  > ${StyledWorkspaceTitle /* sc-selector*/}, > .content-adhoc,
  > .content-interactions,
  > ${StyledSaveButton /* sc-selector*/} {
    overflow: hidden;
    height: 100%;
  }

  > ${StyledWorkspaceTitle /* sc-selector*/} {
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
  const { currentWorkspace, saved, branding, product } = props
  return (
    <Root saved={saved}>
      <StyledWorkspaceTitle>
        <WorkspaceTitle
          title={currentWorkspace.get('title')}
          saved={saved}
          onChange={(title: string) => {
            currentWorkspace.set('title', title)
          }}
        />
      </StyledWorkspaceTitle>
      <StyledSaveButton>
        <SaveButton
          isSaved={saved}
          onClick={() => {
            currentWorkspace.save()
          }}
        />
      </StyledSaveButton>
      <StyledDropdown
        className="content-interactions"
        content={() => (
          <NavigationBehavior>
            <WorkspaceInteractions workspace={currentWorkspace} />
          </NavigationBehavior>
        )}
      >
        <span className="fa fa-ellipsis-v" />
      </StyledDropdown>
      <StyledDropdown
        className="content-adhoc"
        content={() => (
          <MarionetteRegionContainer
            view={QueryAddView}
            viewOptions={() => {
              return {
                model: currentWorkspace,
              }
            }}
          />
        )}
      >
        <Icon className="fa fa-search" /> Search {branding} {product}
      </StyledDropdown>
    </Root>
  )
}

export default hot(module)(render)
