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

const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance')
const DropdownModel = require('../dropdown/dropdown')
const SearchFormInteractionsDropdownView = require('../dropdown/search-form-interactions/dropdown.search-form-interactions.view')
const wreqr = require('../../exports/wreqr.js')
const announcement = require('../announcement')
const Common = require('../../js/Common.js')
const properties = require('../../js/properties')
import React from 'react'
import styled from 'styled-components'

const formTitle = properties.i18n['form.title']
  ? properties.i18n['form.title'].toLowerCase()
  : 'form'
const formsTitle = properties.i18n['forms.title']
  ? properties.i18n['forms.title'].toLowerCase()
  : 'forms'

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

const Author = styled.span`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
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
      <Author title={createdBy}>
        <span className="fa fa-cloud" />
        {' ' + createdBy}
      </Author>
      <Actions
        title={'Shows a list of actions to take on the search ' + formsTitle}
      />
    </RelativeWrapper>
  )
}

export default Marionette.LayoutView.extend({
  template(props) {
    const isDefault = user.getQuerySettings().isDefaultTemplate(this.model)
    if (props.type === 'custom') {
      return (
        <CustomSearchForm
          {...props}
          isDefault={isDefault}
          onClick={this.changeView}
        />
      )
    } else if (props.type == 'result') {
      const isDefault =
        user.getQuerySettings().get('defaultResultFormId') === props.id
      return (
        <RelativeWrapper>
          <FormTitle data-help={props.title}>{props.title}</FormTitle>
          {isDefault ? <DefaultIcon className="fa fa-star" /> : null}
          <FormContents>{props.createdOn}</FormContents>
          <Author title={props.createdBy}>
            <span className="fa fa-cloud" />
            {' ' + props.createdBy}
          </Author>
          <Actions
            title={
              'Shows a list of actions to take on the result ' + formsTitle
            }
          />
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
    this.listenTo(
      user.getQuerySettings(),
      'change:defaultResultFormId',
      this.render
    )
  },
  serializeData() {
    const { createdOn, ...json } = this.model.toJSON()
    return { createdOn: Common.getMomentDate(createdOn), ...json }
  },
  onRender() {
    const newResult = this.model.get('type') === 'new-result'
    const noActions =
      this.model.get('type') === 'result' && !user.canWrite(this.model)
    if (newResult || noActions) {
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
      announcement.announce(
        {
          title: 'Error',
          message: `System search ${formsTitle} are not editable.`,
          type: 'error',
        },
        3000
      )
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
          message: `You have read-only permission on search ${formTitle} ${this.model.get(
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
