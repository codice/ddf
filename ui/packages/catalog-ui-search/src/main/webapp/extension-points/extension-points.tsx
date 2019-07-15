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
import * as React from 'react'
import routes from './routes'
import navigator, { Props } from './navigator'
import filterActions from './filter-actions'
import { SFC } from '../react-component/hoc/utils'
import { providers, Props as ProviderProps } from './providers'
import visualizations from './visualizations'
import queryForms from './query-forms'
import navigationRight from './navigation-right'

export type ExtensionPointsType = {
  routes: {}
  navigator: SFC<Props>
  filterActions: React.ReactNode
  providers: SFC<ProviderProps>
  visualizations: any[]
  queryForms: any[]
  navigationRight: any[]
}

const ExtensionPoints: ExtensionPointsType = {
  routes,
  navigator,
  filterActions,
  providers,
  visualizations,
  queryForms,
  navigationRight,
}

export default ExtensionPoints
