const plugin = require('../../plugins/router')

// notfound route needs to come at the end otherwise no other routes will work
const routes = {
  ...plugin(
    require('!../../js/router/routes-loader!../../js/router/routes.js')
  ),
  ...require('!../../js/router/routes-loader!../../js/router/routes-dev.js'),
  ...require('!../../js/router/routes-loader!../../js/router/routes-notfound.js'),
}

export default routes
