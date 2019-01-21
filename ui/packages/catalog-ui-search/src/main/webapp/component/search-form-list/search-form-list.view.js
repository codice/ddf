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
const React = require('react')
const Router = require('../router/router.js')
const user = require('../singletons/user-instance')
const SearchForm = require('../search-form/search-form')

const NoSearchForms = () => {
  return <div>No search forms are available</div>
}

module.exports = Marionette.ItemView.extend({
  tagName: CustomElements.register('search-form-list'),
  className: 'composed-menu',
  template(props) {
    return (
      <React.Fragment>
        { props.length === 0
          ? <NoSearchForms/>
          : null
        }
        {props.map(form => (
          <div
            key={form.id}
            onClick={() =>
              this.changeView(
                new SearchForm(form),
                this.model.get('currentQuery')
              )
            }
          >
            {form.title}
          </div>
        ))}
      </React.Fragment>
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
