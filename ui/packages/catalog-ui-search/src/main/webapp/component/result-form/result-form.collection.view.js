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
/* global require */
const React = require('react')
const Marionette = require('marionette')
const ResultFormView = require('./result-forms.view.js')
const CustomElements = require('../../js/CustomElements.js')
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import styled from '../../react-component/styles/styled-components'

// TODO Copied from search-form.collection.view... consolidate?
const Item = styled.div`
  display: inline-block;
  padding: ${props => props.theme.mediumSpacing};
  margin: ${props => props.theme.mediumSpacing};
  width: calc(8 * ${props => props.theme.minimumButtonSize});
  height: calc(4 * ${props => props.theme.minimumButtonSize});
  text-align: left;
  vertical-align: top;
  position: relative;
`

module.exports = Marionette.ItemView.extend({
  initialize(options) {
    this.model = this.options.collection
    this.listenTo(this.model, 'add', this.render)
  },
  template() {
    return (
      <React.Fragment>
        { this.model.map((child) => {
          return (
            <Item className='is-button' key={child.cid}>
              <MarionetteRegionContainer
                view={ResultFormView}
                viewOptions={{
                  model: child,
                  queryModel: this.options.queryModel,
                  collectionWrapperModel: this.options.collectionWrapperModel
                }}
              />
             </Item>
          )
        })}
      </React.Fragment>
    )
  },
  className: 'is-list is-inline has-list-highlighting',
  tagName: CustomElements.register('result-forms'),
})
