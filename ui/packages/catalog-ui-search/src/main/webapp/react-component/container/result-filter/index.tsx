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
import * as user from '../../../component/singletons/user-instance'
import * as cql from '../../../js/cql'

const RESULT_FITLER_PROPERTY = 'resultFilter'

type UserPreferences = {
  get: (key: string) => any
  set: (key: string, value: any) => void
  savePreferences: () => void
}

const getUserPreferences = (): UserPreferences =>
  user.get('user').get('preferences')

const getResultFilter = (): Boolean =>
  getUserPreferences().get(RESULT_FITLER_PROPERTY)

const getDeserializableFilter = (resultFilter: any) => {
  if (!!resultFilter) return cql.simplify(cql.read(resultFilter))

  return {
    property: 'anyText',
    value: '',
    type: 'ILIKE',
  }
}

const onSaveFilter = (transformedCql: {}) => {
  getUserPreferences().set(RESULT_FITLER_PROPERTY, transformedCql)
  getUserPreferences().savePreferences()
}

const onRemoveFilter = () => {
  getUserPreferences().set(RESULT_FITLER_PROPERTY, undefined)
  getUserPreferences().savePreferences()
}

export default hot(module)((props: any) => {
  const {
    saveFilter = onSaveFilter,
    removeFilter = onRemoveFilter,
    hasFilter = !!getResultFilter(),
    resultFilter,
    isList = false,
    ...rest
  } = props
  const formattedResultFilter = !!resultFilter
    ? getDeserializableFilter(resultFilter)
    : getDeserializableFilter(getResultFilter())
  return (
    <View
      hasFilter={hasFilter}
      resultFilter={formattedResultFilter}
      saveFilter={saveFilter}
      removeFilter={removeFilter}
      isList={isList}
      {...rest}
    />
  )
})
