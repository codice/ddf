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
import withListenTo, { WithBackboneProps } from '../backbone-container'
import { hot } from 'react-hot-loader'
const user = require('../../../component/singletons/user-instance')

import CreateLocationSearch from './location-interaction'
import ExpandMetacard from './expand-interaction'
import BlacklistToggle, { isBlacklisted } from './hide-interaction'
import DownloadProduct from './download-interaction'
import ExportActions from './export-interaction'
import AddToList from './add-to-list-interaction'
import { Divider } from '../../presentation/metacard-interactions/metacard-interactions'

const plugin = require('plugins/metacard-interactions')

export type Props = {
  model: {} | any
  onClose: () => void
} & WithBackboneProps

export type Result = {
  get: (key: any) => any
  isWorkspace: () => boolean
  isResource: () => boolean
  isRevision: () => boolean
  isDeleted: () => boolean
  isRemote: () => boolean
}

export type Model = {
  map: (
    result: Result | any
  ) =>
    | {
        id?: any
        title?: any
      }
    | {}
  toJSON: () => any
  first: () => any
  forEach: (result: Result | any) => void
  find: (result: Result | any) => boolean
} & Array<any>

type State = {
  blacklisted: Boolean
  model: any
}

const interactions = plugin([
  AddToList,
  BlacklistToggle,
  ExpandMetacard,
  Divider,
  DownloadProduct,
  CreateLocationSearch,
  ExportActions,
])

const mapPropsToState = (props: Props) => {
  return {
    model: props.model,
    blacklisted: isBlacklisted(props.model),
  }
}

class MetacardInteractions extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = mapPropsToState(props)
  }
  componentDidMount = () => {
    const setState = (model: Model) => this.setState({ model: model })

    this.props.listenTo(
      this.props.model,
      'change:metacard>properties',
      setState
    )

    this.props.listenTo(
      user
        .get('user')
        .get('preferences')
        .get('resultBlacklist'),
      'add remove update reset',
      () => this.setState({ blacklisted: isBlacklisted(this.props.model) })
    )
  }

  render = () => {
    return (
      <>
        {interactions.map((Component: any, i: number) => {
          const componentName = Component.toString()
          const key = componentName + '-' + i
          return (
            <Component
              key={key}
              {...this.props}
              blacklisted={this.state.blacklisted}
            />
          )
        })}
      </>
    )
  }
}

const Component = withListenTo(MetacardInteractions)

export default hot(module)(Component)
