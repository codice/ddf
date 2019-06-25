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
const properties = require('../../js/properties')
const api = require('./index')
const oldInit = properties.init

const mock = () => {
  properties.init = function() {
    const data = api('./internal/config')
    const uiConfig = api('./internal/platform/config/ui')
    // use this function to initialize variables that rely on others
    let props = this
    props = _.extend(props, data)
    props.ui = uiConfig
    this.handleEditing()
    this.handleFeedback()
    this.handleExperimental()
    this.handleUpload()
    this.handleListTemplates()
  }
  properties.init()
}

const unmock = () => {
  properties.init = oldInit
}

export { mock, unmock }
