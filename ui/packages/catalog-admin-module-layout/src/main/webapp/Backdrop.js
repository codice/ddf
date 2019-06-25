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

import muiThemeable from 'material-ui/styles/muiThemeable'

const isInIframe = () => window !== window.top

const BackdropView = ({ muiTheme, children, ...rest }) => {
  let fixed = {
    height: '100%',
    backgroundColor: muiTheme.palette.backdropColor,
  }

  if (isInIframe()) {
    fixed.borderRadius = '4px'
  } else {
    fixed.minHeight = '100vh'
  }

  return (
    <div style={fixed} {...rest}>
      {children}
    </div>
  )
}

export default muiThemeable()(BackdropView)
