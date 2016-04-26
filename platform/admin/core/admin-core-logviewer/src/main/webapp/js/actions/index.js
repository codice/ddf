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

// for applying a filter to the logs
export function filter (filter) {
  return {
    type: 'FILTER_LOGS',
    filter: filter
  }
}

// for appending new logs to the stored log list
// note: user will not lose context when scrolling
export function append (entries) {
  return {
    type: 'APPEND_LOGS',
    entries: entries
  }
}

// used for expanding the max amount of stored logs displayed on screen
export function grow () {
  return {
    type: 'GROW_DISPLAY_SIZE'
  }
}

export function expandEntry (hash) {
  return {
    type: 'CHANGE_EXPANDED_ENTRY',
    hash: hash
  }
}
