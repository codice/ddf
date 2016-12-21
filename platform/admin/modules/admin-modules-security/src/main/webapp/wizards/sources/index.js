import React from 'react'
import { connect } from 'react-redux'
import { getSourceStage, getIsSubmitting } from './reducer'
import Wizard from '../components/wizard'

import Flexbox from 'flexbox-react'
import CircularProgress from 'material-ui/CircularProgress'

import styles from './styles.less'

import {
  WelcomeStage,
  DiscoveryStage,
  SourceSelectionStage,
  ConfirmationStage,
  CompletedStage,
  ManualEntryStage
} from './stages'

/*
  - welcomeStage
  - discoveryStage
  - sourceSelectionStage
  - confirmationStage
  - completedStage
  - manualEntryStage
*/

let StageRouter = ({ stage }) => {
  const stageMapping = {
    welcomeStage: <WelcomeStage />,
    discoveryStage: <DiscoveryStage />,
    sourceSelectionStage: <SourceSelectionStage />,
    confirmationStage: <ConfirmationStage />,
    completedStage: <CompletedStage />,
    manualEntryStage: <ManualEntryStage />
  }
  return (stageMapping[stage])
}
StageRouter = connect((state) => ({ stage: getSourceStage(state) }))(StageRouter)

let SourceApp = ({ isSubmitting = false, value = {}, setDefaults }) => (
  <Wizard id='sources'>
    <div style={{width: '100%', height: '100%'}}>
      {isSubmitting
        ? <div className={styles.submitting}>
          <Flexbox justifyContent='center' alignItems='center' width='100%'>
            <CircularProgress size={60} thickness={7} />
          </Flexbox>
        </div>
        : null}
      <StageRouter />
    </div>
  </Wizard>
)
SourceApp = connect((state) => ({ isSubmitting: getIsSubmitting(state) }))(SourceApp)

export default SourceApp
