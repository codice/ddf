const plugin = require('../../plugins/router')

// notfound route needs to come at the end otherwise no other routes will work
const routes = {
  ...plugin(require('!./definitions/loader!./definitions/base.js')),
  ...require('!./definitions/loader!./definitions/dev.js'),
  ...require('!./definitions/loader!./definitions/not-found.js'),
}

export default routes
