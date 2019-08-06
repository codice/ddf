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
import styled from '../../styles/styled-components'
import MarionetteRegionContainer from '../../container/marionette-region-container'
import { Button, buttonTypeEnum } from '../button'

const SortItemCollectionView = require('../../../component/sort/sort.view.js')

type Props = {
  removeSort: () => void
  saveSort: () => void
  hasSort: Boolean
  collection: Backbone.Collection<Backbone.Model>
}

const Root = styled<Props, 'div'>('div')`
  display: block;
  padding: ${props => props.theme.minimumSpacing};
  min-width: 500px;

  .editor-footer {
    padding-top: ${props => props.theme.minimumSpacing};
  }

  .footer-remove {
    display: ${props => (props.hasSort ? `inline-block` : `none`)};
    width: 50%;
  }

  .footer-save {
    display: inline-block;
    width: ${props => (props.hasSort ? `50%` : `100%`)};
  }
`

const render = (props: Props) => {
  const { removeSort, saveSort, collection } = props
  return (
    <Root {...props}>
      <MarionetteRegionContainer
        view={SortItemCollectionView}
        viewOptions={{
          collection: collection,
        }}
      />
      <div className="editor-footer">
        <Button
          className="footer-remove"
          buttonType={buttonTypeEnum.negative}
          text="Remove Sort"
          onClick={removeSort}
        />
        <Button
          buttonType={buttonTypeEnum.positive}
          className="footer-save"
          text="Save Sort"
          onClick={saveSort}
        />
      </div>
    </Root>
  )
}

export default hot(module)(render)
