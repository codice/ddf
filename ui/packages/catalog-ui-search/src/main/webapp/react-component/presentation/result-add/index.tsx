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
import MarionetteRegionContainer from '../../container/marionette-region-container'
import * as PopoutView from '../../../component/dropdown/popout/dropdown.popout.view'
import * as ListCreateView from '../../../component/list-create/list-create.view'

type Result = {
  matchesFilter?: boolean
  alreadyContains?: boolean
  icon: any
  title: string
  id: any
}

type Props = {
  items?: [Result]
  model: any
  bookmarkHandler: (id: any) => void
}

const Root = styled.div`
  padding: ${props => props.theme.minimumSpacing};
`

const Button = styled.button`
  width: 100%;
  display: block;
  padding: 0px ${props => props.theme.largeSpacing};
  text-align: left;
  height: ${props => props.theme.minimumButtonSize};
  line-height: ${props => props.theme.minimumButtonSize};

  .composed-button {
    width: 100%;
    display: block;
  }
`

const IconSpan = styled.span`
  margin-right: ${props => props.theme.mediumSpacing};
`

const getIconStyle = (result: Result) => {
  if (result.alreadyContains) {
    return 'fa-check-square'
  }

  if (result.matchesFilter) {
    return 'fa-square'
  }

  return 'fa-ban'
}

const renderButton = (result: Result, props: Props) => (
  <Button
    className={`is-button is-neutral ${!result.alreadyContains &&
      !result.matchesFilter &&
      'is-disabled'}`}
    key={result.id}
    onClick={() => props.bookmarkHandler(result.id)}
  >
    <IconSpan className={`fa ${getIconStyle(result)}`} />
    <span className={result.icon} />
    <span>{result.title}</span>
  </Button>
)

export default hot(module)((props: Props) => (
  <Root {...props}>
    <div className="is-header">Add / remove from lists</div>
    <div className="is-divider" />
    {props.items && props.items.map(item => renderButton(item, props))}
    {props.items && props.items.length > 0 && <div className="is-divider" />}
    <MarionetteRegionContainer
      view={(viewProps: any) => PopoutView.createSimpleDropdown(viewProps)}
      className="is-button is-neutral composed-button create-new-list"
      viewOptions={{
        modelForComponent: props.model,
        componentToShow: ListCreateView,
        leftIcon: 'fa fa-plus',
        label: 'Create New List',
        options: {
          withBookmarks: true,
        },
      }}
    />
  </Root>
))
