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

const Root = styled.div`
  overflow: auto;
  height: 100%;

  .metacardHistory-cell {
    float: left;
    padding: 10px;
    text-align: center;
  }

  ${props => {
    if (props.theme.screenBelow(props.theme.smallScreenSize)) {
      return `
        .metacardHistory-body {
          max-height: none;
          overflow: auto;
        }
  
        .metacardHistory-cell {
          display: block;
          width: 100%;
        }
    `
    }
  }};
`

const Header = styled.div`
  height: 50px;
`

const Row = styled.div`
  transition: padding ${props => props.theme.transitionTime} linear;
`

// prettier-ignore
const Body = styled.div`
  max-height: calc(100% - ${props => props.theme.minimumButtonSize}*2 - 20px - ${props => props.theme.minimumSpacing});
  overflow: auto;
  overflow-x: hidden;
  width: 100%;
  cursor: pointer;
  display: table;
  content: " ";
  > *,
  > * > td {
    display: inline-block;
    width: 100%;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }
  > *:hover,
  > *:hover > td {
    border-top: 1px solid rgba(255, 255, 255, 0.2);
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  }
`

const Version = styled.div`
  width: 20%;
`

const Date = styled.div`
  width: 50%;
`

const Modified = styled.div`
  width: 30%;
  overflow: hidden;
  text-overflow: ellipsis;
`

const RevertButton = styled(Button)`
  margin-top: 10px;
  width: 100%;
  text-align: center;
  height: ${props => props.theme.minimumButtonSize};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`

const MetacardHistory = (props: Props) => {
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
        <Header>
          <Row>
            <Version className="metacardHistory-cell">Version</Version>
            <Date className="metacardHistory-cell">Date</Date>
            <Modified className="metacardHistory-cell">Modified by</Modified>
          </Row>
        </Header>
        <Body
          className="metacardHistory-body"
          data-help="This is the history of changes to
this item.  If you have the right permissions, you can click one of the items in the list
and then click 'Revert to Selected Version' to restore the item to that specific state.  No history
will be lost in the process.  Instead a new version will be created that is equal to the state you
have chosen."
        >
          {history.map((historyItem: any) => {
            return (
              <Row
                className={`${selectedVersion === historyItem.id &&
                  'is-selected'}`}
                data-id={historyItem.id}
                key={historyItem.id}
                onClick={clickWorkspace}
              >
                <Version className="metacardHistory-cell">
                  {historyItem.versionNumber}
                </Version>
                <Date className="metacardHistory-cell">
                  {historyItem.niceDate}
                </Date>
                <Modified className="metacardHistory-cell">
                  {historyItem.editedBy}
                </Modified>
              </Row>
            )
          })}
        </Body>
        {selectedVersion && (
          <RevertButton
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

export default hot(module)(MetacardHistory)
