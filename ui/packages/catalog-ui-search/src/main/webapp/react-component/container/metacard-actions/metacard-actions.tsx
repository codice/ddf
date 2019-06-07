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

import { hot } from 'react-hot-loader'
import * as React from 'react'
const _ = require('underscore')
const store = require('../../../js/store.js')
import MetacardActionsPresentation from '../../presentation/metacard-actions'

type Props = {
  selectionInterface: any
}

const MetacardActions = (props: Props) => {
  const selectionInterface = props.selectionInterface || store
  const model = selectionInterface.getSelectedResults().first()

  const exportActions = _.sortBy(
    model.getExportActions().map((action: any) => ({
      url: action.get('url'),
      title: action.getExportType(),
    })),
    (action: any) => action.title.toLowerCase()
  )
  const otherActions = _.sortBy(
    model.getOtherActions().map((action: any) => ({
      url: action.get('url'),
      title: action.get('title'),
    })),
    (action: any) => action.title.toLowerCase()
  )

  return (
    <MetacardActionsPresentation
      model={model}
      exportActions={exportActions}
      otherActions={otherActions}
    />
  )
}

export default hot(module)(MetacardActions)
