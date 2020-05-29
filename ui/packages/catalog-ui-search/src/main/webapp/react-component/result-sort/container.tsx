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
import ResultSortPresentation from './presentation'
import { useBackbone } from '../../component/selection-checkbox/useBackbone.hook'

const Backbone = require('backbone')
const user = require('../../component/singletons/user-instance.js')

type Props = {
  closeDropdown: any
}

const getResultSort = () => {
  return user
    .get('user')
    .get('preferences')
    .get('resultSort')
}

const ResultSortContainer = ({ closeDropdown }: Props) => {
  const [collection, setCollection] = React.useState(
    new Backbone.Collection(getResultSort())
  )
  const [hasSort, setHasSort] = React.useState(collection.length > 0)
  const { listenTo } = useBackbone()
  React.useEffect(() => {
    listenTo(user.get('user').get('preferences'), 'change:resultSort', () => {
      const resultSort = getResultSort()
      setHasSort(resultSort !== undefined && resultSort.length > 0)
      setCollection(new Backbone.Collection(resultSort))
    })
  }, [])
  const removeSort = () => {
    user
      .get('user')
      .get('preferences')
      .set('resultSort', undefined)
    user
      .get('user')
      .get('preferences')
      .savePreferences()
    closeDropdown()
  }
  const saveSort = () => {
    const sorting = collection.toJSON()
    user
      .get('user')
      .get('preferences')
      .set('resultSort', sorting.length === 0 ? undefined : sorting)
    user
      .get('user')
      .get('preferences')
      .savePreferences()
    closeDropdown()
  }
  return (
    <ResultSortPresentation
      key={Math.random()}
      saveSort={saveSort}
      removeSort={removeSort}
      collection={collection}
      hasSort={hasSort}
    />
  )
}

export default hot(module)(ResultSortContainer)
