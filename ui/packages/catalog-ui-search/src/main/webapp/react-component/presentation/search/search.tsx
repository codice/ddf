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
import { hot } from 'react-hot-loader'
import styled from '../../styles/styled-components'
import { buttonTypeEnum, Button } from '../button'
import Dropdown from '../../presentation/dropdown'
import NavigationBehavior from '../../presentation/navigation-behavior'
import SearchInteractions from '../../container/search-interactions'

const Card = styled.div`
  width: 300px;
  margin-right: 25px;
  margin-bottom: 25px;

  border: 1px solid ${props => props.theme.backgroundAccentContent};

  color: white;
  font-size: ${props => props.theme.minimumFontSize};

  &:hover {
    cursor: pointer;
    border-color: rgba(255, 255, 255, 0.2);
  }
`

const Header = styled.div`
  font-weight: bolder;
  height: ${props => props.theme.minimumButtonSize};
  border-bottom: 1px solid ${props => props.theme.backgroundAccentContent};

  display: flex;
  flex-direction: row;
  justify-content: space-between;
`

const Title = styled.div`
  padding: 10px;
`

const Body = styled.div`
  padding: 10px;
  opacity: 0.6;
`

const Item = styled.p`
  max-width: 300px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
`

type Props = {
  id: string
  title: string
  owner: string
  created: string
  modified: string
}

const Search = (props: Props) => {
  return (
    <Card
      onClick={() => {
        alert(props.id)
      }}
    >
      <Header
        onClick={e => {
          e.stopPropagation()
        }}
      >
        <Title>{props.title}</Title>
        <Dropdown
          content={() => (
            <NavigationBehavior>
              <SearchInteractions id={props.id} />
            </NavigationBehavior>
          )}
        >
          <Button
            buttonType={buttonTypeEnum.neutral}
            fadeUntilHover
            icon="fa fa-ellipsis-v"
          />
        </Dropdown>
      </Header>
      <Body>
        <Item>Owner: {props.owner}</Item>
        <Item>Created: {props.created}</Item>
        <Item>Modified: {props.modified}</Item>
      </Body>
    </Card>
  )
}

export default hot(module)(Search)
