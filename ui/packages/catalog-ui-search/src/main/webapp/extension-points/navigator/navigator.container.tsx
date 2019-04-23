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
import { Props as PresentationProps } from './navigator.presentation'
import withListenTo, {
  WithBackboneProps,
} from '../../react-component/container/backbone-container'
const store = require('../../js/store')
const sources = require('../../component/singletons/sources-instance')
const properties = require('../../js/properties.js')
const metacard = require('../../component/metacard/metacard')
const wreqr = require('../../js/wreqr.js')

import { hot } from 'react-hot-loader'

type Props = {
  closeSlideout?: () => void
  children: (props: PresentationProps) => React.ReactNode
} & WithBackboneProps

type State = {
  hasUnavailableSources: boolean
  isSaved: boolean
  branding: string
  product: string
  recentWorkspace?: any
  recentMetacard?: any
  uploadEnabled: boolean
  isDevelopment: boolean
}

const visitFragment = (fragment: string) =>
  wreqr.vent.trigger('router:navigate', {
    fragment: fragment,
    options: {
      trigger: true,
    },
  })

const getState = () => {
  const hasUnsaved = store.get('workspaces').find(function(workspace: any) {
    return !workspace.isSaved()
  })

  const hasDown = sources.some(function(source: any) {
    return !source.get('available')
  })

  const currentWorkspace = store.getCurrentWorkspace()
  let workspaceJSON
  if (currentWorkspace) {
    workspaceJSON = currentWorkspace.toJSON()
  }

  const currentMetacard = metacard.get('currentMetacard')
  var metacardJSON
  if (currentMetacard) {
    metacardJSON = currentMetacard.toJSON()
  }

  return {
    isSaved: !hasUnsaved,
    hasUnavailableSources: hasDown,
    branding: properties.branding,
    product: properties.product,
    recentWorkspace: workspaceJSON,
    recentMetacard: metacardJSON,
    uploadEnabled: properties.isUploadEnabled(),
    isDevelopment: properties.isDevelopment(),
  }
}

class NavigationContainer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = getState()
  }
  componentDidMount() {
    this.props.listenTo(
      store.get('workspaces'),
      'change:saved update add remove',
      this.updateState
    )
    this.props.listenTo(sources, 'all', this.updateState)
  }
  updateState = () => {
    this.setState(getState())
  }
  navigateToRoute = (route: string) => {
    if (route === '_home') {
      window.location.href = '/'
    } else {
      visitFragment(route || '')
      if (this.props.closeSlideout) {
        this.props.closeSlideout()
      }
    }
  }
  handleChoice = (e: any) => {
    const fragment = e.currentTarget.getAttribute('data-fragment')
    if (fragment === '_home') {
      window.location.href = '/'
    } else {
      visitFragment(fragment || '')
      if (this.props.closeSlideout) {
        this.props.closeSlideout()
      }
    }
  }
  render() {
    const { children } = this.props
    return children({
      ...this.state,
      navigateToRoute: this.navigateToRoute,
    })
  }
}

export default hot(module)(withListenTo(NavigationContainer))
