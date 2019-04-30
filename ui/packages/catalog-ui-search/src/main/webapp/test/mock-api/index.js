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
