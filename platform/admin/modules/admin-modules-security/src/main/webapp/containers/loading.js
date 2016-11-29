import React from 'react'

import CircularProgress from 'material-ui/CircularProgress'

import { connect } from 'react-redux'

import { isSubmitting } from '../reducer'

import styles from './loading.less'

const Loading = ({ submitting }) => {
  if (submitting) {
    return (
      <div className={styles.loading}>
        <div className={styles.progress} >
          <CircularProgress size={60} thickness={7} />
        </div>
      </div>
    )
  } else {
    return null
  }
}

const mapStateToProps = (state) => ({ submitting: isSubmitting(state) })

export default connect(mapStateToProps)(Loading)
