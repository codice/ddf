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
import VisibilitySensor from 'react-visibility-sensor'

import './log-viewer.less'

import LevelSelector from '../level-selector/level-selector'
import TextFilter from '../text-filter/text-filter'
import LogEntry from '../log-entry/log-entry'
import * as actions from '../../actions'
import filterLogs from '../../filter'
import PollingButton from '../polling-button/polling-button'
import ErrorMessage from '../error-message/error-message'

export default ({ dispatch, expandedHash, displaySize, logs, filter }) => {
  const filteredLogs = filterLogs(filter, logs)

  const displayedLogs = filteredLogs
    .slice(0, displaySize)
    .map(function(row, i) {
      return (
        <LogEntry
          key={i}
          entry={row.entry}
          marks={row.marks}
          expandedHash={expandedHash}
          dispatch={dispatch}
        />
      )
    })

  // grow the log display when the bottom is reached
  const growLogs = isVisible => {
    if (isVisible) {
      dispatch(actions.grow())
    }
  }

  // show loading bar is there are more logs, hide if the end is reached
  const loading = () => {
    if (filteredLogs.length > 0 && displayedLogs.length < filteredLogs.length) {
      return (
        <VisibilitySensor
          onChange={growLogs}
          partialVisibility={Boolean(true)}
          delay={200}
        >
          <div className="loading">Loading...</div>
        </VisibilitySensor>
      )
    }
  }

  const select = level => {
    dispatch(actions.filter({ level: level }))
  }

  const textFilter = field => {
    const on = filterObject => {
      dispatch(actions.filter(filterObject))
    }

    return <TextFilter field={field} value={filter[field]} onChange={on} />
  }

  const getTableClasses = () => {
    if (expandedHash === null) {
      return 'table'
    } else {
      return 'table dimUnselected'
    }
  }

  const deselect = () => {
    dispatch(actions.expandEntry())
  }

  return (
    <div className="container">
      <div className="filterRow">
        <table className="table" onClick={deselect}>
          <thead>
            <tr>
              <td className="header" width={175}>
                Time
              </td>
              <td className="header" width={90}>
                Level
              </td>
              <td className="header">Message</td>
              <td className="header" width={200}>
                Bundle
              </td>
            </tr>
            <tr>
              <td className="controls">
                <PollingButton />
              </td>
              <td className="controls">
                <LevelSelector selected={filter.level} onSelect={select} />
              </td>
              <td className="controls">{textFilter('message')}</td>
              <td className="controls">{textFilter('bundleName')}</td>
            </tr>
          </thead>
        </table>
        <ErrorMessage />
      </div>

      <div className="logRows">
        <table className={getTableClasses()}>
          <tbody>{displayedLogs}</tbody>
        </table>
        {loading()}
      </div>
    </div>
  )
}
