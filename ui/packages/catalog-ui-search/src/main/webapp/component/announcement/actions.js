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

const uuid = require('uuid')
const _ = require('underscore')

const remove = (exports.remove = function(id, timeout) {
  return function(dispatch) {
    dispatch({
      type: 'START_REMOVE_ANNOUNCEMENT',
      id,
    })

    setTimeout(() => {
      dispatch({
        type: 'REMOVE_ANNOUNCEMENT',
        id,
      })
    }, timeout || 250)
  }
})

exports.announce = function(announcement, timeout) {
  const id = uuid.v4()

  return function(dispatch, getState) {
    getState()
      .filter(
        a =>
          a.title === announcement.title &&
          a.message === announcement.message &&
          a.type === announcement.type
      )
      .map(a => a.id)
      .forEach(id => {
        dispatch(remove(id))
      })

    dispatch({
      type: 'ADD_ANNOUNCEMENT',
      announcement: _.extend({ id }, announcement),
    })

    if (announcement.type !== 'error') {
      setTimeout(() => {
        dispatch(remove(id, timeout))
      }, timeout || 5000)
    }
  }
}
