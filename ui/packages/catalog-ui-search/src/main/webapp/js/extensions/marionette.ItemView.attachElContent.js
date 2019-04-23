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
const Marionette = require('marionette')
import { render } from 'react-dom'
import React from 'react'
const Parser = require('html-react-parser')
const properties = require('properties')
import ThemeContainer from '../../react-component/container/theme-container'
import { IntlProvider } from 'react-intl'

Marionette.ItemView.prototype.attachElContent = function(rendering) {
  this.triggerMethod('before:react:attach', rendering)
  render(
    <ThemeContainer>
      <React.Fragment>
        <IntlProvider locale={navigator.language} messages={properties.i18n}>
          <React.Fragment>
            {React.isValidElement(rendering) ? rendering : Parser(rendering)}
          </React.Fragment>
        </IntlProvider>
      </React.Fragment>
    </ThemeContainer>,
    this.el
  )
  this.triggerMethod('after:react:attach', rendering)
  return this
}
