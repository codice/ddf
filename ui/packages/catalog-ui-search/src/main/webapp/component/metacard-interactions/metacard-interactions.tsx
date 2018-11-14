import * as React from 'react'

import withListenTo from '../../react-component/container/backbone-container'

import * as user from '../../component/singletons/user-instance'
import * as store from '../../js/store'
import * as router from '../router/router'

import * as sources from '../singletons/sources-instance'

import * as PopoutView from '../dropdown/popout/dropdown.popout.view'
import * as ResultAddView from '../result-add/result-add.view'
import * as ExportActionsView from '../export-actions/export-actions.view'

import {
  Props,
  Result,
  Model,
  withCloseDropdown,
  handleAdd,
  handleDownload,
  handleCreateSearch,
  handleExpand,
  handleHide,
  handleShow,
  handleShare,
  getGeoLocations,
} from './metacard-interactions.action.handlers'

const Marionette = require('marionette')

const interactionViewModel = [
  {
    parent: `interaction-add`,
    dataHelp: `Add the result to a list.`,
    icon: `fa fa-plus`,
    linkText: `Add / Remove from List`,
    actionHandler: handleAdd,
  },
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

type State = {
  model: Model
}

type El = {
  find: (matcher: string) => any
  toggleClass: (key: string, flag: boolean) => void
}

const isDownloadble = (model: Model) =>
  model.find((result: Result) =>
    result
      .get('metacard')
      .get('properties')
      .get('resource-download-url')
  )
const isRouted = () => router && router.toJSON().name === 'openMetacard'
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
    return blacklist.get(id) || accum
  }, false)
}

const isRemoteResourceCached = (model: Model) => {
  if (!model) return false

  const modelJson = model.toJSON()

  if (!modelJson || modelJson.length <= 0) return false

  return (
    modelJson[0].isResourceLocal &&
    modelJson[0].metacard.properties['source-id'] !== sources.localCatalog
  )
}

const hasLocation = (model: Model) => getGeoLocations(model).length > 0

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

const getRegionForSelector = (el: El, selector: any) =>
  new Marionette.Region({ el: el.find(selector) })

class InteractionsView extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)

    this.state = {
      model: props.model,
    }
  }

  componentDidMount = () => {
    this.appendCssIfNeeded(this.props.model, this.props.el)
    getRegionForSelector(this.props.el, '.interaction-add').show(
      createAddRemoveRegion(this.props.model)
    )
    getRegionForSelector(this.props.el, '.interaction-actions-export').show(
      createResultActionsExportRegion(this.props.model)
    )
    this.props.extensions &&
      getRegionForSelector(this.props.el, '.interaction-extensions').show(
        this.props.extensions
      )
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

  appendCssIfNeeded = (model: Model, el: El) => {
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
    el.toggleClass('is-downloadable', isDownloadble(model))
    el.toggleClass('is-multiple', model.length > 1)
    el.toggleClass('is-routed', isRouted())
    el.toggleClass('is-blacklisted', isBlacklisted(model))
    el.toggleClass('has-location', hasLocation(model))
  }

  render = () =>
    (this.state.model && (
      <>
        {interactionViewModel.map(model => (
          <div
            key={`key-${model.parent}-${model.icon}`}
            className={`metacard-interaction ${model.parent}`}
            data-help={model.dataHelp}
            onClick={() => withCloseDropdown(this.props, model.actionHandler)}
          >
            <div className={`interaction-icon ${model.icon}`} />
            <div className="interaction-text">{model.linkText}</div>
          </div>
        ))}
        <hr style={{ width: '90%' }} />
        <div
          className="metacard-interaction interaction-download"
          data-help="Downloads the results associated product directly to your machine."
          onClick={() => withCloseDropdown(this.props, handleDownload)}
        >
          <div className="interaction-icon fa fa-download" />
          <div className="interaction-text">Download</div>
          {isRemoteResourceCached(this.state.model) && (
            <span
              data-help="Displayed if the remote resource has been cached locally."
              className="download-cached"
            >
              Local
            </span>
          )}
        </div>
        <div
          className="metacard-interaction interaction-create-search"
          data-help="Uses the geometry of the metacard to populate a search."
          onClick={() => withCloseDropdown(this.props, handleCreateSearch)}
        >
          <div className="interaction-icon fa fa-globe" />
          <div className="interaction-text">Create Search from Location</div>
        </div>
        <div
          className="metacard-interaction interaction-actions-export composed-menu"
          data-help="Opens the available actions for the item."
        />

        <div className="composed-menu interaction-extensions" />
      </>
    )) ||
    null
}

export default withListenTo(InteractionsView)
