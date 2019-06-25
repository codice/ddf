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
const wreqr = require('../../../js/wreqr.js')
import MapActionsPresentation from '../../presentation/map-actions'

type Props = {
  model: Backbone.Model
}

type State = {
  currentOverlayUrl: string
}

const getActionsWithIdPrefix = (actions: any, id: string) => {
  return actions.filter((action: any) => action.get('id').startsWith(id))
}

class MapActions extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)

    this.state = {
      currentOverlayUrl: this.props.model.get('currentOverlayUrl'),
    }
  }

  getActions = () => {
    return this.props.model.get('actions')
  }

  getMapActions = () => {
    return getActionsWithIdPrefix(
      this.getActions(),
      'catalog.data.metacard.map.'
    )
  }

  getOverlayActions = () => {
    const modelOverlayActions = getActionsWithIdPrefix(
      this.getActions(),
      'catalog.data.metacard.map.overlay.'
    )

    return modelOverlayActions.map((modelOverlayAction: any) => {
      return {
        description: modelOverlayAction.get('description'),
        url: modelOverlayAction.get('url'),
        overlayText: this.getOverlayText(modelOverlayAction.get('url')),
      }
    })
  }

  getOverlayText = (actionUrl: String) => {
    const overlayTransformerPrefix = 'overlay.'
    const overlayTransformerIndex = actionUrl.lastIndexOf(
      overlayTransformerPrefix
    )
    if (overlayTransformerIndex >= 0) {
      const overlayName = actionUrl.substr(
        overlayTransformerIndex + overlayTransformerPrefix.length
      )
      return 'Overlay ' + overlayName + ' on the map'
    }

    return ''
  }

  overlayImage = (event: any) => {
    const clickedOverlayUrl = event.target.getAttribute('data-url')
    const removeOverlay = clickedOverlayUrl === this.state.currentOverlayUrl

    if (removeOverlay) {
      this.props.model.unset('currentOverlayUrl', { silent: true })
      this.setState({ currentOverlayUrl: '' })
      wreqr.vent.trigger(
        'metacard:overlay:remove',
        this.props.model.get('metacard').get('id')
      )
    } else {
      this.props.model.set('currentOverlayUrl', clickedOverlayUrl, {
        silent: true,
      })
      this.setState({ currentOverlayUrl: clickedOverlayUrl })
      this.props.model
        .get('metacard')
        .set('currentOverlayUrl', clickedOverlayUrl)
      wreqr.vent.trigger('metacard:overlay', this.props.model.get('metacard'))
    }
  }

  render() {
    const hasMapActions = this.getMapActions().length !== 0
    return (
      <MapActionsPresentation
        hasMapActions={hasMapActions}
        overlayActions={this.getOverlayActions()}
        currentOverlayUrl={this.state.currentOverlayUrl}
        overlayImage={this.overlayImage}
      />
    )
  }
}

export default hot(module)(MapActions)
