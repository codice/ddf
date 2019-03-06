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
import React from 'react'
import ResultFilterView from '../../react-component/container/result-filter'
import Marionette from 'marionette'
import user from '../singletons/user-instance'

const RESULT_FILTER_PROPERTY = 'resultFilter'

const getUserPreferences = () => user.get('user').get('preferences')

const getResultFilter = () => getUserPreferences().get(RESULT_FILTER_PROPERTY)

const onSaveFilter = transformedCql => {
  getUserPreferences().set(RESULT_FILTER_PROPERTY, transformedCql)
  getUserPreferences().savePreferences()
}

const onRemoveFilter = () => {
  getUserPreferences().set(RESULT_FILTER_PROPERTY, undefined)
  getUserPreferences().savePreferences()
}

module.exports = Marionette.LayoutView.extend({
  template() {
    return (
      <ResultFilterView
        hasFilter={!!getResultFilter()}
        resultFilter={getResultFilter()}
        saveFilter={onSaveFilter}
        removeFilter={onRemoveFilter}
      />
    )
  },
})
