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
import moment from 'moment'

import levels from '../../levels'

const format = (time) => {
  return moment(time).format('D MMM YYYY, HH:mm:ss')
}

// log entry to display
export default ({ entry, marks }) => {
  // check if marks exist for filter highlighting
  const tryMark = (key) => {
    const mark = marks[key]
    const displayString = entry[key]
    if (mark) {
      const first = displayString.slice(0, mark.start)
      const second = displayString.slice(mark.start, mark.end)
      const third = displayString.slice(mark.end)
      return (
        <span>
          <span className='dim'>{first}</span>
          <mark>{second}</mark>
          <span className='dim'>{third}</span>
        </span>
      )
    } else {
      return (
        <span>{displayString}</span>
      )
    }
  }

  return (
    <tr className='rowAnimation'>
      <td className='row' style={{ background: levels(entry.level) }} width={175}>
        {format(entry.timestamp)}
      </td>
      <td className='row' style={{ background: levels(entry.level) }} width={75}>
        {entry.level}
      </td>
      <td className='row' style={{ background: levels(entry.level) }}>
        {tryMark('message')}
      </td>
      <td className='row' style={{ background: levels(entry.level) }} width={200}>
        {tryMark('bundleName')}
      </td>
    </tr>
  )
}
