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
import withListenTo, { WithBackboneProps } from '../backbone-container'
import * as _ from 'lodash'
import * as ListModel from '../../../js/model/List'
import * as store from '../../../js/store'

import View from '../../presentation/result-add'

const LIST_CQL_KEY = 'list.cql'
const LIST_BOOKMARKS_KEY = 'list.bookmarks'
const LIST_ICON_KEY = 'list.icon'

type Props = {
  model: [
    Backbone.Model & {
      matchesCql: (item: any) => any
    }
  ]
} & WithBackboneProps

type State = {
  resultItems?: [any]
}

type List = {
  toJSON: () => [any]
}

const listItemToObject = (
  list: { [key: string]: any },
  model: Props['model']
): { [key: string]: {} } => {
  const matchesFilter =
    (list[LIST_CQL_KEY] !== '' &&
      model.every(item => item.matchesCql(list[LIST_CQL_KEY]))) ||
    true
  const alreadyContains =
    _.intersection(
      list[LIST_BOOKMARKS_KEY],
      model.map((item: any) => item.get('metacard').id)
    ).length === model.length

  const icon = ListModel.getIconMapping()[list[LIST_ICON_KEY]]

  return {
    ...list,
    matchesFilter,
    alreadyContains,
    icon,
  }
}

const formatList = (model: Props['model']) => {
  const json = getLists().toJSON()

  return json.map((list: List) => listItemToObject(list, model))
}

const getLists = () => store.getCurrentWorkspace().get('lists')

const addToList = (id: string, metacardIds: [string]) =>
  getLists()
    .get(id)
    .addBookmarks(metacardIds)

const removeFromList = (id: string, metacardIds: [string]) =>
  getLists()
    .get(id)
    .removeBookmarks(metacardIds)

export default hot(module)(
  withListenTo(
    class extends React.Component<Props, State> {
      constructor(props: Props) {
        super(props)

        this.state = {
          resultItems: formatList(this.props.model),
        }
      }

      componentDidMount = () => {
        const updateResultItemsState = () =>
          this.setState({ resultItems: formatList(this.props.model) })

        this.props.listenTo(
          getLists(),
          'add remove update change',
          updateResultItemsState
        )
      }

      updateBookmark = (id: any) => {
        const existingBookmarks = getLists()
          .get(id)
          .get(LIST_BOOKMARKS_KEY)
        const metacardIds = this.props.model.map(
          (item: any) => item.get('metacard').id
        )

        const alreadyBookmarked =
          _.intersection(existingBookmarks, metacardIds).length > 0

        alreadyBookmarked
          ? removeFromList(id, metacardIds as [any])
          : addToList(id, metacardIds as [any])
      }

      render = () => (
        <View
          model={this.props.model}
          items={this.state.resultItems}
          bookmarkHandler={this.updateBookmark}
        />
      )
    }
  )
)
