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

import { MarionetteRegionContainer } from '../../container/marionette-region-container'

const FilterBuilderView = require('../../../component/filter-builder/filter-builder.view')

type Props = {
  removeFilter: () => void
  saveFilter: (transformToCql: () => any) => void
  hasFilter: Boolean
  resultFilter?: {}
  isList?: Boolean
}

type State = {
  filterCqlCallback: () => any
  resultFilter?: {}
}

type BuilderView = {
  turnOnEditing: () => void
  turnOffNesting: () => void
  transformToCql: () => string
}

const Root = styled.div`
  display: block;
  padding: ${props => props.theme.minimumSpacing};
`

const EditorFooter = styled.div`
  padding-top: 30px;
  white-space: nowrap;
`

const SaveButtonStyle = (props: Props) =>
  ({
    display: 'inline-block',
    width: (props.hasFilter && '50%') || '100%',
  } as React.CSSProperties)

const RemoveButtonStyle = {
  display: 'inline-block',
  width: '49%',
  marginRight: '1%',
} as React.CSSProperties

class BuilderViewRegionContainer extends MarionetteRegionContainer {
  onceInDOM(callback: () => void) {
    const view = this.props.view

    super.onceInDOM(() => {
      callback()
      view.turnOnEditing()
      view.turnOffNesting()
    })
  }
}

class View extends React.Component<Props, State> {
  view: BuilderView
  constructor(props: Props) {
    super(props)
    this.state = {
      filterCqlCallback: () => {},
      resultFilter: props.resultFilter,
    }
  }

  getOrCreateFilterBuilderView = () => {
    if (!this.view)
      this.view = new FilterBuilderView({
        filter: this.state.resultFilter,
        isResultFilter: true,
      })

    return this.view
  }

  componentDidMount = async () =>
    this.setState({
      filterCqlCallback: this.getOrCreateFilterBuilderView().transformToCql.bind(
        this.getOrCreateFilterBuilderView()
      ),
    })

  onSave = () => {
    this.props.saveFilter(this.state.filterCqlCallback())
  }

  render = () => (
    <Root
      {...this.props}
      className={`result-filter ${this.props.hasFilter && 'has-filter'} ${this
        .props.isList && 'is-list'}`}
    >
      <BuilderViewRegionContainer view={this.getOrCreateFilterBuilderView()} />
      <EditorFooter>
        {this.props.hasFilter && (
          <Button
            rootStyle={RemoveButtonStyle}
            buttonType={buttonTypeEnum.negative}
            text="Remove Filter"
            onClick={this.props.removeFilter}
          />
        )}
        <Button
          rootStyle={SaveButtonStyle(this.props)}
          buttonType={buttonTypeEnum.positive}
          text="Save Filter"
          onClick={this.onSave.bind(this)}
        />
      </EditorFooter>
    </Root>
  )
}

export default hot(module)(View)
