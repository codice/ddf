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
import { combineReducers } from 'redux'

const messages = (
  messages = [],
  { type, checked = false, expanded = false, id, message, ids }
) => {
  switch (type) {
    case 'ADD_MESSAGE':
      return [...messages, { checked, ...message }]
    case 'REMOVE_MESSAGES':
      return messages.filter(message => !ids.includes(message.id))
    case 'CHECK_MESSAGE':
      return messages.map(message => {
        if (message.id !== id) return message
        return { ...message, checked: !message.checked }
      })
    case 'CHECK_ALL_MESSAGES':
      return messages.map(message => ({ ...message, checked }))
    case 'CLEAR_MESSAGES':
      return []
    case 'EXPAND_MESSAGE':
      return messages.map(message => {
        if (message.id !== id) return message
        return { ...message, expanded: !message.expanded }
      })
    case 'EXPAND_ALL_MESSAGES':
      return messages.map(message => ({ ...message, expanded }))
    default:
      return messages
  }
}

const polling = (state = { polling: false }, { type, id = 0 }) => {
  switch (type) {
    case 'TOGGLE_POLLING':
      return { polling: !state.polling, id }
    default:
      return state
  }
}

const operationStatus = (state = 0, { type, num }) => {
  switch (type) {
    case 'UPDATE_DELETED':
      return num
    default:
      return state
  }
}

export const isAllSelected = messages => {
  if (messages.length === 0) return false
  return messages.reduce((prev, message) => prev && message.checked, true)
}

export const getIds = messages => messages.map(message => message.id)
export const getSelectedMessages = messages =>
  messages.filter(message => message.checked)
export const getSelectedIds = messages => getIds(getSelectedMessages(messages))

export default combineReducers({ messages, polling, operationStatus })
