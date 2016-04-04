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

// total number of log entries to keep in memory that a
// user can filter through
const MAX_LOG_ENTRIES = 5000
const INITIAL_DISPLAY_SIZE = 25
const GROW_SIZE = 50

export default (state, action) => {
  if (state === undefined) {
    return {
      logs: [],
      filter: { level: 'ALL' },
      displaySize: INITIAL_DISPLAY_SIZE
    }
  }

  switch (action.type) {
    case 'FILTER_LOGS':
      return { ...state,
        displaySize: INITIAL_DISPLAY_SIZE,
        filter: { ...state.filter, ...action.filter }
      }

    case 'APPEND_LOGS':
      return { ...state,
        logs: action.entries.concat(state.logs).slice(0, MAX_LOG_ENTRIES)
      }

    case 'GROW_DISPLAY_SIZE':
      return { ...state,
        displaySize: state.displaySize + GROW_SIZE
      }

    default:
      return state
  }
}
