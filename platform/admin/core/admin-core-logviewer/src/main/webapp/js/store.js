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

import * as redux from 'redux'
import backend from './backend'
import * as actions from './actions'
import uniq from './uniq'
import batch from './batch'
import reducer from './reducers'

var store = redux.createStore(reducer)

// added to store.js to ensure that it should only be run once
// pipe order:
// backend (fetch logs) -> uniq (filter out duplicates) -> dispatch action.append (append new logs)
backend()
  .pipe(uniq())
  .pipe(batch())
  .on('data', (data) => {
    store.dispatch(actions.append(data))
  })

export default store
