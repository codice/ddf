import { SortItemType } from './sort-selections'

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
const metacardDefinitions = require('../../component/singletons/metacard-definitions.js')
const properties = require('../../js/properties')
import { Option } from './sort-selections'

const blacklist = ['anyText', 'anyGeo']

type AttributeType = {
  alias: string
  hidden: boolean
  id: string
  isInjected: boolean
  multivalued: boolean
  readOnly: boolean
  type: string
}

export const getLabel = (value: string) => {
  let label = metacardDefinitions.getLabel(value)
  if (label === 'RELEVANCE') {
    return 'Best Text Match'
  }
  return label
}

export const getSortAttributeOptions = (
  showBestTextOption: boolean,
  currentSelections: string[]
) => {
  const currentAttributes =
    currentSelections && currentSelections.length ? currentSelections : []
  const attributes = metacardDefinitions.sortedMetacardTypes as AttributeType[]
  const options: Option[] = attributes
    .filter(type => !properties.isHidden(type.id))
    .filter(type => !metacardDefinitions.isHiddenTypeExceptThumbnail(type.id))
    .filter(type => !blacklist.includes(type.id))
    .filter(type => !currentAttributes.includes(type.id))
    .map(type => ({
      label: type.alias || type.id,
      value: type.id,
    }))

  const showBestTextValue = 'RELEVANCE'
  if (showBestTextOption && !currentAttributes.includes(showBestTextValue)) {
    options.unshift({
      label: 'Best Text Match',
      value: showBestTextValue,
    })
  }
  return options
}

export const getSortDirectionOptions = (attributeVal: string) => {
  let ascendingLabel, descendingLabel
  if (metacardDefinitions.metacardTypes[attributeVal] === undefined) {
    ascendingLabel = descendingLabel = ''
  } else {
    switch (metacardDefinitions.metacardTypes[attributeVal].type) {
      case 'DATE':
        ascendingLabel = 'Earliest'
        descendingLabel = 'Latest'
        break
      case 'BOOLEAN':
        ascendingLabel = 'True First' //Truthiest
        descendingLabel = 'False First' //Falsiest
        break
      case 'LONG':
      case 'DOUBLE':
      case 'FLOAT':
      case 'INTEGER':
      case 'SHORT':
        ascendingLabel = 'Smallest'
        descendingLabel = 'Largest'
        break
      case 'STRING':
        ascendingLabel = 'A to Z'
        descendingLabel = 'Z to A'
        break
      case 'GEOMETRY':
        ascendingLabel = 'Closest'
        descendingLabel = 'Furthest'
        break
      case 'XML':
      case 'BINARY':
      default:
        ascendingLabel = 'Ascending'
        descendingLabel = 'Descending'
        break
    }
  }

  const ascendingOption: Option = {
    label: ascendingLabel,
    value: 'ascending',
  }
  const descendingOption: Option = {
    label: descendingLabel,
    value: 'descending',
  }
  return [ascendingOption, descendingOption]
}

export const getNextAttribute = (
  collection: SortItemType[],
  options: Option[]
) => {
  const attributes = collection.map(type => type.attribute.value)
  for (let option of options) {
    if (!attributes.includes(option.value)) {
      return option.value
    }
  }
  return options[0].value
}

export const isDirectionalSort = (attribute: string) => {
  return metacardDefinitions.metacardTypes[attribute] !== undefined
}
