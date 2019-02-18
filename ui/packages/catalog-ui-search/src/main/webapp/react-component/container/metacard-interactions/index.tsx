/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import * as React from 'react'
import { Geometry } from 'wkx'

import withListenTo, { WithBackboneProps } from '../backbone-container'
import { hot } from 'react-hot-loader'

// import * as CqlUtils from '../../../js/CQLUtils'
const CqlUtils = require('../../../js/CQLUtils')

import * as QueryConfirmationView from '../../../component/confirmation/query/confirmation.query.view'
import * as LoadingView from '../../../component/loading/loading.view'
import * as PopoutView from '../../../component/dropdown/popout/dropdown.popout.view'
import * as ResultAddView from '../../../component/result-add/result-add.view'
import * as ExportActionsView from '../../../component/export-actions/export-actions.view'

import * as router from '../../../component/router/router'
import * as sources from '../../../component/singletons/sources-instance'

import * as store from '../../../js/store'
import * as user from '../../../component/singletons/user-instance'

import MarionetteRegionContainer from '../marionette-region-container'
import {
  Divider,
  MetacardInteraction,
} from '../../presentation/metacard-interactions/metacard-interactions'

const plugin = require('plugins/metacard-interactions')

const Query = require('../../../js/model/Query')
const wreqr = require('wreqr')

type Props = {
  model: {} | any
  onClose: () => void
  extensions: {} | any
  categories: { [key: string]: Array<{}> }
} & WithBackboneProps

type Result = {
  get: (key: any) => any
  isWorkspace: () => boolean
  isResource: () => boolean
  isRevision: () => boolean
  isDeleted: () => boolean
  isRemote: () => boolean
}

