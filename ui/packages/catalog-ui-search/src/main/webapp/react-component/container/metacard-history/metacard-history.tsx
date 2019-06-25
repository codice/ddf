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
import fetch from '../../utils/fetch'
const store = require('../../../js/store.js')
const Common = require('../../../js/Common.js')
const ResultUtils = require('../../../js/ResultUtils.js')
const moment = require('moment')
const announcement = require('component/announcement')
import MetacardHistoryPresentation from '../../presentation/metacard-history'

type Props = {
  selectionInterface: any
}

type State = {
  history: any
  selectedVersion: any
  loading: boolean
}

class MetacardHistory extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)

    const selectionInterface = props.selectionInterface || store
    this.model = selectionInterface.getSelectedResults().first()

    this.state = {
      history: [],
      selectedVersion: undefined,
      loading: true,
    }
  }
  model: Backbone.Model
  componentDidMount() {
    this.loadData()
  }

  loadData() {
    setTimeout(async () => {
      const res = await fetch(
        `./internal/history/${this.model.get('metacard').get('id')}`
      )

      if (!res.ok || res.status === 204) {
        this.setState({ history: [], loading: false })
        return
      }

      const history = await res.json()
      history.sort((historyItem1: any, historyItem2: any) => {
        return (
          moment.unix(historyItem2.versioned.seconds) -
          moment.unix(historyItem1.versioned.seconds)
        )
      })
      history.forEach((historyItem: any, index: any) => {
        historyItem.niceDate = Common.getMomentDate(
          moment.unix(historyItem.versioned.seconds).valueOf()
        )
        historyItem.versionNumber = history.length - index
      })

      this.setState({ history, loading: false })
    }, 1000)
  }

  clickWorkspace = (event: any) => {
    const selectedVersion = event.currentTarget.getAttribute('data-id')
    this.setState({ selectedVersion })
  }

  revertToSelectedVersion = async () => {
    this.setState({ loading: true })

    const res = await fetch(
      `./internal/history/revert/${this.model.get('metacard').get('id')}/${
        this.state.selectedVersion
      }`
    )

    if (!res.ok) {
      this.setState({ loading: false })
      announcement.announce({
        title: 'Unable to revert to the selected version',
        message: 'Something went wrong.',
        type: 'error',
      })
      return
    }

    this.model
      .get('metacard')
      .get('properties')
      .set('metacard-tags', ['revision'])
    ResultUtils.refreshResult(this.model)

    setTimeout(() => {
      //let solr flush
      this.model.trigger('refreshdata')
      if (
        this.model
          .get('metacard')
          .get('properties')
          .get('metacard-tags')
          .indexOf('revision') >= 0
      ) {
        announcement.announce({
          title: 'Waiting on Reverted Data',
          message: [
            "It's taking an unusually long time for the reverted data to come back.",
            'The item will be put in a revision-like state (read-only) until data returns.',
          ],
          type: 'warn',
        })
      }
      this.loadData()
    }, 2000)
  }

  render() {
    const { history, selectedVersion, loading } = this.state
    return (
      <MetacardHistoryPresentation
        clickWorkspace={this.clickWorkspace}
        revertToSelectedVersion={this.revertToSelectedVersion}
        history={history}
        selectedVersion={selectedVersion}
        loading={loading}
      />
    )
  }
}

export default hot(module)(MetacardHistory)
