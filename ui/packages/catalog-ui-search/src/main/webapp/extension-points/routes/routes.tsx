const plugin = require('../../plugins/router')
import Base from './definitions/base'
import Dev from './definitions/dev'
import NotFound from './definitions/not-found'

// notfound route needs to come at the end otherwise no other routes will work
const routes = {
  ...plugin(Base),
  ...Dev,
  ...NotFound,
}

export default routes