type Model = {
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

const handleCreateSearch = (context: Props) => {
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

const handleShow = (context: Props) => {
  context.onClose()
  const preferences = user.get('user').get('preferences')
  const getResult = (result: Result) =>
    result
      .get('metacard')
      .get('properties')
      .get('id')

  preferences.get('resultBlacklist').remove(context.model.map(getResult))
  preferences.savePreferences()
}

const handleHide = (context: Props) => {
  context.onClose()
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

const handleExpand = (context: Props) => {
  context.onClose()
  let id = context.model
    .first()
    .get('metacard')
    .get('properties')
    .get('id')

  id = encodeURIComponent(id)

  wreqr.vent.trigger('router:navigate', {
    fragment: 'metacards/' + id,
    options: {
      trigger: true,
    },
  })
}

const isBlacklisted = (model: Model): boolean => {
  const blacklist = user
    .get('user')
    .get('preferences')
    .get('resultBlacklist')
  return model.reduce((accum: boolean, result: Result) => {
    const id = result
      .get('metacard')
      .get('properties')
      .get('id')
    return blacklist.get(id) !== undefined || accum
  }, false)
}

const isRemoteResourceCached = (model: Model): boolean => {
  if (!model) return false

  const modelJson = model.toJSON()

  if (!modelJson || modelJson.length <= 0) return false

  return (
    modelJson[0].isResourceLocal &&
    modelJson[0].metacard.properties['source-id'] !== sources.localCatalog
  )
}

const createAddRemoveRegion = (model: Model) =>
  PopoutView.createSimpleDropdown({
    componentToShow: ResultAddView,
    modelForComponent: model,
    leftIcon: 'fa fa-plus',
    rightIcon: 'fa fa-chevron-down',
    label: 'Add / Remove from List',
  })

const createResultActionsExportRegion = (model: Model) =>
  PopoutView.createSimpleDropdown({
    componentToShow: ExportActionsView,
    dropdownCompanionBehaviors: {
      navigation: {},
    },
    modelForComponent: model.first(),
    leftIcon: 'fa fa-external-link',
    rightIcon: 'fa fa-chevron-down',
    label: 'Export as',
  })

const AddToList = (props: any) => {
  const currentWorkspace = store.getCurrentWorkspace()
  if (!currentWorkspace) {
    return null
  }

  return (
    <MarionetteRegionContainer
      data-help="Add the result to a list."
      className="metacard-interaction interaction-add"
      view={createAddRemoveRegion(props.model)}
      viewOptions={{ model: props.model }}
    />
  )
}

const isDownloadable = (model: Model): boolean =>
  model.some((result: Result) =>
    result
      .get('metacard')
      .get('properties')
      .get('resource-download-url')
  )

const handleDownload = (model: Model) => {
  const openValidUrl = (result: Result) => {
    const downloadUrl = result
      .get('metacard')
      .get('properties')
      .get('resource-download-url')
    downloadUrl && window.open(downloadUrl)
  }

  model.forEach(openValidUrl)
}

const DownloadProduct = (props: any) => {
  if (!isDownloadable(props.model)) {
    return null
  }
  return (
    <MetacardInteraction
      text="Download"
      help="Downloads the result's associated product directly to your machine."
      icon="fa fa-download"
      onClick={() => handleDownload(props.model)}
    >
      {isRemoteResourceCached(props.model) && (
        <span
          data-help="Displayed if the remote resource has been cached locally."
          className="download-cached"
        >
          Local
        </span>
      )}
    </MetacardInteraction>
  )
}

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

const ExportActions = (props: any) => {
  return (
    <MarionetteRegionContainer
      data-help="Opens the available actions for the item."
      className="metacard-interaction interaction-actions-export composed-menu"
      view={createResultActionsExportRegion(props.model)}
      viewOptions={{ model: props.model }}
    />
  )
}

const BlacklistToggle = (props: any) => {
  if (props.blacklisted) {
    return (
      <MetacardInteraction
        text="Unhide from Future Searches"
        help="Removes from the
              list of results that are hidden from future searches."
        icon="fa fa-eye"
        onClick={() => handleShow(props)}
      />
    )
  } else {
    return (
      <MetacardInteraction
        text="Hide from Future Searches"
        help="Adds to a list
              of results that will be hidden from future searches.  To clear this list,
              click the Settings icon, select Hidden, then choose to unhide the record."
        icon="fa fa-eye-slash"
        onClick={() => handleHide(props)}
      />
    )
  }
}

const ExpandMetacard = (props: any) => {
  const isRouted = router && router.toJSON().name === 'openMetacard'

  if (isRouted || props.model.length > 1) {
    return null
  }

  return (
    <MetacardInteraction
      text="Expand Metacard View"
      help="Takes you to a
            view that only focuses on this particular result.  Bookmarking it will allow
            you to come back to this result directly."
      icon="fa fa-expand"
      onClick={() => handleExpand(props)}
    />
  )
}

type State = {
  blacklisted: Boolean
  model: any
}

const interactions = plugin([
  AddToList,
  BlacklistToggle,
  ExpandMetacard,
  Divider,
  DownloadProduct,
  CreateLocationSearch,
  ExportActions,
])

const mapPropsToState = (props: Props) => {
  return {
    model: props.model,
    blacklisted: isBlacklisted(props.model),
  }
}

class MetacardInteractions extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = mapPropsToState(props)
  }
  componentDidMount = () => {
    const setState = (model: Model) => this.setState({ model: model })

    this.props.listenTo(
      this.props.model,
      'change:metacard>properties',
      setState
    )

    this.props.listenTo(
      user
        .get('user')
        .get('preferences')
        .get('resultBlacklist'),
      'add remove update reset',
      () => this.setState({ blacklisted: isBlacklisted(this.props.model) })
    )
  }

  render = () => {
    return (
      <>
        {interactions.map((Component: any, i: number) => {
          return (
            <Component
              key={i}
              {...this.props}
              blacklisted={this.state.blacklisted}
            />
          )
        })}
      </>
    )
  }
}

const Component = withListenTo(MetacardInteractions)

export default hot(module)(Component)
