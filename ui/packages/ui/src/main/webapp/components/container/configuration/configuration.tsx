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
import { hot } from 'react-hot-loader'
import ConfigurationElement, {
  ConfigurationType,
} from '../../presentation/configuration'
const wreqr = require('js/wreqr.js')
const configUrl =
  './jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0'
const $ = require('jquery')
const destroy = (id: string) => {
  var deleteUrl = [configUrl, 'delete', id].join('/')

  return $.ajax({
    type: 'GET',
    url: deleteUrl,
  }).always(() => {
    wreqr.vent.trigger('refreshConfigurations')
  })
}
const onBeforeEdit = () => {
  wreqr.vent.trigger('poller:stop')
}

type Props = ConfigurationType
type State = {}

class Configuration extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {}
  }
  render() {
    return (
      <ConfigurationElement
        destroy={() => {
          destroy(this.props.id)
        }}
        onBeforeEdit={onBeforeEdit}
        {...this.props}
      />
    )
  }
}

export default hot(module)(Configuration)
