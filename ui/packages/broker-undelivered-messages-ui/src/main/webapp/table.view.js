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
import React from 'react'
import RowView from './row.view'
import store from './store'
import { checkAllMessages } from './actions'

export default ({ data, checked }) => (
  <table>
    <thead>
      <tr>
        <th>
          <input
            type="checkbox"
            checked={checked}
            onChange={() => store.dispatch(checkAllMessages(!checked))}
          />
        </th>
        <th>Time Stamp</th>
        <th>Message</th>
      </tr>
    </thead>
    <tbody>
      {data.map((n, i) => (
        <RowView {...n} key={i} />
      ))}
    </tbody>
  </table>
)
