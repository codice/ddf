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
import { hot } from 'react-hot-loader'
import styled from '../../styles/styled-components'

import { Button, buttonTypeEnum } from '../button'

import MarionetteRegionContainer from '../../container/marionette-region-container'
import * as FilterBuilderView from '../../../component/filter-builder/filter-builder.view'
import * as FilterBuilderModel from '../../../component/filter-builder/filter-builder'

type Props = {
  removeFilter: () => void
  saveFilter: (transformToCql: () => any) => void
  hasFilter: Boolean
  resultFilter?: {}
  isList?: Boolean
}

const Root = styled<Props, 'div'>('div')`
  display: block;
  padding: ${props => props.theme.minimumSpacing};

  .editor-footer {
    padding-top: 30px;
    white-space: nowrap;

    button {
      display: inline-block;
      width: ${props => (props.hasFilter && '49%') || '100%'};
      ${props => props.hasFilter && 'margin-right: 1%;'};
    }

    .footer-remove {
      display: ${props => (props.hasFilter && 'inline-block') || 'none'};
    }
  }
`

class View extends React.Component<Props, { filterCqlCallback: () => any }> {
  view: any
  constructor(props: Props) {
    super(props)
    this.state = {
      filterCqlCallback: () => {},
    }
  }

  onViewBind = (view: any) => {
    this.view = view

    this.view.turnOnEditing()
    this.view.turnOffNesting()
    this.props.resultFilter && this.view.deserialize(this.props.resultFilter)

    this.setState({
      filterCqlCallback: this.view.transformToCql.bind(this.view),
    })
  }

  onSave = () => {
    this.props.saveFilter(this.state.filterCqlCallback())
  }

  render = () => {
    return (
      <Root
        {...this.props}
        className={`result-filter ${this.props.hasFilter && 'has-filter'} ${this
          .props.isList && 'is-list'}`}
      >
        <div className="editor-properties">
          <MarionetteRegionContainer
            className="editor-properties"
            view={FilterBuilderView}
            bindView={this.onViewBind.bind(this)}
            viewOptions={{
              model: new FilterBuilderModel({ isResultFilter: true }),
            }}
          />
        </div>
        <div className="editor-footer">
          <Button
            className="footer-remove"
            buttonType={buttonTypeEnum.negative}
            text="Remove Filter"
            onClick={this.props.removeFilter}
          />
          <Button
            className="footer-save"
            buttonType={buttonTypeEnum.positive}
            text="Save Filter"
            onClick={this.onSave.bind(this)}
          />
        </div>
      </Root>
    )
  }
}

export default hot(module)(View)
