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

import { hot } from 'react-hot-loader'
import * as React from 'react'
import styled from '../../styles/styled-components'
import { Button, buttonTypeEnum } from '../button'
import LoadingCompanion from '../../container/loading-companion'

type Props = {
  clickWorkspace: (event: any) => void
  revertToSelectedVersion: () => void
  history: any
  selectedVersion: any
  loading: boolean
}

// prettier-ignore
const Root = styled.div`
  overflow: auto;
  height: 100%;

  .metacardHistory-row {
    transition: padding ${props => props.theme.transitionTime} linear;
  }

  .metacardHistory-header {
    height: 50px;
  }
  
  .metacardHistory-body {
    display: inline-block;
    max-height: ~'calc(100% - ${props => props.theme.minimumButtonSize}*2 - 20px - ${props => props.theme.minimumSpacing})';
    overflow: auto;
    overflow-x: hidden;
    width: 100%;
  }

  .metacardHistory-cell {
    float: left;
    padding: 10px;
    text-align: center;
  }

  .metacardHistory-version {
    width: 20%;
  }

  .metacardHistory-date {
    width: 50%;
  }

  .metacardHistory-user {
    width: 30%;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  button {
    margin-top: 10px;
    width: 100%;
    text-align: center;
    height: ${props => props.theme.minimumButtonSize};
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .is-small-screen,
  .is-mobile-screen {
    .metacardHistory-body {
      max-height: none;
      overflow: auto;
    }

    .metacardHistory-cell {
      display: block;
      width: 100%;
    }
  }
`

const render = (props: Props) => {
  const {
    clickWorkspace,
    revertToSelectedVersion,
    history,
    selectedVersion,
    loading,
  } = props
  return (
    <LoadingCompanion loading={loading}>
      <Root>
        <div className="metacardHistory-header clearfix">
          <div className="metacardHistory-row clearfix">
            <div className="metacardHistory-cell metacardHistory-version">
              Version
            </div>
            <div className="metacardHistory-cell metacardHistory-date">
              Date
            </div>
            <div className="metacardHistory-cell metacardHistory-user">
              Modified by
            </div>
          </div>
        </div>
        <div
          className="metacardHistory-body is-list has-list-highlighting is-clickable"
          data-help="This is the history of changes to
this item.  If you have the right permissions, you can click one of the items in the list
and then click 'Revert to Selected Version' to restore the item to that specific state.  No history
will be lost in the process.  Instead a new version will be created that is equal to the state you
have chosen."
        >
          {history.map((historyItem: any) => {
            return (
              <div
                className={`metacardHistory-row clearfix ${selectedVersion ===
                  historyItem.id && 'is-selected'}`}
                data-id={historyItem.id}
                key={historyItem.id}
                onClick={clickWorkspace}
              >
                <div className="metacardHistory-cell metacardHistory-version">
                  {historyItem.versionNumber}
                </div>
                <div className="metacardHistory-cell metacardHistory-date">
                  {historyItem.niceDate}
                </div>
                <div className="metacardHistory-cell metacardHistory-user">
                  {historyItem.editedBy}
                </div>
              </div>
            )
          })}
        </div>
        {selectedVersion && (
          <Button
            buttonType={buttonTypeEnum.primary}
            onClick={revertToSelectedVersion}
            icon="fa fa-undo"
            text="Revert to Selected Version"
          />
        )}
      </Root>
    </LoadingCompanion>
  )
}

export default hot(module)(render)
