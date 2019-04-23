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
/**
 * ApplicationSetup needs to happen first.
 * This ensures styles are applied correctly,
 * because some of our styles have the same specificity as vendor
 * styles.
 */
;(function verifyFirstImport() {
  if (document.querySelector('[data-styled-components]')) {
    const firstImportErrorMessage = `The entry import has to be the first (top) import for your application, otherwise styles won't be applied properly.
    If you're seeing this, it probably means you need to move your import of the Entry file to the top of whatever file it's in.
    `
    alert(firstImportErrorMessage)
    throw Error(firstImportErrorMessage)
  }
})()
require('../js/ApplicationSetup')
import ExtensionPoints, { ExtensionPointsType } from '../extension-points'

export type EntryParameters = {
  routes?: ExtensionPointsType['routes']
  navigator?: ExtensionPointsType['navigator']
  filterActions?: ExtensionPointsType['filterActions']
}

const entry = (extensionPoints: EntryParameters = {}) => {
  const { routes, navigator, filterActions } = extensionPoints
  if (routes) {
    ExtensionPoints.routes = routes
  }
  if (navigator) {
    ExtensionPoints.navigator = navigator
  }
  if (filterActions) {
    ExtensionPoints.filterActions = filterActions
  }
  require('../js/ApplicationStart')
}

export default entry
