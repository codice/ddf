const _ = require('underscore')
const webgl = require('./webglcheck.js')
const twoD = require('./2dmapcheck.js')
const qs = require('querystring')
const user = require('../component/singletons/user-instance.js')

function getActiveVisualization() {
  return user
    .get('user')
    .get('preferences')
    .get('visualization')
}

const MapTypeEnum = {
  THREED: '3d',
  TWOD: '2d',
  NONE: 'none',
}

const url = function() {
  // replace removes leading ? in query string
  const query = window.location.search.replace(/^\?/, '')
  return qs.parse(query)
}

module.exports = {
  type: (function() {
    const param = url().map
    if (!_.isUndefined(param)) {
      if (_.contains(_.values(MapTypeEnum), param)) {
        return param
      }
    }

    if (webgl.isAvailable()) {
      return MapTypeEnum.THREED
    } else if (twoD.isAvailable()) {
      return MapTypeEnum.TWOD
    } else {
      return MapTypeEnum.NONE
    }
  })(),

  is3d: function() {
    return getActiveVisualization() === '3dmap'
  },
  is2d: function() {
    return getActiveVisualization() === '2dmap'
  },
  isNone: function() {
    return this.type === MapTypeEnum.NONE
  },
  isMap: function() {
    return this.is3d() || this.is2d()
  },
}
