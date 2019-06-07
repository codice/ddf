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
import ExtensionPoints from '../../extension-points'

const Providers = ExtensionPoints.providers

Marionette.ItemView.prototype.attachElContent = function(rendering) {
  this.triggerMethod('before:react:attach', rendering)
  render(
    <Providers>
      {React.isValidElement(rendering) ? rendering : Parser(rendering)}
    </Providers>,
    this.el
  )
  this.triggerMethod('after:react:attach', rendering)
  return this
}
