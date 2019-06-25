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
