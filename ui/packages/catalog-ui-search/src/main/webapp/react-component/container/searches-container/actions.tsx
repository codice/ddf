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

import { Search } from '.'
import fetch from '../../utils/fetch'

export const ADD_SEARCH = 'searchApp/ADD_SEARCH'
export const addSearch = (search: Search) => {
  return {
    type: ADD_SEARCH,
    payload: search,
  }
}

export const GET_SEARCHES = 'searchApp/GET_SEARCHES'
const getSearches = (searches: Search[]) => {
  return {
    type: GET_SEARCHES,
    payload: searches,
  }
}
export const getSearchesRequest = () => async (dispatch: any) => {
  try {
    const response = await fetch('./internal/queries')
    if (!response.ok) {
      throw new Error(response.statusText)
    }
    const json = await response.json()
    return dispatch(getSearches(json))
  } catch (err) {
    console.log(err)
  }
}

export const DELETE_SEARCH = 'searchApp/DELETE_SEARCH'
const deleteSearch = (id: string) => {
  return {
    type: DELETE_SEARCH,
    payload: id,
  }
}
export const deleteSearchRequest = (id: string) => async (dispatch: any) => {
  try {
    const response = await fetch(`./internal/queries/${id}`, {
      method: 'delete',
    })
    if (!response.ok) {
      throw new Error(response.statusText)
    }

    return dispatch(deleteSearch(id))
  } catch (err) {
    console.log(err)
  }
}
