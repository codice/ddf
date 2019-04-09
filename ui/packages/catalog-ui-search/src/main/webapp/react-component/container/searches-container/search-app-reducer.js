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

import {
  ADD_SEARCHES,
  GET_SEARCHES,
  DELETE_SEARCH,
  SET_PAGINATION_COMPLETE,
  PAGE_SIZE,
} from './actions'

const initialState = {
  searches: [],
  complete: false,
}

const isPaginationComplete = (oldSearches, newSearches) => {
  if (oldSearches === newSearches) {
    return true
  }

  if (newSearches.length < PAGE_SIZE) {
    return true
  }

  return false
}

const searchAppReducer = (state = initialState, action) => {
  switch (action.type) {
    case DELETE_SEARCH:
      return {
        ...state,
        searches: state.searches.filter(search => search.id !== action.payload),
      }
    case ADD_SEARCHES:
      return {
        ...state,
        searches: state.searches.concat(action.payload),
        complete: isPaginationComplete(state.searches, action.payload),
      }
    case SET_PAGINATION_COMPLETE:
      return { ...state, complete: action.payload }
    default:
      return state
  }
}

export default searchAppReducer
