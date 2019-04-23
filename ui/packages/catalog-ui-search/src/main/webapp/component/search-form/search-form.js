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

const Backbone = require('backbone')
const user = require('../singletons/user-instance.js')

module.exports = Backbone.Model.extend({
  defaults() {
    return {
      title: '',
      description: '',
      createdBy: user.getEmail(),
      owner: user.getEmail(),
      createdOn: Date.now(),
      type: 'custom',
      id: undefined,
      filterTemplate: { property: 'anyText', value: '', type: 'ILIKE' },
      descriptors: [],
      accessIndividuals: [],
      accessGroups: [],
      accessAdministrators: [],
      querySettings: {},
    }
  },
  transformToQueryStructure() {
    const querySettings = this.get('querySettings')
    return {
      title: this.get('title'),
      filterTree: this.get('filterTemplate'),
      src: (querySettings && querySettings.src) || '',
      federation: (querySettings && querySettings.federation) || 'enterprise',
      sorts:
        querySettings && querySettings.sorts
          ? querySettings.sorts.map(sort => ({
              attribute: sort.split(',')[0],
              direction: sort.split(',')[1],
            }))
          : [],
      'detail-level':
        (querySettings && querySettings['detail-level']) || 'allFields',
    }
  },
})
