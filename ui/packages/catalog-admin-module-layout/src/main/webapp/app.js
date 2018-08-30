import React from 'react'
import { Provider } from 'react-redux'
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider'
import { Router, hashHistory } from 'react-router'

import store from './store'
import DefaultLayout from './default-layout'

import getMuiTheme from 'material-ui/styles/getMuiTheme'

import admin from 'themes/admin'
import catalog from 'themes/catalog'

import Backdrop from './Backdrop'

const themes = { admin, catalog }

const App = ({ children, location }) => (
  <Provider store={store}>
    <MuiThemeProvider
      muiTheme={getMuiTheme(themes[location.query.theme || 'admin'])}
    >
      <Backdrop>
        <div style={{ maxWidth: 960, margin: '0 auto', padding: '0 20px' }}>
          {children}
        </div>
      </Backdrop>
    </MuiThemeProvider>
  </Provider>
)

const routes = {
  path: '/',
  component: App,
  indexRoute: { component: DefaultLayout },
}

export default () => <Router history={hashHistory} routes={routes} />
