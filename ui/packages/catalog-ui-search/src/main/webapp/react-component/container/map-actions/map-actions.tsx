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

import { hot } from 'react-hot-loader'
import * as React from 'react'
const wreqr = require('../../../js/wreqr.js')
const _ = require('underscore')

type Props = {
  model: Backbone.Model
}

type State = {
  currentOverlayUrl: string
}

const getActionsWithIdPrefix = (actions: any, id: any) => {
  return actions.filter((action: any) => action.get('id').indexOf(id) === 0)
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
    let modelOverlayActions = getActionsWithIdPrefix(
      this.getActions(),
      'catalog.data.metacard.map.overlay.'
    )

    return _.map(modelOverlayActions, (modelOverlayAction: any) => {
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
      let overlayName = actionUrl.substr(
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
    this.render()
  }

  render() {
    const { currentOverlayUrl } = this.state
    return (
      this.getMapActions().length !== 0 && (
        <>
          <div className="is-header">Map:</div>
          <div className="is-divider" />
          <div className="actions">
            {this.getOverlayActions().map((overlayAction: any) => {
              return (
                <a
                  style={{
                    cursor: 'pointer',
                  }}
                  data-url={overlayAction.url}
                  title={overlayAction.description}
                  onClick={this.overlayImage}
                  key={overlayAction.url}
                >
                  {overlayAction.overlayText}
                  {overlayAction.url === currentOverlayUrl ? ' (remove)' : ''}
                </a>
              )
            })}
          </div>
          <div className="is-divider" />
        </>
      )
    )
  }
}

export default hot(module)(MapActions)
