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
const User = require('../../js/model/User')

const mockDataMap = {
  './internal/metacardtype': require('./metacardtype.json'),
  './internal/config': require('./config.json'),
  './internal/platform/config/ui': require('./metacardtype.json'),
  './internal/enumerations/attribute/datatype': require('./datatype.json'),
  './internal/user': User.Model.prototype.defaults(),
  './internal/localcatalogid': 'ddf.distribution',
  './internal/forms/result': [],
  './internal/workspaces': [],
  './internal/catalog/sources': require('./sources.json'),
}

const mockDataGlobs = {
  './internal/enumerations': require('./enumerations.json'),
}

module.exports = url => {
  let data = mockDataMap[url]
  if (data === undefined) {
    Object.keys(mockDataGlobs).forEach(glob => {
      if (url.startsWith(glob)) {
        data = mockDataGlobs[glob]
      }
    })
  }
  if (data === undefined) {
    throw new Error(`Unknown url '${url}' for mock api.`)
  }
  return data
}
