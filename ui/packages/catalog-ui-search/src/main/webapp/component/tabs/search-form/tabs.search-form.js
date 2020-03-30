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

import React from 'react'
import { TabMessage } from '../../search-form/search-form-presentation'
const Tabs = require('../tabs.js')
const user = require('../../singletons/user-instance')
const SearchFormCollectionView = require('../../search-form/search-form.collection.view')
const SearchFormCollection = require('../../search-form/search-form-collection-instance')
const FormContainer = require('../../search-form/form-tab-container.view')
const properties = require('../../../js/properties')

const formsTitle = properties.i18n['forms.title'] || 'Forms'

const tabChildViewOptions = {
  [`My Search ${formsTitle}`]: {
    filter: child => child.get('createdBy') === user.getUserId(),
    showNewForm: true,
  },
  [`Shared Search ${formsTitle}`]: {
    type: 'Shared',
    filter: child =>
      child.get('createdBy') !== 'system' &&
      child.get('createdBy') !== user.getUserId() &&
      user.canRead(child),
  },
  [`System Search ${formsTitle}`]: {
    type: 'System',
    filter: child => child.get('createdBy') === 'system',
    message: (
      <TabMessage>
        These are system search {formsTitle.toLowerCase()} and{' '}
        <b>cannot be changed</b>
      </TabMessage>
    ),
  },
}

const tabs = Object.keys(tabChildViewOptions).reduce((tabs, title) => {
  tabs[title] = FormContainer({
    collection: SearchFormCollection,
    childView: SearchFormCollectionView,
    childViewOptions: tabChildViewOptions[title],
  })
  return tabs
}, {})

module.exports = Tabs.extend({
  defaults: { tabs },
})
