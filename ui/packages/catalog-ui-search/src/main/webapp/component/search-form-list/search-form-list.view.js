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

const CustomElements = require('../../js/CustomElements.js')
const Marionette = require('marionette')
const Router = require('../router/router.js')
const user = require('../singletons/user-instance')
const SearchForm = require('../search-form/search-form')
import React from 'react'
import { lighten, readableColor, transparentize } from 'polished'
import styled from '../../react-component/styles/styled-components'

const ListContainer = styled.div`
  max-height: 50vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
`

const ScrollableContainer = styled.div`
  overflow-y: auto;
`

const ListItem = styled.div`
  cursor: pointer;
  display: block;
  line-height: ${props => props.theme.minimumButtonSize};
  padding: 0px ${props => props.theme.largeSpacing};
  box-sizing: border-box;
`

const HoverableListItem = styled(ListItem)`
  &:hover {
    background: ${props =>
      transparentize(0.9, readableColor(props.theme.backgroundDropdown))};
    box-shadow: inset 0px 0px 0px 1px ${props => props.theme.primaryColor};
  }
`

const WarningItem = styled(ListItem)`
  text-align: center;
  color: ${props => lighten(0.2, props.theme.warningColor)};
`

const NothingFound = () => <WarningItem>Nothing Found</WarningItem>

const NoSearchForms = () => <ListItem>No search forms are available</ListItem>

const SearchFormItem = ({ title, onClick }) => {
  return <HoverableListItem onClick={onClick}>{title}</HoverableListItem>
}

const FilterPadding = styled.div`
  box-sizing: border-box;
  padding: 0px ${props => props.theme.minimumSpacing}
    ${props => props.theme.minimumSpacing}
    ${props => props.theme.minimumSpacing};
`
class SearchForms extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      filter: '',
    }
  }
  render() {
    const { filter } = this.state
    const { forms, onClick } = this.props

    const filteredForms = forms.filter(form =>
      form.title.toLowerCase().match(filter.toLowerCase())
    )

    return (
      <ListContainer>
        <FilterPadding>
          <input
            style={{ width: '100%' }}
            value={filter}
            onChange={e => this.setState({ filter: e.target.value })}
            placeholder="Type to filter"
          />
        </FilterPadding>
        <ScrollableContainer>
          {forms.length === 0 ? <NoSearchForms /> : null}
          {filteredForms.map(form => (
            <SearchFormItem
              title={form.title}
              key={form.id}
              onClick={() => onClick(form)}
            />
          ))}
          {forms.length !== 0 && filteredForms.length === 0 ? (
            <NothingFound />
          ) : null}
        </ScrollableContainer>
      </ListContainer>
    )
  }
}

module.exports = Marionette.ItemView.extend({
  className: 'composed-menu',
  template(forms) {
    return (
      <SearchForms
        forms={forms}
        onClick={form =>
          this.changeView(new SearchForm(form), this.model.get('currentQuery'))
        }
      />
    )
  },
  serializeData: function() {
    return this.model.get('searchForms').toJSON()
  },
  changeView: function(selectedForm, currentQuery) {
    const sharedAttributes = selectedForm.transformToQueryStructure()
    currentQuery.set({
      type: 'custom',
      ...sharedAttributes,
    })
    if (currentQuery.get('type') === 'custom') {
      currentQuery.trigger('change:type')
    }
    user.getQuerySettings().set('type', 'custom')
    user.savePreferences()
    this.triggerCloseDropdown()
  },
  triggerCloseDropdown: function() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
})
