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

import React from 'react'
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const WorkspaceContentTabs = require('../tabs/workspace-content/tabs-workspace-content.js')
const WorkspaceContentTabsView = require('../tabs/workspace-content/tabs-workspace-content.view.js')
const store = require('../../js/store.js')
const GoldenLayoutView = require('../golden-layout/golden-layout.view.js')
const LoadingCompanionView = require('../loading-companion/loading-companion.view.js')

import { ChangeBackground } from '../../react-component/styles/mixins/change-background'
import MultiSelectActions from '../../react-component/container/multi-select-actions'
import styled from '../../react-component/styles/styled-components'

const ContentLeft = styled.div`
  ${props => {
    return ChangeBackground(props.theme.backgroundAccentContent)
  }};
  border-right: 1px solid
    fade(contrast(${props => props.theme.backgroundAccentContent}), 5%);
  width: calc(9.55 * ${props => props.theme.minimumButtonSize});
  left: 0%;
  top: 0%;
  transition: width ${props => props.theme.coreTransitionTime} ease-in-out;
  position: absolute;
  height: 100%;
  vertical-align: top;
  overflow: hidden;
`

const ContentRight = styled.div`
  width: calc(100% - 9.55 * ${props => props.theme.minimumButtonSize});
  transition: width ${props => props.theme.coreTransitionTime} ease-in-out;
  right: 0%;
  top: 0%;
  position: absolute;
  height: 100%;
  vertical-align: top;
  overflow: hidden;
  display: flex;
  flex-direction: column;
`
const Visualizations = styled.div`
  ${props => {
    return ChangeBackground(props.theme.backgroundContent)
  }};
  flex: 1;
`

const Root = styled.div`
  height: 100%;
  width: 100%;
  position: relative;
  z-index: @zIndexContent;
`

const ContentView = Marionette.LayoutView.extend({
  template() {
    return (
      <Root>
        <ContentLeft className="content-left" />
        <ContentRight>
          <MultiSelectActions selectionInterface={this.selectionInterface} />
          <Visualizations className="content-right" />
        </ContentRight>
      </Root>
    )
  },
  tagName: CustomElements.register('content'),
  regions: {
    contentLeft: '.content-left',
    contentRight: '.content-right',
  },
  selectionInterface: store,
  initialize() {
    this._mapView = new GoldenLayoutView({
      selectionInterface: store.get('content'),
      configName: 'goldenLayout',
    })
  },
  onFirstRender() {
    this.listenTo(
      store.get('content'),
      'change:currentWorkspace',
      this.handleWorkspaceChange
    )
    this.updateContentLeft()
    if (this._mapView) {
      this.contentRight.show(this._mapView)
    }
  },
  handleWorkspaceChange(contentModel) {
    if (
      contentModel &&
      Object.keys(contentModel.changedAttributes()).some(
        key => key === 'currentWorkspace'
      )
    ) {
      this.handlePreviousWorkspace(
        contentModel.previousAttributes().currentWorkspace
      )
      this.updateContentLeft()
    }
  },
  handlePreviousWorkspace(previousWorkspace) {
    if (previousWorkspace) {
      this.stopListening(previousWorkspace, 'partialSync')
    }
  },
  startLoading() {
    LoadingCompanionView.beginLoading(this)
  },
  endLoading() {
    LoadingCompanionView.endLoading(this)
  },
  updateContentLeft() {
    const currentWorkspace = store.get('content').get('currentWorkspace')
    store.clearSelectedResults()
    this.contentLeft.empty()
    if (currentWorkspace === undefined) {
      this.endLoading()
      return
    }
    if (currentWorkspace.isPartial()) {
      this.startLoading()
      this.stopListening(currentWorkspace, 'partialSync')
      this.listenToOnce(currentWorkspace, 'partialSync', this.updateContentLeft)
      currentWorkspace.fetchPartial()
    } else {
      this.contentLeft.show(
        new WorkspaceContentTabsView({
          model: new WorkspaceContentTabs(),
          selectionInterface: store.get('content'),
        })
      )
      this.endLoading()
    }
  },
  _mapView: undefined,
})

module.exports = ContentView
