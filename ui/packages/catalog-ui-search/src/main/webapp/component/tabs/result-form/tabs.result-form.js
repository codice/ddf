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
/* global require */
const Tabs = require('../tabs.js')
const FormContainer = require('../../search-form/form-tab-container.view')
const ResultFormCollectionView = require('../../result-form/result-form.collection.view')
const ResultFormCollection = require('../../result-form/result-form-collection-instance.js')

const tabs = {
  'Result Forms': FormContainer({
    collection: ResultFormCollection,
    childView: ResultFormCollectionView,
  }),
}

module.exports = Tabs.extend({
  defaults: { tabs },
})
