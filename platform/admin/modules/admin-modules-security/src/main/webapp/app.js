import React from 'react'
import { Provider } from 'react-redux'
import store from './store'

import Exception from './containers/exceptions'
import Ldap from './wizards/ldap'
import Sources from './wizards/sources'
import { SourcesHome } from './wizards/sourcesHome'
import Wcpm from './adminTools/webContextPolicyManager'

import { Router, Route, hashHistory, IndexRoute } from 'react-router'
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider'
import Flexbox from 'flexbox-react'

const fixed = {
  position: 'fixed',
  top: 0,
  left: 0,
  bottom: 0,
  right: 0
}

const App = ({ children }) => (
  <Flexbox flexDirection='column' height='100vh' style={fixed}>
    <Flexbox flex='1' style={{ overflowY: 'scroll', width: '100%' }}>
      {children}
    </Flexbox>
    <Exception />
  </Flexbox>
)

var DevTools

if (process.env.NODE_ENV === 'production') {
  DevTools = () => null
}

if (process.env.NODE_ENV !== 'production') {
  DevTools = require('./containers/dev-tools').default
}

export default () => (
  <MuiThemeProvider>
    <Provider store={store}>
      <div>
        <Router history={hashHistory}>
          <Route path='/' component={App}>
            <IndexRoute component={SourcesHome} />
            <Route path='/ldap' component={Ldap} />
            <Route path='/sources' component={Sources} />
            <Route path='/webContextPolicyManager' component={Wcpm} />
          </Route>
        </Router>
        <DevTools />
      </div>
    </Provider>
  </MuiThemeProvider>
)
