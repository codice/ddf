import { Geometry } from 'wkx'

import { WithBackboneProps } from '../../react-component/container/backbone-container'

import * as CqlUtils from '../../js/CQLUtils'

import * as CustomElements from '../../js/CustomElements'

import * as Query from '../../js/model/Query'
import * as QueryConfirmationView from '../confirmation/query/confirmation.query.view'
import * as LoadingView from '../loading/loading.view'

import * as store from '../../js/store'
import * as user from '../../component/singletons/user-instance'

const wreqr = require('wreqr')

export type Props = {
  model: {} | any
  el: {} | any
  extensions: {} | any
  handleShare?: () => void
} & WithBackboneProps

export type Result = {
  get: (key: any) => any
  isWorkspace: () => boolean
  isResource: () => boolean
  isRevision: () => boolean
  isDeleted: () => boolean
  isRemote: () => boolean
}

export type Model = {
  map: (
    result: Result | any
  ) =>
    | {
        id?: any
        title?: any
      }
    | {}
  toJSON: () => any
  first: () => any
  forEach: (result: Result | any) => void
  find: (result: Result | any) => boolean
} & Array<any>

export const withCloseDropdown = (
  context: Props,
  action: (context: Props) => void
) => {
  context.el.trigger(`closeDropdown.${CustomElements.getNamespace()}`)
  action(context)
}

export const handleAdd = (context: Props) =>
  context.el
    .find('.interaction-add > *')
    .mousedown()
    .click()

export const handleDownload = (context: Props) => {
  const openValidUrl = (result: Result) => {
    const downloadUrl = result
      .get('metacard')
      .get('properties')
      .get('resource-download-url')
    downloadUrl && window.open(downloadUrl)
  }

  context.model.forEach(openValidUrl)
}

export const getGeoLocations = (model: Model) =>
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

export const handleCreateSearch = (context: Props) => {
  const locations = getGeoLocations(context.model)

  if (locations.length === 0) return

  const locationString = `(${locations.join(` OR `)})`
  const newQuery = new Query.Model({
    type: locations.length > 1 ? 'advanced' : 'basic',
  })
  newQuery.set('cql', locationString)

  const exsitingQuery = store.getCurrentQueries()

  if (exsitingQuery.canAddQuery()) {
    exsitingQuery.add(newQuery)
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

export const handleShow = (context: Props) => {
  const preferences = user.get('user').get('preferences')
  const getResult = (result: Result) =>
    result
      .get('metacard')
      .get('properties')
      .get('id')

  preferences.get('resultBlacklist').remove(context.model.map(getResult))
  preferences.savePreferences()
}

export const handleHide = (context: Props) => {
  const preferences = user.get('user').get('preferences')
  const getResult = (result: Result) => ({
    id: result
      .get('metacard')
      .get('properties')
      .get('id'),
    title: result
      .get('metacard')
      .get('properties')
      .get('title'),
  })

  preferences.get('resultBlacklist').add(context.model.map(getResult))
  preferences.savePreferences()
}

export const handleExpand = (context: Props) => {
  const id = context.model
    .first()
    .get('metacard')
    .get('properties')
    .get('id')

  wreqr.vent.trigger('router:navigate', {
    fragment: 'metacards/' + id,
    options: {
      trigger: true,
    },
  })
}

export const handleShare = (context: Props) => context.handleShare && context.handleShare()
