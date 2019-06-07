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
import fetch from '../../utils/fetch'
const announcement = require('component/announcement')
const store = require('../../../js/store.js')
const ResultUtils = require('../../../js/ResultUtils.js')
const ConfirmationView = require('../../../component/confirmation/confirmation.view.js')
import MetacardArchivePresentation from '../../presentation/metacard-archive'

type Props = {
  selectionInterface: any
} & WithBackboneProps

type State = {
  collection: Backbone.Collection<Backbone.Model>
  isDeleted: boolean
  loading: boolean
}

class MetacardArchive extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)

    const selectionInterface = props.selectionInterface || store
    const collection = selectionInterface.getSelectedResults()

    const isDeleted = collection.some((result: any) => {
      return result.isDeleted()
    })

    this.state = {
      collection,
      isDeleted,
      loading: false,
    }
  }

  onArchiveConfirm = async (confirmation: any) => {
    if (confirmation.get('choice')) {
      const body = JSON.stringify(
        this.state.collection.map((result: any) => {
          return result.get('metacard').get('id')
        })
      )
      this.setState({ loading: true })

      const res = await fetch('./internal/metacards', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body,
      })
      if (!res.ok) {
        this.setState({ loading: false })
        announcement.announce({
          title: 'Unable to archive the selected item(s).',
          message: 'Something went wrong.',
          type: 'error',
        })
        return
      }

      setTimeout(() => {
        this.setState({ isDeleted: true, loading: false })
        this.state.collection.forEach(function(result) {
          result
            .get('metacard')
            .get('properties')
            .set('metacard-tags', ['deleted'])
          result.trigger('refreshdata')
        })
        this.refreshResults()
      }, 2000)
    }
  }

  handleArchive = () => {
    this.props.listenTo(
      ConfirmationView.generateConfirmation({
        prompt:
          'Are you sure you want to archive?  Doing so will remove the item(s) from future search results.',
        no: 'Cancel',
        yes: 'Archive',
      }),
      'change:choice',
      this.onArchiveConfirm
    )
  }

  onRestoreConfirm = (confirmation: any) => {
    if (confirmation.get('choice')) {
      this.setState({ loading: true })

      const promises = this.state.collection.map((result: any) => {
        const metacardDeletedId = result
          .get('metacard')
          .get('properties')
          .get('metacard.deleted.id')
        const metacardDeletedVersion = result
          .get('metacard')
          .get('properties')
          .get('metacard.deleted.version')

        return fetch(
          `./internal/history/revert/${metacardDeletedId}/${metacardDeletedVersion}`
        )
      })

      Promise.all(promises).then((responses: any) => {
        const isResponseOk = responses.every((resp: any) => {
          return resp.ok
        })
        if (!isResponseOk) {
          this.setState({ loading: false })
          announcement.announce({
            title: 'Unable to restore the selected item(s).',
            message: 'Something went wrong.',
            type: 'error',
          })
        }

        this.state.collection.map((result: any) => {
          ResultUtils.refreshResult(result)
        })

        setTimeout(() => {
          this.setState({ isDeleted: false, loading: false })
        }, 2000)
      })
    }
  }

  handleRestore = () => {
    this.props.listenTo(
      ConfirmationView.generateConfirmation({
        prompt:
          'Are you sure you want to restore?  Doing so will include the item(s) in future search results.',
        no: 'Cancel',
        yes: 'Restore',
      }),
      'change:choice',
      this.onRestoreConfirm
    )
  }

  refreshResults = () => {
    this.state.collection.forEach((result: any) => {
      ResultUtils.refreshResult(result)
    })
  }

  render() {
    const { isDeleted, loading } = this.state
    return (
      <MetacardArchivePresentation
        handleArchive={this.handleArchive}
        handleRestore={this.handleRestore}
        isDeleted={isDeleted}
        loading={loading}
      />
    )
  }
}

export default hot(module)(withListenTo(MetacardArchive))
