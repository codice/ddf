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

export const ADD_SEARCHES = 'searchApp/ADD_SEARCH'
export const PAGE_SIZE = 5
const getSearches = (searches: Search[]) => {
  return {
    type: ADD_SEARCHES,
    payload: searches,
  }
}
export const getSearchesRequest = (start: number) => async (dispatch: any) => {
  try {
    const response = await fetch(
      `./internal/queries?start=${start}&count=${PAGE_SIZE}`
    )
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

export const SET_PAGINATION_COMPLETE = 'searchApp/SET_PAGINATION_COMPLETE'
export const setPaginationComplete = (complete: boolean) => {
  return {
    type: SET_PAGINATION_COMPLETE,
    payload: complete,
  }
}
