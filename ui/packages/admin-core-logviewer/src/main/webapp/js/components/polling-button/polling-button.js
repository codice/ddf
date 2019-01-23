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
import { togglePolling } from '../../actions'
import { connect } from 'react-redux'

const pollingButton = ({ isPolling, onTogglePolling }) => {
  if (isPolling) {
    return (
      <button className="button-live" onClick={onTogglePolling}>
        <span className="status-text">
          <b>LIVE</b>
        </span>
        <span>◉</span>
      </button>
    )
  } else {
    return (
      <button className="button-paused" onClick={onTogglePolling}>
        <span className="status-text">
          <b>PAUSED</b>
        </span>
        <span>◉</span>
      </button>
    )
  }
}

const mapStateToProps = ({ isPolling }) => ({ isPolling })

export default connect(
  mapStateToProps,
  {
    onTogglePolling: togglePolling,
  }
)(pollingButton)
