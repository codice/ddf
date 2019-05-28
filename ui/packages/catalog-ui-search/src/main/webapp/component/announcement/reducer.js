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
const _ = require('underscore')

module.exports = function(state, action) {
  if (state === undefined) {
    return []
  }

  switch (action.type) {
    case 'ADD_ANNOUNCEMENT':
      return state.concat(action.announcement)
    case 'START_REMOVE_ANNOUNCEMENT':
      return state.map(announcement => {
        if (announcement.id === action.id) {
          return _.extend({}, announcement, { removing: true })
        }
        return announcement
      })
    case 'REMOVE_ANNOUNCEMENT':
      return state.filter(announcement => announcement.id !== action.id)
    case 'DEDUPE_ANNOUNCEMENT':
      return
    default:
      return state
  }
}
