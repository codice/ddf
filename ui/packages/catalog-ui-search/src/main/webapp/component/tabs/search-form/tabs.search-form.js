/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

import React from 'react'
import { TabMessage } from '../../search-form/search-form-presentation'
const Tabs = require('../tabs.js')
const user = require('../../singletons/user-instance')
const SearchFormContainer = require('../../search-form/search-form-tab-container.view')

const data = {
  'My Search Forms': {
    filter: child => child.get('createdBy') === user.getEmail(),
    showNewForm: true,
  },
  'Shared Search Forms': {
    type: 'Shared',
    filter: child =>
      child.get('createdBy') !== 'system' &&
      child.get('createdBy') !== user.getEmail() &&
      user.canRead(child),
  },
  'System Search Forms': {
    type: 'System',
    filter: child => child.get('createdBy') === 'system',
    children: (
      <TabMessage>
        These are system search forms and <b>cannot be changed</b>
      </TabMessage>
    ),
  },
}

const tabs = Object.keys(data).reduce((tabs, title) => {
  tabs[title] = SearchFormContainer(data[title])
  return tabs
}, {})

module.exports = Tabs.extend({
  defaults: { tabs },
})
