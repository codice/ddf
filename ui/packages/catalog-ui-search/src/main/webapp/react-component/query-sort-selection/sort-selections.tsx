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
import { useState } from 'react'
import { hot } from 'react-hot-loader'
import styled from 'styled-components'
import { useBackbone } from '../../component/selection-checkbox/useBackbone.hook'
import SortItem from './sort-item'
import {
  getNextAttribute,
  getSortAttributeOptions,
  getSortDirectionOptions,
  getLabel,
} from './sort-selection-helpers'

const SortRoot = styled.div`
  display: block;
  width: 100%;
  overflow: hidden;
`

const SortItemContainer = styled.div<{ first: boolean; last: boolean }>`
  display: block;
  white-space: nowrap;
  overflow: hidden;
  position: relative;
  margin: ${props => {
    if (props.first && props.last) {
      return `0 0 ${props.theme.minimumSpacing}`
    } else if (props.last) {
      return `${props.theme.minimumSpacing} 0`
    } else if (props.first) {
      return
    } else {
      return `${props.theme.minimumSpacing} 0 0`
    }
  }};
`

const AddSortContainer = styled.div`
  padding: 0px 1.5rem;
`

const AddSortButton = (props: { onClick: () => void }) => (
  <button
    className="is-primary"
    onClick={props.onClick}
    style={{ width: '100%' }}
  >
    <span className="fa fa-plus" />
  </button>
)

type Props = {
  collection: any
  showBestTextOption: boolean
}

export type Option = {
  label: string
  value: string
}

export type SortItemType = {
  attribute: Option
  direction: string
}

const getCollectionAsJson = (collection: any) => {
  const items: SortItemType[] = collection.models.map((model: any) => {
    return {
      attribute: {
        label: getLabel(model.get('attribute')),
        value: model.get('attribute'),
      },
      direction: model.get('direction'),
    }
  })
  return items
}

const SortSelections = ({ collection, showBestTextOption }: Props) => {
  if (!collection.length) {
    collection.add({
      attribute: 'title',
      direction: 'ascending',
    })
  }

  const { listenTo } = useBackbone()
  const [collectionJson, setCollectionJson] = useState<SortItemType[]>(
    getCollectionAsJson(collection)
  )

  const sortAttributeOptions = getSortAttributeOptions(
    showBestTextOption,
    collectionJson.map(item => item.attribute.value)
  )

  React.useEffect(() => {
    listenTo(collection, 'add remove change', () => {
      setCollectionJson(getCollectionAsJson(collection))
    })
  }, [])

  const updateAttribute = (index: number) => (attribute: string) => {
    collection.models[index].set('attribute', attribute)
  }

  const updateDirection = (index: number) => (direction: string) => {
    collection.models[index].set('direction', direction)
  }

  const removeItem = (index: number) => () => {
    collection.models[index].destroy()
  }

  const addSort = () => {
    collection.add({
      attribute: getNextAttribute(collectionJson, sortAttributeOptions),
      direction: 'descending',
    })
  }

  return (
    <SortRoot>
      {collectionJson.map((sortItem, index) => {
        return (
          <SortItemContainer
            key={sortItem.attribute.value}
            first={index === 0}
            last={index === collectionJson.length - 1}
          >
            <SortItem
              sortItem={sortItem}
              attributeOptions={sortAttributeOptions}
              directionOptions={getSortDirectionOptions(
                sortItem.attribute.value
              )}
              updateAttribute={updateAttribute(index)}
              updateDirection={updateDirection(index)}
              onRemove={removeItem(index)}
              showRemove={index !== 0}
            />
          </SortItemContainer>
        )
      })}
      <AddSortContainer>
        <AddSortButton onClick={addSort} />
      </AddSortContainer>
    </SortRoot>
  )
}

export default hot(module)(SortSelections)
