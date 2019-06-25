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
import Card from '../card'
import SaveButton from '../save-button'
import { buttonTypeEnum, Button } from '../button'
import Dropdown from '../dropdown'
import NavigationBehavior from '../navigation-behavior'
import WorkspaceInteractions from '../../container/workspace-interactions'
import { hot } from 'react-hot-loader'

const StyledDropdown = styled(Dropdown)`
  height: 100%;
  line-height: inherit;
`

const Root = styled.div`
  .choice-title {
    > .title-text {
      white-space: pre;
      display: inline-block;
      max-width: calc(100% - ${props => props.theme.minimumFontSize});
      overflow: hidden;
      text-overflow: ellipsis;
    }
    > .title-indicator {
      display: inline-block;
      vertical-align: top;
    }
  }
`

interface openWorkspaceFunction {
  (): void
}

interface Props {
  title: string
  workspace: any
  date: string
  localStorage: boolean
  owner: string
  openWorkspace: openWorkspaceFunction
  unsaved: boolean
}

const Header = (props: Props) => {
  return (
    <div className="choice-title" data-help="The title of the workspace.">
      <span className="title-text"> {props.title} </span>
      <div className="title-indicator" />
    </div>
  )
}

const Icon = styled.span`
  margin-right: calc(${props => props.theme.minimumSpacing} / 2);
`

const Author = styled.div`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`

const Details = (props: Props) => {
  return (
    <React.Fragment>
      <div
        title={props.date}
        data-help="The date the workspace was last modified"
      >
        {props.date}
      </div>
      <Author title={props.owner} data-help="The owner of the workspace.">
        {props.localStorage ? (
          <Icon className="fa fa-home" />
        ) : (
          <Icon className="fa fa-cloud" />
        )}
        <span className="owner-id">{props.owner}</span>
      </Author>
    </React.Fragment>
  )
}

const Footer = (props: Props) => {
  return (
    <>
      <SaveButton
        isSaved={!props.unsaved}
        onClick={e => {
          e.stopPropagation()
          props.workspace.save()
        }}
      />
      <div
        onClick={e => {
          e.stopPropagation()
        }}
      >
        <StyledDropdown
          className="content-interactions"
          content={() => (
            <NavigationBehavior>
              <WorkspaceInteractions workspace={props.workspace} />
            </NavigationBehavior>
          )}
        >
          <Button
            buttonType={buttonTypeEnum.neutral}
            fadeUntilHover
            icon="fa fa-ellipsis-v"
          />
        </StyledDropdown>
      </div>
    </>
  )
}

const WorkspaceItem = (props: Props) => {
  return (
    <Root onClick={props.openWorkspace} tabIndex={0}>
      <Card
        header={Header(props)}
        details={Details(props)}
        footer={Footer(props)}
      />
    </Root>
  )
}

export default hot(module)(WorkspaceItem)
