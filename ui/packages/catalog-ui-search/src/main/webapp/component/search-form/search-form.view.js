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
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance')
const DropdownModel = require('../dropdown/dropdown')
const SearchFormInteractionsDropdownView = require('../dropdown/search-form-interactions/dropdown.search-form-interactions.view')
const wreqr = require('../../exports/wreqr.js')
const Router = require('../router/router.js')
const announcement = require('../announcement')
const Common = require('../../js/Common.js')
import styled from '../../react-component/styles/styled-components'

// TODO use vars from theme
const FormTitle = styled.h3`
  padding-bottom: 0.625rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  width: 100%;
`

const FormContents = styled.span`
  display: block;
`

// TODO 3 * 1.4rem = 4.2rem, should this be dynamically calculated?
const NewFormCircle = styled.div`
  font-size: 4.2rem;
  padding-top: 0.625rem;
`

class NewSearchForm extends React.Component {
  render() {
    return (
      <div>
        <NewFormCircle className="fa fa-plus-circle" />
        <h3>New Search Form</h3>
      </div>
    )
  }
}

class CustomSearchForm extends React.Component {
  constructor(props) {
    super(props)
  }
  render() {
    return (
      <div>
        <FormTitle data-help={this.props.title}>
          {this.props.title}
        </FormTitle>
        <div className="default-icon">
          <div className="fa fa-star" />
        </div>
        <FormContents>{this.props.createdOn}</FormContents>
        <FormContents>
          <span className="fa fa-cloud" />
          {this.props.createdBy}
        </FormContents>
        <span
          className="choice-actions is-button"
          title="Shows a list of actions to take on the search forms"
          data-help="Shows a list of actions to take on the search forms."
        />
      </div>
    )
  }
}

module.exports = Marionette.LayoutView.extend({
  template(props) {
    {
      if (props.type === 'new-form') {
        return (
          <NewSearchForm onClick={this.changeView}/>
        )
      } else if (props.type === 'custom') {
        return (
          <CustomSearchForm {...props} onClick={this.changeView}/>
        )
      } else if (props.type === 'new-result') {
        return (
          <div>
            <NewFormCircle className="fa fa-plus-circle" />
            <h3>New Result Form</h3>
          </div>
        )
      } else if (props.type == 'result') {
        return (
          <div>
            <h3 className="search-form-title" data-help={props.title}>
              {props.title}
            </h3>
            <FormContents>{props.createdOn}</FormContents>
            <FormContents>
              <span className="fa fa-cloud" />
              {props.createdBy}
            </FormContents>
            <span
              className="choice-actions is-button"
              title="Shows a list of actions to take on the result forms"
              data-help="Shows a list of actions to take on the result forms."
            />
          </div>
        )
      }
    }
  },
  tagName: CustomElements.register('search-form'),
  className() {
    return this.model.get('createdBy') === 'system' &&
      Router.attributes.path === 'forms(/)'
      ? 'systemSearchForm'
      : 'is-button'
  },
  events: {
    click: 'changeView',
  },
  modelEvents: {
    change: 'render',
  },
  regions: {
    searchFormActions: '.choice-actions',
  },
  initialize: function() {
    this.listenTo(this.model, 'change:type', this.changeView)
    this.handleDefault()
    this.listenTo(
      user.getQuerySettings(),
      'change:template',
      this.handleDefault
    )
  },
  serializeData: function() {
    const { createdOn, ...json } = this.model.toJSON()
    return { createdOn: Common.getMomentDate(createdOn), ...json }
  },
  onRender: function() {
    if (
      this.model.get('type') === 'basic' ||
      this.model.get('type') === 'text' ||
      this.model.get('type') === 'new-form' ||
      this.model.get('type') === 'new-result'
    ) {
      this.$el.addClass('is-static')
    } else {
      this.searchFormActions.show(
        new SearchFormInteractionsDropdownView({
          model: new DropdownModel(),
          modelForComponent: this.model,
          collectionWrapperModel: this.options.collectionWrapperModel,
          queryModel: this.options.queryModel,
          dropdownCompanionBehaviors: {
            navigation: {},
          },
        })
      )
    }
  },
  changeView: function() {
    let oldType = this.options.queryModel.get('type')
    switch (this.model.get('type')) {
      case 'new-form':
        this.options.queryModel.set({
          type: 'new-form',
          associatedFormModel: this.model,
          title: this.model.get('title'),
          filterTree: this.model.get('filterTemplate'),
        })
        if (oldType === 'new-form') {
          this.options.queryModel.trigger('change:type')
        }
        this.routeToSearchFormEditor('create')
        break
      case 'basic':
        this.options.queryModel.set('type', 'basic')
        if (oldType === 'new-form' || oldType === 'custom') {
          this.options.queryModel.set('title', 'Search Name')
        }
        user.getQuerySettings().set('type', 'basic')
        break
      case 'text':
        this.options.queryModel.set('type', 'text')
        if (oldType === 'new-form' || oldType === 'custom') {
          this.options.queryModel.set('title', 'Search Name')
        }
        user.getQuerySettings().set('type', 'text')
        break
      case 'custom':
        const sharedAttributes = this.model.transformToQueryStructure()
        if (
          Router.attributes.path === 'forms(/)' &&
          this.model.get('createdBy') !== 'system'
        ) {
          this.openEditor(sharedAttributes)
        } else {
          this.options.queryModel.set({
            type: 'custom',
            ...sharedAttributes,
          })
          if (oldType === 'custom') {
            this.options.queryModel.trigger('change:type')
          }
          user.getQuerySettings().set('type', 'custom')
        }
    }
    user.savePreferences()
    this.triggerCloseDropdown()
  },
  handleDefault: function() {
    this.$el.toggleClass(
      'is-default',
      user.getQuerySettings().isTemplate(this.model)
    )
  },
  triggerCloseDropdown: function() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  openEditor: function(sharedAttributes) {
    if (user.canWrite(this.model)) {
      this.model.set({
        ...sharedAttributes,
        id: this.model.get('id'),
        accessGroups: this.model.get('accessGroups'),
        accessIndividuals: this.model.get('accessIndividuals'),
        accessAdministrators: this.model.get('accessAdministrators'),
      })
      this.routeToSearchFormEditor(this.model.get('id'))
    } else {
      announcement.announce(
        {
          title: 'Error',
          message: `You have read-only permission on search form ${this.model.get(
            'title'
          )}.`,
          type: 'error',
        },
        3000
      )
    }
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
