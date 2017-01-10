import React from 'react'

import { connect } from 'react-redux'
import { clearWizard } from '../../actions'
import Mount from '../../components/mount'

const Wizard = ({ id, children, clearWizard }) => (
  <Mount key={id} off={clearWizard}>{children}</Mount>
)

export default connect(null, { clearWizard })(Wizard)
