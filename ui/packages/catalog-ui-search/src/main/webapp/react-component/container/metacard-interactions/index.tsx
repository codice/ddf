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

import {
  render as View,
  Model as ViewModel,
} from '../../presentation/metacard-interactions'

import withListenTo, { WithBackboneProps } from '../backbone-container'

import * as CqlUtils from '../../../js/CQLUtils'

import * as CustomElements from '../../../js/CustomElements'

import * as QueryConfirmationView from '../../../component/confirmation/query/confirmation.query.view'
import * as LoadingView from '../../../component/loading/loading.view'
import * as PopoutView from '../../../component/dropdown/popout/dropdown.popout.view'
import * as ResultAddView from '../../../component/result-add/result-add.view'

import * as router from '../../../component/router/router'
import * as sources from '../../../component/singletons/sources-instance'

import * as store from '../../../js/store'
import * as user from '../../../component/singletons/user-instance'

import MarionetteRegionContainer from '../marionette-region-container'

import ExportActions from '../../../react-component/presentation/export-actions'

const Query = require('../../../js/model/Query')
const wreqr = require('wreqr')
const Marionette = require('marionette')

type Props = {
  model: {} | any
  el: {} | any
  extensions: {} | any
  handleShare?: () => void
  categories: { [key: string]: Array<{}> }
} & WithBackboneProps

type El = {
  find: (matcher: string) => any
  toggleClass: (key: string, flag: boolean) => void
}

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

const withCloseDropdown = (
  context: Props,
  action: (context: Props) => void
) => {
  context.el.trigger(`closeDropdown.${CustomElements.getNamespace()}`)
  action(context)
}

const handleDownload = (context: Props) => {
  const openValidUrl = (result: Result) => {
    const downloadUrl = result
      .get('metacard')
      .get('properties')
      .get('resource-download-url')
    downloadUrl && window.open(downloadUrl)
  }

  context.model.forEach(openValidUrl)
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

const handleShare = (context: Props) =>
  context.handleShare && context.handleShare()

const appendCssIfNeeded = (model: Model, el: El) => {
  const isMixed = model.reduce((accum: number, item: Result) => {
    if (item.isRemote()) {
      el.toggleClass('is-remote', true)
      return ++accum
    }
    if (item.isWorkspace()) {
      el.toggleClass('is-workspace', true)
      return ++accum
    }
    if (item.isResource()) {
      el.toggleClass('is-resource', true)
      return ++accum
    }
    if (item.isRevision()) {
      el.toggleClass('is-revision', true)
      return ++accum
    }
    if (item.isDeleted()) {
      el.toggleClass('is-deleted', true)
      return ++accum
    }
  }, 0)

  el.toggleClass('is-mixed', isMixed > 1)

  const currentWorkspace = store.getCurrentWorkspace()
  el.toggleClass('in-workspace', Boolean(currentWorkspace))
  el.toggleClass('is-downloadable', isDownloadable(model))
  el.toggleClass('is-multiple', model.length > 1)
  el.toggleClass('is-routed', isRouted())
  el.toggleClass('is-blacklisted', isBlacklisted(model))
  el.toggleClass('has-location', hasLocation(model))
}

const isDownloadable = (model: Model): boolean =>
  model.some((result: Result) =>
    result
      .get('metacard')
      .get('properties')
      .get('resource-download-url')
  )
const isRouted = (): boolean =>
  router && router.toJSON().name === 'openMetacard'
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

const hasLocation = (model: Model): boolean => getGeoLocations(model).length > 0

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
    componentToShow: Marionette.LayoutView.extend({
      template() {
        return (
          <ExportActions
            actions={this.model.getExportActions().map((action: any) => {
              return { url: action.get('url'), title: action.getExportType() }
            })}
          />
        )
      },
    }),
    dropdownCompanionBehaviors: {
      navigation: {},
    },
    modelForComponent: model.first(),
    leftIcon: 'fa fa-external-link',
    rightIcon: 'fa fa-chevron-down',
    label: 'Export as',
  })

const defaultLinks = [
  {
    parent: `interaction-hide`,
    dataHelp: `Adds to a list
    of results that will be hidden from future searches.  To clear this list,
    click the Settings icon, select Hidden, then choose to unhide the record.`,
    icon: `fa fa-eye-slash`,
    linkText: `Hide from Future Searches`,
    actionHandler: handleHide,
  },
  {
    parent: `interaction-show`,
    dataHelp: `Removes from the
    list of results that are hidden from future searches.`,
    icon: `fa fa-eye`,
    linkText: `Unhide from Future Searches`,
    actionHandler: handleShow,
  },
  {
    parent: `interaction-expand`,
    dataHelp: `Takes you to a
    view that only focuses on this particular result.  Bookmarking it will allow
    you to come back to this result directly.`,
    icon: `fa fa-expand`,
    linkText: `Expand Metacard View`,
    actionHandler: handleExpand,
  },
  {
    parent: `interaction-share`,
    dataHelp: `Copies the
    URL that leads to this result directly to your clipboard.`,
    icon: `fa fa-share-alt`,
    linkText: `Share Metacard`,
    actionHandler: handleShare,
  },
]

const viewModelFromProps = (props: Props): ViewModel => {
  const defaultCategories = [{ name: 'default', items: defaultLinks }]

  if (!props.categories) return { categories: defaultCategories } as ViewModel

  const categories = Object.keys(props.categories).map(key => ({
    name: key,
    items: props.categories[key],
  }))
  return { categories: [...defaultCategories, ...categories] } as ViewModel
}

class MetacardInteractions extends React.Component<Props> {
  componentDidMount = () => {
    appendCssIfNeeded(this.props.model, this.props.el)

    const setState = (model: Model) => this.setState({ model: model })

    const toggleIsBlacklisted = () =>
      this.props.el.toggleClass(
        'is-blacklisted',
        isBlacklisted(this.props.model)
      )

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
      toggleIsBlacklisted
    )
  }

  render = () => (
    <>
      <MarionetteRegionContainer
        data-help="Add the result to a list."
        className="metacard-interaction interaction-add"
        view={createAddRemoveRegion(this.props.model)}
        viewOptions={{ model: this.props.model }}
      />
      <View
        handleCreateSearch={() =>
          withCloseDropdown(this.props, handleCreateSearch)
        }
        handleDownload={() => withCloseDropdown(this.props, handleDownload)}
        isRemoteResourceCached={isRemoteResourceCached(this.props.model)}
        withCloseDropdown={handler => withCloseDropdown(this.props, handler)}
        viewModel={viewModelFromProps(this.props)}
      />
      <MarionetteRegionContainer
        data-help="Opens the available actions for the item."
        className="metacard-interaction interaction-actions-export composed-menu"
        view={createResultActionsExportRegion(this.props.model)}
        viewOptions={{ model: this.props.model }}
      />
      {this.props.extensions && (
        <MarionetteRegionContainer
          className="composed-menu interaction-extensions"
          view={this.props.extensions}
          viewOptions={{ mode: this.props.model }}
        />
      )}
    </>
  )
}

export default withListenTo(MetacardInteractions)
