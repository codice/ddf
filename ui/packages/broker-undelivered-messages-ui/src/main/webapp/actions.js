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
export const checkMessage = id => ({ type: 'CHECK_MESSAGE', id })
export const checkAllMessages = checked => ({
  type: 'CHECK_ALL_MESSAGES',
  checked,
})
export const addMessage = message => ({ type: 'ADD_MESSAGE', message })
export const removeMessages = ids => ({ type: 'REMOVE_MESSAGES', ids })
export const togglePolling = id => ({ type: 'TOGGLE_POLLING', id })
export const expandMessage = id => ({ type: 'EXPAND_MESSAGE', id })
export const expandAllMessages = expanded => ({
  type: 'EXPAND_ALL_MESSAGES',
  expanded,
})
export const updateDeleted = num => ({ type: 'UPDATE_DELETED', num })
