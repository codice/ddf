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
import plugin from '../../plugins/metacard-interactions'
import CreateLocationSearch from '../../react-component/container/metacard-interactions/location-interaction'
import ExpandMetacard from '../../react-component/container/metacard-interactions/expand-interaction'
import BlacklistToggle from '../../react-component/container/metacard-interactions/hide-interaction'
import DownloadProduct from '../../react-component/container/metacard-interactions/download-interaction'
import ExportActions from '../../react-component/container/metacard-interactions/export-interaction'
import AddToList from '../../react-component/container/metacard-interactions/add-to-list-interaction'
import { Divider } from '../../react-component/presentation/metacard-interactions/metacard-interactions'

const DefaultItems = [AddToList, BlacklistToggle, ExpandMetacard, Divider,
    DownloadProduct, CreateLocationSearch, ExportActions]

export default plugin(DefaultItems) as any[]
