/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
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
  require('../js/ApplicationSetup')
}

export default entry
