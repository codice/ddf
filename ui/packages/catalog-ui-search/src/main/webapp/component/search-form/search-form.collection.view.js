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
/*global require*/
const React = require('react')
const Marionette = require('marionette')
const SearchFormView = require('./search-form.view')
const CustomElements = require('../../js/CustomElements')
const user = require('../singletons/user-instance')
const wreqr = require('../../exports/wreqr.js')
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import styled, {
  ThemeProvider,
} from '../../react-component/styles/styled-components'

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

const NewFormCircle = styled.div`
  font-size: calc(3 * ${props => props.theme.largeFontSize});
  padding-top: ${props => props.theme.minimumSpacing};
`

const NewSearchForm = ({ onClick }) => {
  return (
    <Item
      className="is-button"
      style={{ textAlign: 'center' }}
      onClick={onClick}
    >
      <NewFormCircle className="fa fa-plus-circle" />
      <h3 style={{ lineHeight: '2em' }}>New Search Form</h3>
    </Item>
  )
}

module.exports = Marionette.ItemView.extend({
  tagName: CustomElements.register('my-search-forms'),
  className: 'is-list is-inline has-list-highlighting',
  initialize: function(options) {
    this.model = this.options.collection
    this.filter = this.options.filter
    this.listenTo(this.model, 'add', this.render)
  },
  template() {
    return (
      <React.Fragment>
        {this.options.showNewForm ? (
          <NewSearchForm onClick={this.handleNewForm.bind(this)} />
        ) : null}
        {this.model.filter(child => this.doFilter(child)).map(child => {
          return (
            <Item className="is-button" key={child.get('id')}>
              <MarionetteRegionContainer
                view={SearchFormView}
                viewOptions={{
                  model: child,
                  queryModel: this.options.queryModel,
                  collectionWrapperModel: this.options.collectionWrapperModel,
                }}
              />
            </Item>
          )
        })}
      </React.Fragment>
    )
  },
  handleNewForm() {
    this.options.queryModel.set({
      type: 'new-form',
      associatedFormModel: this.model,
    })
    user.getQuerySettings().set('type', 'new-form')
    this.routeToSearchFormEditor('create')
  },
  doFilter(child) {
    if (typeof this.options.filter !== 'function') {
      // Show all children if no filter is provided
      return true
    }
    return this.options.filter(child)
  },
  routeToSearchFormEditor: function(newSearchFormId) {
    const fragment = `forms/${newSearchFormId}`
    wreqr.vent.trigger('router:navigate', {
      fragment,
      options: {
        trigger: true,
      },
    })
  },
})
