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
import { dismissError } from '../../actions'
import { connect } from 'react-redux'

const errorMessage = ({ errorState, onDismissError }) => {
  if (errorState.isInError) {
    return (
      <div onClick={onDismissError} className="error-message">
        <span>{errorState.message}</span>
        <span className="float-right">&times;</span>
      </div>
    )
  } else {
    return <span />
  }
}

const mapStateToProps = ({ errorState }) => ({ errorState })

export default connect(
  mapStateToProps,
  {
    onDismissError: dismissError,
  }
)(errorMessage)
