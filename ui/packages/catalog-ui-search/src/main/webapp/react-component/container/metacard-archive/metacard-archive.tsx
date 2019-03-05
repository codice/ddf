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

class MetacardQuality extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)

    let collection
    if (props.selectionInterface) {
      collection = props.selectionInterface.getSelectedResults()
    } else {
      collection = store.getSelectedResults()
    }

    const isDeleted = collection.some((result: any) => {
      result.isDeleted()
    })

    this.state = {
      collection,
      isDeleted,
      loading: false,
    }
  }

  handleArchive = () => {
    const body = JSON.stringify(
      this.state.collection.map((result: any) => {
        return result.get('metacard').get('id')
      })
    )

    const self = this
    this.props.listenTo(
      ConfirmationView.generateConfirmation({
        prompt:
          'Are you sure you want to archive?  Doing so will remove the item(s) from future search results.',
        no: 'Cancel',
        yes: 'Archive',
      }),
      'change:choice',
      async function(confirmation: any) {
        if (confirmation.get('choice')) {
          self.setState({ loading: true })

          const res = await fetch('./internal/metacards', {
            method: 'DELETE',
            body,
          })
          if (!res.ok) {
            announcement.announce({
              title: 'Unable to archive the selected item(s).',
              message: 'Something went wrong.',
              type: 'error',
            })
          }

          self.state.collection.forEach(function(result) {
            result
              .get('metacard')
              .get('properties')
              .set('metacard-tags', ['deleted'])
            result.trigger('refreshdata')
          })
          self.refreshResults()

          setTimeout(() => {
            self.setState({ loading: false })
          }, 2000)
        }
      }.bind(this)
    )
  }

  handleRestore = () => {
    const self = this
    this.props.listenTo(
      ConfirmationView.generateConfirmation({
        prompt:
          'Are you sure you want to restore?  Doing so will include the item(s) in future search results.',
        no: 'Cancel',
        yes: 'Restore',
      }),
      'change:choice',
      function(confirmation: any) {
        if (confirmation.get('choice')) {
          self.setState({ loading: true })

          const promises = self.state.collection.map((result: any) => {
            fetch(
              `./internal/history/revert/${result
                .get('metacard')
                .get('properties')
                .get('metacard.deleted.id')}/${result
                .get('metacard')
                .get('properties')
                .get('metacard.deleted.version')}`
            )
          })

          Promise.all(promises).then(() => {
            self.state.collection.map((result: any) => {
              ResultUtils.refreshResult(result)
            })

            setTimeout(() => {
              self.setState({ loading: false })
            }, 2000)
          })
        }
      }.bind(this)
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

export default hot(module)(withListenTo(MetacardQuality))
