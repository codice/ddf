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

import { hot } from 'react-hot-loader'
import * as React from 'react'
const _ = require('underscore')
const store = require('../../../js/store.js')
import MetacardActionsPresentation from '../../presentation/metacard-actions'

type Props = {
  selectionInterface: any
}

type State = {
  model: Backbone.Model
  exportActions: any
  otherActions: any
}

class MetacardActions extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)

    let model
    if (props.selectionInterface) {
      model = props.selectionInterface.getSelectedResults().first()
    } else {
      model = store.getSelectedResults().first()
    }

    this.state = {
      model: model,
      exportActions: _.sortBy(
        model.getExportActions().map((action: any) => ({
          url: action.get('url'),
          title: action.getExportType(),
        })),
        (action: any) => action.title.toLowerCase()
      ),
      otherActions: _.sortBy(
        model.getOtherActions().map((action: any) => ({
          url: action.get('url'),
          title: action.get('title'),
        })),
        (action: any) => action.title.toLowerCase()
      ),
    }
  }

  render() {
    return <MetacardActionsPresentation {...this.state} />
  }
}

export default hot(module)(MetacardActions)
