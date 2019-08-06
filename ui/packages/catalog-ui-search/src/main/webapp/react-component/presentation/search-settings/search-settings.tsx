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
import withListenTo, {
  WithBackboneProps,
} from '../../container/backbone-container'
const user = require('../../../component/singletons/user-instance.js')
const properties = require('../../../js/properties.js')
const Property = require('../../../component/property/property.js')
const PropertyView = require('../../../component/property/property.view.js')
import MarionetteRegionContainer from '../../../react-component/container/marionette-region-container'
const QuerySettingsView = require('../../../component/query-settings/query-settings.view.js')
const QueryModel = require('../../../js/model/Query.js')
const ConfirmationView = require('../../../component/confirmation/confirmation.view.js')
import styled from '../../styles/styled-components'
import { hot } from 'react-hot-loader'

const Root = styled.div`
  overflow: hidden;
  padding: ${props => props.theme.minimumSpacing}
    ${props => props.theme.minimumSpacing};
`

const PropertyResultCount = styled.div`
  margin-bottom: ${props => props.theme.largeSpacing};
`

const QuerySettings = styled.div`
  .editor-header,
  .editor-footer {
    display: none;
  }
  .editor-properties {
    padding: 0px;
  }
`

const EditorFooter = styled<Props, 'div'>('div')`
  display: ${props => (props.showFooter ? 'block' : 'none')};
  button {
    display: inline-block;
    width: 50%;
  }
`

type Props = {
  onClose?: () => void
  model?: any
  showFooter: boolean
} & WithBackboneProps

class SearchSettings extends React.Component<Props, {}> {
  querySettingsView: any
  propertyView: any
  constructor(props: Props) {
    super(props)
    this.setQuerySettingsView()
    this.setPropertyView()
  }
  render() {
    return (
      <Root>
        <div className="editor-properties">
          <PropertyResultCount>
            <MarionetteRegionContainer
              view={this.propertyView}
              replaceElement
            />
          </PropertyResultCount>
          <div className="is-header">Defaults</div>
          <QuerySettings className="property-search-settings">
            <MarionetteRegionContainer view={this.querySettingsView} />
          </QuerySettings>
        </div>
        <EditorFooter {...this.props}>
          <button className="is-negative" onClick={this.triggerCancel}>
            <span className="fa fa-times" />
            <span>Cancel</span>
          </button>
          <button className="is-positive" onClick={this.triggerSave}>
            <span className="fa fa-floppy-o" />
            <span>Save</span>
          </button>
        </EditorFooter>
      </Root>
    )
  }
  triggerSave = () => {
    this.updateResultCountSettings()
    this.updateSearchSettings()
    this.props.listenTo(
      ConfirmationView.generateConfirmation({
        prompt: 'Do you want to apply the new defaults to this search?',
        no: 'No',
        yes: 'Apply',
      }),
      'change:choice',
      (confirmation: any) => {
        if (confirmation.get('choice')) {
          this.props.model.applyDefaults()
        }
      }
    )
    user.savePreferences()
    if (!!this.props.onClose) {
      this.props.onClose()
    }
  }
  updateResultCountSettings = () => {
    user.getPreferences().set({
      resultCount: this.propertyView.model.getValue()[0],
    })
  }
  updateSearchSettings = () => {
    user
      .getPreferences()
      .get('querySettings')
      .set(this.querySettingsView.toJSON())
  }
  triggerCancel = () => {
    this.setPropertyView()
    this.setQuerySettingsView()
    if (!!this.props.onClose) {
      this.props.onClose()
    }
    this.forceUpdate()
  }
  getUserResultCount = () => {
    return user
      .get('user')
      .get('preferences')
      .get('resultCount')
  }
  setPropertyView = () => {
    this.propertyView = new PropertyView({
      model: new Property({
        label: 'Number of Search Results',
        value: [this.getUserResultCount()],
        min: 1,
        max: properties.resultCount,
        type: 'RANGE',
        isEditing: true,
      }),
    })
  }
  setQuerySettingsView = () => {
    this.querySettingsView = new QuerySettingsView({
      model: new QueryModel.Model(),
      inSearchSettings: true,
    })
  }
  componentWillUnmount = () => {
    if (!this.props.showFooter) {
      this.updateResultCountSettings()
      this.updateSearchSettings()
      user.savePreferences()
    }
  }
}

export default hot(module)(withListenTo(SearchSettings))
