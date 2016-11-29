import React from 'react'

import { back } from '../actions'
import { connect } from 'react-redux'

import { canGoBack, isSubmitting } from '../reducer'

import FlatButton from 'material-ui/FlatButton'

const Back = ({ canGoBack, disabled, label, onBack, ...rest }) => (
  <FlatButton onClick={onBack} label='back' secondary disabled={disabled || !canGoBack} />
)

const mapStateToProps = (state, ownProps) => ({
  canGoBack: canGoBack(state),
  disabled: ownProps.disabled || isSubmitting(state)
})

export default connect(mapStateToProps, { onBack: back })(Back)
