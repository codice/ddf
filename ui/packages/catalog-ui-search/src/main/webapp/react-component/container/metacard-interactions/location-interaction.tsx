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

const QueryConfirmationView = require('../../../component/confirmation/query/confirmation.query.view')
const LoadingView = require('../../../component/loading/loading.view')

import { Geometry } from 'wkx'
const store = require('../../../js/store')

import { MetacardInteraction } from '../../presentation/metacard-interactions/metacard-interactions'
import { Props, Model, Result } from './'
import { hot } from 'react-hot-loader'

const CqlUtils = require('../../../js/CQLUtils')
const wreqr = require('wreqr')
const Query = require('../../../js/model/Query')

const addFilter = (filterTree: any, filter: any) => {
  filter.value = filter.value.value
  filterTree.filters.push(filter)
}

const addMultipleFilters = (filterTree: any, filters: any) => {
  filters.forEach((filter: any) => {
    addFilter(filterTree, filter)
  })
}

const createFilterTree = (locations: string[]) => {
  const filterTree = {
    type: 'OR',
    filters: [] as any[],
  }

  locations.forEach((location: string) => {
    const filterObject = CqlUtils.transformCQLToFilter(location)
    if (filterObject.filters !== undefined) {
      addMultipleFilters(filterTree, filterObject.filters)
    } else {
      addFilter(filterTree, filterObject)
    }
  })

  return filterTree
}

const handleCreateSearch = (props: Props) => {
  const locations = getGeoLocations(props.model)

  if (locations.length === 0) return

  const locationString = `(${locations.join(` OR `)})`
  const filterTree = createFilterTree(locations)
  const newQuery = new Query.Model({
    type: filterTree.filters.length > 1 ? 'advanced' : 'basic',
  })
  newQuery.set('filterTree', filterTree)
  newQuery.set('cql', locationString)

  const existingQuery = store.getCurrentQueries()

  if (existingQuery.canAddQuery()) {
    existingQuery.add(newQuery)
    store.setCurrentQuery(newQuery)
    return
  }

  props.listenTo(
    QueryConfirmationView.generateConfirmation({}),
    'change:choice',
    (confirmation: any) => {
      const choice = confirmation.get('choice')
      if (choice === true) {
        const loadingView = new LoadingView()
        store.get('workspaces').once('sync', (workspace: any) => {
          loadingView.remove()
          wreqr.vent.trigger('router:navigate', {
            fragment: `workspaces/${workspace.id}`,
            options: {
              trigger: true,
            },
          })
        })
        store.get('workspaces').createWorkspaceWithQuery(newQuery)
      } else if (choice !== false) {
        store.getCurrentQueries().remove(choice)
        store.getCurrentQueries().add(newQuery)
        store.setCurrentQuery(newQuery)
      }
    }
  )
}

const getGeoLocations = (model: Model) =>
  model.reduce((locationArray: Array<string>, result: Result) => {
    const location = result
      .get('metacard')
      .get('properties')
      .get('location')

    if (location) {
      const geometry = Geometry.parse(location)
      locationArray.push(`(${CqlUtils.buildIntersectCQL(geometry)})`)
    }

    return locationArray
  }, [])

const hasLocation = (model: Model): boolean => getGeoLocations(model).length > 0

const CreateLocationSearch = (props: any) => {
  if (!hasLocation(props.model)) {
    return null
  }
  return (
    <MetacardInteraction
      {...props}
      icon="fa fa-globe"
      text="Create Search from Location"
      help="Uses the geometry of the metacard to populate a search"
      onClick={() => handleCreateSearch(props)}
    />
  )
}

export default hot(module)(CreateLocationSearch)
