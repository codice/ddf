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

const Tabs = require('../tabs.js')
const MySearchFormCollectionView = require('../../search-form/search-form-tab-container.view.js')
const MySearchSharingFormCollectionView = require('../../search-form/forms-sharing/search-form-sharing-tab-container.view.js')
const MySearchSystemFormCollectionView = require('../../search-form/forms-system/search-form-system-tab-container.view.js')

module.exports = Tabs.extend({
  defaults: {
    tabs: {
      'My Search Forms': MySearchFormCollectionView,
      'Shared Search Forms': MySearchSharingFormCollectionView,
      'System Search Forms': MySearchSystemFormCollectionView,
    },
  },
})
