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
import routes from './routes'
import navigator, { Props } from './navigator'
import { SFC } from '../react-component/hoc/utils'
import { providers, Props as ProviderProps } from './providers'
import visualizations from './visualizations'
import queryForms from './query-forms'
import navigationRight from './navigation-right'
import metacardInteractions from './metacard-interactions'
import searchInteractions, {
  SearchInteractionProps,
} from './search-interactions'
import { tableExport, Props as TableExportProps } from './table-export'
import multiSelectActions from './multi-select-actions'

export type ExtensionPointsType = {
  routes: {}
  navigator: SFC<Props>
  providers: SFC<ProviderProps>
  visualizations: any[]
  queryForms: any[]
  navigationRight: any[]
  metacardInteractions: any[]
  searchInteractions: SFC<SearchInteractionProps>
  tableExport: SFC<TableExportProps>
  multiSelectActions: any[]
}

const ExtensionPoints: ExtensionPointsType = {
  routes,
  navigator,
  providers,
  visualizations,
  queryForms,
  navigationRight,
  metacardInteractions,
  searchInteractions,
  tableExport,
  multiSelectActions,
}

export default ExtensionPoints
