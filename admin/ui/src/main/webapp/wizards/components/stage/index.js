import React from 'react'

import { connect } from 'react-redux'

import Paper from 'material-ui/Paper'
import RaisedButton from 'material-ui/RaisedButton'
import FlatButton from 'material-ui/FlatButton'

import Flexbox from 'flexbox-react'
import CircularProgress from 'material-ui/CircularProgress'
import Mount from '../../../components/mount'

import {
  probeOptions,
  testConfig,
  setDefaults,
  nextStage,
  prevStage
} from '../../actions'

import { isSubmitting, getMessages } from '../../../reducer'

import * as styles from './styles.less'

const Message = ({type, message}) => (
  <div className={type === 'FAILURE' ? styles.error : styles.success}>{message}</div>
)

const StageView = (props) => {
  const {
    children,
    submitting = false,
    messages = [],

    onInit
  } = props

  return (
    <Mount on={onInit}>
      <Paper className={styles.main}>
        {submitting
          ? <div className={styles.submitting}>
            <Flexbox justifyContent='center' alignItems='center' width='100%'>
              <CircularProgress size={60} thickness={7} />
            </Flexbox>
          </div>
          : null}
        <div className={styles.container}>
          {children}
          {messages.map((msg, i) => <Message key={i} {...msg} />)}
        </div>
      </Paper>
    </Mount>
  )
}

export const Stage = connect((state, {id}) => ({
  messages: getMessages(state, id),
  submitting: isSubmitting(state, id)
}), (dispatch, { id, probeUrl, defaults }) => ({
  onInit: () => {
    if (defaults !== undefined) {
      dispatch(setDefaults(defaults))
    }
    if (probeUrl !== undefined) {
      dispatch(probeOptions(id, probeUrl))
    }
  }
}))(StageView)

export const StageControls = ({children, style = {}, ...rest}) => (
  <Flexbox style={{marginTop: 20, ...style}} justifyContent='space-between' {...rest}>
    {children}
  </Flexbox>
)

export const Title = ({children}) => (
  <h1 className={styles.title}>{children}</h1>
)

export const Description = ({children}) => (
  <p className={styles.description}>{children}</p>
)

const BackView = ({onBack, disabled}) => (
  <FlatButton disabled={disabled} secondary label='back' onClick={onBack} />
)

export const Back = connect(null, {onBack: prevStage})(BackView)

const mapDispatchToPropsNext = (dispatch, {id, url, nextStageId}) => ({
  next: () => dispatch(testConfig(id, url, nextStageId))
})

const NextView = ({next, disabled, nextStageId}) =>
  <RaisedButton label='Next' disabled={disabled} primary onClick={next} />

export const Next = connect(null, mapDispatchToPropsNext)(NextView)

export const Save = connect(null, (dispatch, {id, url, nextStageId, configType}) => ({
  saveConfig: () => dispatch(testConfig(id, url, nextStageId, configType))
}))(({saveConfig}) => (
  <RaisedButton label='Save' primary onClick={saveConfig} />
))

const BeginView = ({onBegin, disabled, next}) => (
  <RaisedButton disabled={disabled} primary label='begin' onClick={next} />
)

export const Begin = connect(null, (dispatch, { nextStageId }) => ({
  next: () => dispatch(nextStage(nextStageId))
}))(BeginView)

export const Submit = ({ label = 'Submit', onClick, disabled = false }) => (
  <RaisedButton label={label} disabled={disabled} primary onClick={onClick} />
)

