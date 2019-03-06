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
import { hot } from 'react-hot-loader'
import View from '../../presentation/result-filter'

const cql = require('../../../js/cql')

const getDeserializableFilter = (resultFilter: any) => {
  if (!!resultFilter) return cql.simplify(cql.read(resultFilter))

  return {
    property: 'anyText',
    value: '',
    type: 'ILIKE',
  }
}

export default hot(module)((props: any) => {
  const {
    saveFilter,
    removeFilter,
    hasFilter = false,
    resultFilter,
    isList = false,
    ...rest
  } = props
  return (
    <View
      hasFilter={hasFilter}
      resultFilter={getDeserializableFilter(resultFilter)}
      saveFilter={saveFilter}
      removeFilter={removeFilter}
      isList={isList}
      {...rest}
    />
  )
})
