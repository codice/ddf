import React from 'react'

import { connect } from 'react-redux'
import { clearWizard } from '../../actions'
import Mount from '../../components/mount'

const Wizard = ({ id, children, clearWizard }) => (
  <Mount key={id} off={clearWizard}>
    <div style={{ width: '100%', height: '100%' }}>{children}</div>
  </Mount>
)

export default connect(null, { clearWizard })(Wizard)
