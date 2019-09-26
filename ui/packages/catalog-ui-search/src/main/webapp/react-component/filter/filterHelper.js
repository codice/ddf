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
import metacardDefinitions from '../../component/singletons/metacard-definitions.js'
import properties from '../../js/properties.js'

export const getFilteredAttributeList = includedAttributes => {
  return metacardDefinitions.sortedMetacardTypes
    .filter(({ id }) => !properties.isHidden(id))
    .filter(({ id }) => !metacardDefinitions.isHiddenType(id))
    .filter(
      ({ id }) =>
        includedAttributes === undefined
          ? true
          : includedAttributes.includes(id)
    )
    .map(({ alias, id }) => ({
      label: alias || id,
      value: id,
      description: (properties.attributeDescriptions || {})[id],
    }))
}

export const getAttributeType = attribute => {
  const type = metacardDefinitions.metacardTypes[attribute].type
  if (type === 'GEOMETRY') return 'LOCATION'
  if (isIntegerType(type)) return 'INTEGER'
  if (isFloatType(type)) return 'FLOAT'

  return type
}

const isIntegerType = type => {
  return type === 'INTEGER' || type === 'SHORT' || type === 'LONG'
}

const isFloatType = type => {
  return type === 'FLOAT' || type === 'DOUBLE'
}
