import React from 'react'

import { connect } from 'react-redux'
import { getErrors } from '../reducer'

import { dismissErrors } from '../actions'

import Flexbox from 'flexbox-react'
import Close from 'material-ui/svg-icons/navigation/close'

import styles from './errors.less'

const Errors = ({ errors, onDismiss }) => {
  if (errors === null) return null

  return (
    <div className={styles.message}>
      <Flexbox justifyContent='space-between'>
        {errors}
        <div className={styles.dismiss} onClick={onDismiss}>
          <Close color='white' />
        </div>
      </Flexbox>
    </div>
  )
}

const mapStateToProps = (state) => ({ errors: getErrors(state) })

export default connect(mapStateToProps, { onDismiss: dismissErrors })(Errors)
