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
import MenuAction from '../menu-action'
import styled from '../../styles/styled-components'
import { readableColor } from 'polished'
import { hot } from 'react-hot-loader'

type Props = {
  saveWorkspace: () => void
  runAllSearches: () => void
  cancelAllSearches: () => void
  openWorkspaceInNewTab: () => void
  viewSharing: () => void
  viewDetails: () => void
  duplicateWorkspace: () => void
  subscribeToWorkspace: () => void
  unsubscribeFromWorkspace: () => void
  deletionPrompt: () => void
  isSubscribed: boolean
  isLocal: boolean
  isDeletable: boolean
  isShareable: boolean
}

const Root = styled<{}, 'div'>('div')`
  width: 100%;
  color: ${props => readableColor(props.theme.background)};
`

const render = (props: Props) => {
  const {
    saveWorkspace,
    runAllSearches,
    cancelAllSearches,
    openWorkspaceInNewTab,
    viewSharing,
    viewDetails,
    duplicateWorkspace,
    subscribeToWorkspace,
    unsubscribeFromWorkspace,
    deletionPrompt,
    isSubscribed,
    isLocal,
    isDeletable,
    isShareable,
  } = props
  return (
    <Root className="composed-menu">
      <MenuAction
        help="Save your changes to the workspace"
        icon="fa fa-floppy-o"
        onClick={(_e, context) => {
          saveWorkspace()
          context.closeAndRefocus()
        }}
      >
        Save Workspace
      </MenuAction>
      <MenuAction
        help="Runs the workspace's searches."
        onClick={(_e, context) => {
          runAllSearches()
          context.closeAndRefocus()
        }}
        icon="fa fa-play"
      >
        Run All Searches
      </MenuAction>
      <MenuAction
        help="Cancels the workspace's searches."
        onClick={(_e, context) => {
          cancelAllSearches()
          context.closeAndRefocus()
        }}
        icon="fa fa-stop"
      >
        Cancel All Searches
      </MenuAction>
      <MenuAction
        help="Opens the workspace view in a new browser tab."
        onClick={(_e, context) => {
          openWorkspaceInNewTab()
          context.closeAndRefocus()
        }}
        icon="fa fa-external-link"
      >
        Open Workspace in New Tab
      </MenuAction>
      {!isShareable || isLocal ? null : (
        <MenuAction
          help="Brings up a view of the current
        sharing settings for this workspace.  From there, you can change what permissions each role has on the workspace,
         as well as give permissions to individuals by specifying their email."
          onClick={(_e, context) => {
            viewSharing()
            context.closeAndRefocus()
          }}
          icon="fa fa-users"
        >
          View Sharing
        </MenuAction>
      )}
      {isLocal ? null : (
        <MenuAction
          help="Brings up a view of the current
        details for this workspace."
          onClick={(_e, context) => {
            viewDetails()
            context.closeAndRefocus()
          }}
          icon="fa fa-expand"
        >
          View Details
        </MenuAction>
      )}
      <MenuAction
        help="Creates a new workspace
        based off this workspace and redirects you there."
        onClick={(_e, context) => {
          duplicateWorkspace()
          context.closeAndRefocus()
        }}
        icon="fa fa-copy"
      >
        Duplicate Workspace
      </MenuAction>
      {isLocal ? null : isSubscribed ? (
        <MenuAction
          help="Clicking this will cause
        you to no longer recieve email alerts about new items relevant to this workspace's searches."
          onClick={(_e, context) => {
            unsubscribeFromWorkspace()
            context.closeAndRefocus()
          }}
          icon="fa fa-envelope-o"
        >
          Unsubscribe
        </MenuAction>
      ) : (
        <MenuAction
          help="Clicking this will cause
      email alerts to be sent to you regarding new items relevant to this workspace's searches."
          onClick={(_e, context) => {
            subscribeToWorkspace()
            context.closeAndRefocus()
          }}
          icon="fa fa-envelope"
        >
          Subscribe
        </MenuAction>
      )}
      {isDeletable && (
        <MenuAction
          help="Deletes the workspace.
        Anyone who has access to this workspace will subsequently lose access."
          onClick={(_e, context) => {
            deletionPrompt()
            context.closeAndRefocus()
          }}
          icon="fa fa-trash-o"
        >
          Delete Workspace
        </MenuAction>
      )}
    </Root>
  )
}

export default hot(module)(render)
