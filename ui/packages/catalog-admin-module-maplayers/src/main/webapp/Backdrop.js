import React from 'react'

import muiThemeable from 'material-ui/styles/muiThemeable'

const isInIframe = () => window !== window.top

const BackdropView = ({ muiTheme, children, ...rest }) => {
  let fixed = {
    height: '100%',
    backgroundColor: muiTheme.palette.backdropColor
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
