import * as React from 'react'

import * as QueryConfirmationView from '../../../component/confirmation/query/confirmation.query.view'
import * as LoadingView from '../../../component/loading/loading.view'

import { Geometry } from 'wkx'
import * as store from '../../../js/store'

import { MetacardInteraction } from '../../presentation/metacard-interactions/metacard-interactions'
import { MetacardInteractionProps, Model, Result } from './'
import { hot } from 'react-hot-loader'

const CqlUtils = require('../../../js/CQLUtils')
const wreqr = require('wreqr')
const Query = require('../../../js/model/Query')

const handleCreateSearch = (context: MetacardInteractionProps) => {
  const locations = getGeoLocations(context.model)

  if (locations.length === 0) return

  const locationString = `(${locations.join(` OR `)})`
  const newQuery = new Query.Model({
    type: locations.length > 1 ? 'advanced' : 'basic',
  })
  newQuery.set('cql', locationString)

  const existingQuery = store.getCurrentQueries()

  if (existingQuery.canAddQuery()) {
    existingQuery.add(newQuery)
    store.setCurrentQuery(newQuery)
    return
  }

  context.listenTo(
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
