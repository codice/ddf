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
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance')
const DropdownModel = require('../dropdown/dropdown')
const SearchFormInteractionsDropdownView = require('../dropdown/search-form-interactions/dropdown.search-form-interactions.view')
const wreqr = require('../../exports/wreqr.js')
const announcement = require('../announcement')
const Common = require('../../js/Common.js')
import React from 'react'
import styled from '../../react-component/styles/styled-components'

const Item = styled(({ className, ...props }) => {
  return <div className={className + ' is-button'} {...props} />
})`
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

const NewFormText = styled.h3`
  line-height: 2em;
`

const NewForm = ({ onClick, label }) => {
  return (
    <Item
      className="is-button"
      style={{ textAlign: 'center' }}
      onClick={onClick}
    >
      <NewFormCircle className="fa fa-plus-circle" />
      <NewFormText>{label}</NewFormText>
    </Item>
  )
}

export { Item, NewForm }

const FormTitle = styled.h3`
  padding-bottom: ${props => props.theme.minimumSpacing};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  width: 100%;
`

const FormContents = styled.span`
  display: block;
`

const DefaultIcon = styled.div`
  position: absolute;
  right: 0;
  top: 0;
`

const RelativeWrapper = styled.div`
  position: relative;
  height: 100%;
`

const ThreeDotMenu = styled.span`
  position: absolute;
  right: 0;
  bottom: 0;
  width: ${props => props.theme.minimumButtonSize};
  text-align: center;
`

const Actions = props => {
  const { title } = props
  return (
    <ThreeDotMenu
      className="choice-actions is-button"
      data-help={title}
      title={title}
    />
  )
}

const CustomSearchForm = props => {
  const { title, createdOn, createdBy, isDefault } = props
  return (
    <RelativeWrapper>
      <FormTitle data-help={title}>{title}</FormTitle>
      {isDefault ? <DefaultIcon className="fa fa-star" /> : null}
      <FormContents>{createdOn}</FormContents>
      <FormContents>
        <span className="fa fa-cloud" />
        {' ' + createdBy}
      </FormContents>
      <Actions title="Shows a list of actions to take on the search forms" />
    </RelativeWrapper>
  )
}

export default Marionette.LayoutView.extend({
  template(props) {
    const isDefault = user.getQuerySettings().isTemplate(this.model)
    if (props.type === 'custom') {
      return (
        <CustomSearchForm
          {...props}
          isDefault={isDefault}
          onClick={this.changeView}
        />
      )
    } else if (props.type == 'result') {
      return (
        <RelativeWrapper>
          <FormTitle data-help={props.title}>{props.title}</FormTitle>
          <FormContents>{props.createdOn}</FormContents>
          <FormContents>
            <span className="fa fa-cloud" />
            {' ' + props.createdBy}
          </FormContents>
          <Actions title="Shows a list of actions to take on the result forms" />
        </RelativeWrapper>
      )
    }
  },
  tagName: CustomElements.register('search-form'),
  events: {
    click: 'changeView',
  },
  modelEvents: {
    change: 'render',
  },
  regions: {
    searchFormActions: '.choice-actions',
  },
  initialize() {
    this.listenTo(this.model, 'change:type', this.changeView)
    this.listenTo(user.getQuerySettings(), 'change:template', this.render)
  },
  serializeData() {
    const { createdOn, ...json } = this.model.toJSON()
    return { createdOn: Common.getMomentDate(createdOn), ...json }
  },
  onRender() {
    if (this.model.get('type') === 'new-result') {
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
  changeView() {
    // Don't allow users to edit system forms
    if (this.model.get('createdBy') === 'system') {
      return
    }
    const sharedAttributes = this.model.transformToQueryStructure()
    this.openEditor(sharedAttributes)
  },
  openEditor(sharedAttributes) {
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
  routeToSearchFormEditor(newSearchFormId) {
    const fragment = `forms/${newSearchFormId}`
    wreqr.vent.trigger('router:navigate', {
      fragment,
      options: {
        trigger: true,
      },
    })
  },
})
