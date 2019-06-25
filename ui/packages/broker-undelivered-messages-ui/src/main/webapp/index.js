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
import React from 'react'
import { render } from 'react-dom'
import store from './store'
import {
  isAllSelected,
  getSelectedIds,
  getSelectedMessages,
  getIds,
} from './reducer'
import {
  addMessage,
  checkAllMessages,
  togglePolling,
  expandAllMessages,
  updateDeleted,
  removeMessages,
} from './actions'
import TableView from './table.view'

require('es6-promise').polyfill()
require('isomorphic-fetch')

const getMessagesURI =
  '../jolokia/exec/org.codice.ddf.broker.ui.UndeliveredMessages:service=UndeliveredMessages/getMessages/DLQ/DLQ/'

const retrieveData = (data, url) =>
  window
    .fetch(url, {
      credentials: 'same-origin',
      headers: {
        'X-Requested-With': 'XMLHttpRequest',
      },
    })
    .then(res => res.json())
    .then(json => {
      json.value
        .filter(message => !getIds(data).includes(message.messageID))
        .map(message => {
          store.dispatch(
            addMessage({
              timestamp: message.timestamp,
              id: message.messageID,
              expanded: false,
              message: {
                messageBody: message.text,
                origin: message.StringProperties._AMQ_ORIG_QUEUE,
                address: message.address,
              },
            })
          )
        })
    })

const operateOnData = (data, method) => {
  var ids =
    method === 'resendMessages'
      ? getSelectedIds(
          getSelectedMessages(data).filter(
            message => message.message.origin.length !== 0
          )
        )
      : getSelectedIds(data)
  window
    .fetch('../jolokia/', {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest',
      },
      body: JSON.stringify({
        type: 'EXEC',
        mbean:
          'org.codice.ddf.broker.ui.UndeliveredMessages:service=UndeliveredMessages',
        operation: method,
        arguments: ['DLQ', 'DLQ', ids],
      }),
    })
    .then(res => res.json())
    .then(json => {
      if (json.value) {
        store.dispatch(removeMessages(ids))
        if (method === 'deleteMessages') {
          store.dispatch(updateDeleted(json.value))
        }
      }
    })
    .catch()
}

const Header = ({ data }) => {
  const checked = isAllSelected(data)
  return (
    <div>
      <button onClick={() => retrieveData(data, getMessagesURI)}>
        RETRIEVE
      </button>
      <button onClick={() => operateOnData(data, 'resendMessages')}>
        RESEND
      </button>
      <button onClick={() => operateOnData(data, 'deleteMessages')}>
        DELETE
      </button>
      <button onClick={() => poll()}>
        {store.getState().polling.polling ? 'STOP POLLING' : 'START POLLING'}
      </button>
      <div className="selected">
        {getSelectedIds(data).length} of {data.length} selected.
        <span
          className="clickable"
          onClick={() => store.dispatch(checkAllMessages(true))}
        >
          Select All
        </span>
        <span
          className="clickable"
          onClick={() => store.dispatch(checkAllMessages(false))}
        >
          Deselect All
        </span>
        <span
          className="clickable"
          onClick={() => store.dispatch(expandAllMessages(true))}
        >
          Expand All
        </span>
        <span
          className="clickable"
          onClick={() => store.dispatch(expandAllMessages(false))}
        >
          Collapse All
        </span>
        <span>{store.getState().operationStatus} messages deleted</span>
      </div>
      <div className="container">
        <TableView data={data} checked={checked} />
      </div>
    </div>
  )
}

const renderView = () =>
  render(
    <Header data={store.getState().messages} />,
    document.getElementById('root')
  )

const poll = () => {
  if (store.getState().polling.polling) {
    clearInterval(store.getState().polling.id)
    store.dispatch(togglePolling())
  } else {
    store.dispatch(
      togglePolling(
        setInterval(() => {
          retrieveData(store.getState().messages, getMessagesURI)
        }, 5000)
      )
    )
  }
}

store.subscribe(renderView)
renderView()
