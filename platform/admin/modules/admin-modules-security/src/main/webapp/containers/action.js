import React from 'react'

import { submit } from '../actions'
import { connect } from 'react-redux'

import { isSubmitting } from '../reducer'

import RaisedButton from 'material-ui/RaisedButton'

const Action = ({ submitting, label, onSubmit, ...rest }) => (
  <RaisedButton style={{ marginLeft: 10 }} onClick={() => { onSubmit(rest) }} label={label} primary disabled={submitting} />
)

const mapStateToProps = (state) => ({ submitting: isSubmitting(state) })

export default connect(mapStateToProps, { onSubmit: submit })(Action)
