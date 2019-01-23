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
import * as actions from '../../actions'

const format = time => {
  return moment(time).format('D MMM YYYY, HH:mm:ss')
}

// log entry to display
export default ({ entry, marks, expandedHash, dispatch }) => {
  // check if marks exist for filter highlighting
  const tryMark = key => {
    const mark = marks[key]
    const displayString = entry[key]
    if (mark) {
      const first = displayString.slice(0, mark.start)
      const second = displayString.slice(mark.start, mark.end)
      const third = displayString.slice(mark.end)
      return (
        <span>
          <span className="dim">{first}</span>
          <mark>{second}</mark>
          <span className="dim">{third}</span>
        </span>
      )
    } else {
      return <span>{displayString}</span>
    }
  }

  const expandEntry = () => {
    dispatch(actions.expandEntry(entry.hash))
  }

  const getMessageClasses = () => {
    return entry.hash === expandedHash
      ? 'rowData messageExpanded'
      : 'rowData message'
  }

  const getRowClasses = () => {
    if (entry.hash === expandedHash) {
      return 'rowAnimation selectedRow ' + levels(entry.level)
    } else {
      return 'rowAnimation ' + levels(entry.level)
    }
  }

  return (
    <tr onClick={expandEntry} className={getRowClasses()}>
      <td className="rowData timestampColumn">{format(entry.timestamp)}</td>
      <td className="rowData levelColumn">{entry.level}</td>
      <td className={getMessageClasses()}>{tryMark('message')}</td>
      <td className="rowData bundleColumn">{tryMark('bundleName')}</td>
    </tr>
  )
}
