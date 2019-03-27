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
/*global require*/
const Marionette = require('marionette')
const template = require('./notfound.hbs')
const CustomElements = require('../../js/CustomElements.js')
const router = require('../router/router.js')
const NavigatorView = require('../navigator/navigator.view.js')
import * as React from 'react'
import ExtensionPoints from '../../extension-points'

module.exports = Marionette.LayoutView.extend({
  template() {
    const Navigator = ExtensionPoints.navigator
    return (
      <React.Fragment>
        <div className="content">
          <div className="message is-large-font is-centered">
            We can't find the page you requested.
          </div>
          <div className="message is-medium-font is-centered">
            Please check the url or navigate to another page.
          </div>
          <div className="navigator">
            <Navigator />
          </div>
        </div>
      </React.Fragment>
    )
  },
  tagName: CustomElements.register('notfound'),
  serializeData: function() {
    return {
      route: window.location.hash.substring(1),
    }
  },
})
