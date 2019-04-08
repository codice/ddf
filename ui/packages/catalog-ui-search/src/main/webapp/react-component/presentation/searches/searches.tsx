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
import Search from '../search'
import Text from '../../container/input-wrappers/text'
import Dropdown from '../../presentation/dropdown'
import NavigationBehavior from '../../presentation/navigation-behavior'
import MenuSelection from '../../presentation/menu-selection'

const SearchesPage = styled.div`
  height: 100%;
  overflow-y: auto;

  background-color: ${props => props.theme.backgroundContent};
`

const ContextBar = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;

  background-color: ${props => props.theme.backgroundAccentContent};

  .filter-searches {
    width: calc(300px + (2 * ${props => props.theme.largeSpacing}));
    padding-left: calc(
      (5 * ${props => props.theme.minimumSpacing}) -
        ${props => props.theme.minimumSpacing}
    );
  }
`

const FilterOptions = styled.div`
  padding-right: calc(
    (5 * ${props => props.theme.minimumSpacing}) -
      ${props => props.theme.minimumSpacing}
  );

  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding-top: 15px;
`

const SearchContainer = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;

  padding: calc(5 * ${props => props.theme.minimumSpacing});
`

const SectionHeader = styled.h1`
  margin-top: calc(5 * ${props => props.theme.minimumSpacing});
  padding-left: calc(5 * ${props => props.theme.minimumSpacing});
`

const CreateSearchCard = styled.div`
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

const CreateSearchContent = styled.div`
  width: 100%;

  display: inline-block;
  text-align: center;

  padding: 25px;

  h1 {
    padding-top: 15px;
  }
`

type Search = {
  id: string
  title: string
  owner: string
  created: string
  modified: string
}

type Props = {
  searches: Search[]
}

const Searches = (props: Props) => {
  return (
    <SearchesPage>
      <ContextBar>
        <Text
          className="filter-searches"
          value=""
          showLabel={false}
          placeholder="Filter searches"
          onChange={() => {}}
        />
        <FilterOptions>
          <Dropdown
            content={context => (
              <NavigationBehavior>
                <MenuSelection
                  onClick={() => {
                    context.closeAndRefocus()
                  }}
                  isSelected={true}
                >
                  Alphabetical
                </MenuSelection>
                <MenuSelection
                  onClick={() => {
                    context.closeAndRefocus()
                  }}
                  isSelected={true}
                >
                  Date Created
                </MenuSelection>
                <MenuSelection
                  onClick={() => {
                    context.closeAndRefocus()
                  }}
                  isSelected={true}
                >
                  Date Modified
                </MenuSelection>
              </NavigationBehavior>
            )}
          >
            Sort by: Alphabetical
            <span className="fa-filter fa" />
          </Dropdown>
        </FilterOptions>
      </ContextBar>
      <SectionHeader>My Searches</SectionHeader>
      <SearchContainer>
        <CreateSearchCard onClick={() => alert('hello world')}>
          <CreateSearchContent>
            <span className="fa fa-plus-circle fa-5x" />
            <h1>Create Search</h1>
          </CreateSearchContent>
        </CreateSearchCard>
        {props.searches.map(search => (
          <Search
            key={search.id}
            id={search.id}
            title={search.title}
            owner={search.owner}
            created={search.created}
            modified={search.modified}
          />
        ))}
      </SearchContainer>
      <span>Show more</span>
    </SearchesPage>
  )
}

export default hot(module)(Searches)
