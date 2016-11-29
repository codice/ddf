import React from 'react'
import { Provider } from 'react-redux'
import store from './store'

import Stage from './containers/stage'
import StageList from './containers/stage-list'
import Exception from './containers/exceptions'

import { Router, Route, hashHistory } from 'react-router'
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider'
import AppBar from 'material-ui/AppBar'
import Flexbox from 'flexbox-react'

const fixed = {
  position: 'fixed',
  top: 0,
  left: 0,
  bottom: 0,
  right: 0
}

const App = (props) => (
  <Flexbox flexDirection='column' height='100vh' style={fixed}>
    <AppBar title='Security UI' iconClassNameRight='muidocs-icon-navigation-expand-more' />

    <Flexbox flex='1' style={{ overflowY: 'scroll' }}>
      <Flexbox>
        <StageList />
      </Flexbox>
      <Flexbox>
        {props.children}
      </Flexbox>
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
            <Route path='/stage/:stageId' component={Stage} />
          </Route>
        </Router>
        <DevTools />
      </div>
    </Provider>
  </MuiThemeProvider>
)
