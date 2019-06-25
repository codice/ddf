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
import { CustomElement } from '../../styles/mixins'
import WorkspaceItemContainer from '../../container/workspace-item-container'
import { Button, buttonTypeEnum } from '../button'
import { hot } from 'react-hot-loader'

type Props = {
  filterDropdown: React.ReactNode
  sortDropdown: React.ReactNode
  byDate: boolean
  workspaces: Backbone.Model[]
  createBlankWorkspace: () => void
}

const Root = styled.div`
  ${CustomElement} position: relative;

  .home-items-center {
    max-width: 1200px;
    margin: auto;
    padding: 0px 100px;
  }

  .home-items-header {
    font-size: ${props => props.theme.minimumFontSize};
    font-weight: bolder;
    line-height: ${props => props.theme.minimumButtonSize};
  }

  .header-menu {
    float: right;
    text-align: center;
  }

  .menu-button {
    display: inline-block;
    padding: 0px 10px;
  }

  ${props => {
    if (props.theme.screenBelow(props.theme.smallScreenSize)) {
      return `
                .home-items-center {
                    max-width: 100%;
                    padding: 0px 20px;
                }
                .home-items-choices {
                    text-align: center;
                }
            `
    }
  }};
`

const WorkspaceItemRoot = styled.div`
  width: 18rem;
  height: calc(4.5 * ${props => props.theme.minimumLineSize});
  overflow: hidden;
  display: inline-block;
  margin-bottom: ${props => props.theme.minimumSpacing};
  margin-right: ${props => props.theme.largeSpacing};
`

const ModifiedButton = WorkspaceItemRoot.withComponent(Button)

const Icon = styled.div`
  margin-top: ${props => props.theme.mediumSpacing};
`

const WorkspacesItems = (props: Props) => {
  const { createBlankWorkspace } = props
  return (
    <Root>
      <div className="home-items-center">
        <div className="home-items-header clearfix">
          {props.byDate ? (
            <span className="header-hint by-date">Recent workspaces</span>
          ) : (
            <span className="header-hint by-title">Workspaces by title</span>
          )}
          <div className="header-menu">
            <div className="menu-button home-items-filter">
              {props.filterDropdown}
            </div>
            <div className="menu-button home-items-display">
              {props.sortDropdown}
            </div>
          </div>
        </div>
        <div className="home-items-choices is-list is-inline has-list-highlighting">
          <ModifiedButton
            buttonType={buttonTypeEnum.neutral}
            fadeUntilHover
            onClick={createBlankWorkspace}
          >
            <Icon>
              <span className="fa fa-plus-circle fa-3x" />
            </Icon>
            <div>New Workspace</div>
          </ModifiedButton>
          {props.workspaces.filter(workspace => workspace.id).map(workspace => {
            return (
              <WorkspaceItemRoot key={workspace.id}>
                <WorkspaceItemContainer workspace={workspace} />
              </WorkspaceItemRoot>
            )
          })}
        </div>
      </div>
    </Root>
  )
}

export default hot(module)(WorkspacesItems)
