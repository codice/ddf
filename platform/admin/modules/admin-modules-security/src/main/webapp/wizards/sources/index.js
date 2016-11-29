import React from 'react'
import { connect } from 'react-redux'

import { getSourceStage, getStagesClean, getConfig, getStageProgress, getSourceSelections, getSelectedSource, getSourceName } from './reducer'
import { changeStage, setNavStage, testConfig, clearConfiguration } from './actions'

import RaisedButton from 'material-ui/RaisedButton'
import IconButton from 'material-ui/IconButton'
import {RadioButton, RadioButtonGroup} from 'material-ui/RadioButton'
import Flexbox from 'flexbox-react'
import CheckIcon from 'material-ui/svg-icons/action/check-circle'
import CloseIcon from 'material-ui/svg-icons/navigation/cancel'
import AlertIcon from 'material-ui/svg-icons/alert/warning'
import InfoIcon from 'material-ui/svg-icons/action/info'
import {green500, red500} from 'material-ui/styles/colors'
import { Link } from 'react-router'
import { setDefaults, editConfig } from '../../actions'
import Mount from '../../components/mount'

import LeftIcon from 'material-ui/svg-icons/hardware/keyboard-arrow-left'
import RightIcon from 'material-ui/svg-icons/hardware/keyboard-arrow-right'

import {
  stageStyle,
  descriptionIconStyle,
  widthConstraintStyle,
  infoTitle,
  infoSubtitle,
  submit,
  animated,
  fadeIn,
  outerFlex,
  navButtonStyles,
  navButtonStylesDisabled
} from './styles.less'

import {
  Input,
  Password,
  Hostname,
  Port,
  Select
} from '../inputs'

const mapStateToProps = (state, { id }) => getConfig(state, id)

const mapDispatchToProps = (dispatch, { id }) => ({
  onEdit: (value) => dispatch(editConfig(id, value))
})

// Components
const WidthConstraint = ({ children }) => (
  <div className={widthConstraintStyle}>
    {children}
  </div>
)

let ConstrainedInput = ({ id, label, description, value }) => (
  <WidthConstraint>
    <Input id={id} label={label} />
    <DescriptionIcon description={description} />
  </WidthConstraint>
)

const ConstrainedPasswordInput = ({ id, label, description }) => (
  <WidthConstraint>
    <Password id={id} label={label} />
    <DescriptionIcon description={description} />
  </WidthConstraint>
)

const ConstrainedHostnameInput = ({ id, label, description }) => (
  <WidthConstraint>
    <Hostname id={id} label={label} />
    <DescriptionIcon description={description} />
  </WidthConstraint>
)

const ConstrainedPortInput = ({ id, label, description }) => (
  <WidthConstraint>
    <Port id={id} label={label} value={8993} />
    <DescriptionIcon description={description} />
  </WidthConstraint>
)

const ConstrainedSelectInput = ({ id, label, description }) => (
  <WidthConstraint>
    <Select id={id} label={label} />
    <DescriptionIcon description={description} />
  </WidthConstraint>
)

const DescriptionIcon = ({ description }) => {
  if (description) {
    return (
      <span className={descriptionIconStyle}>
        <IconButton tooltip={description} touch tooltipPosition='top-left'><InfoIcon /></IconButton>
      </span>
    )
  } else {
    return null
  }
}

const statusPageStyles = { width: '300px', height: '300px' }

const StatusPage = ({succeeded}) => {
  if (succeeded) {
    return (
      <CheckIcon style={statusPageStyles} color={green500} />
    )
  }
  return (
    <CloseIcon style={statusPageStyles} color={red500} />
  )
}

const ButtonBox = ({ disabled, id, path = [], label, description, children = [] }) => (
  <div>
    <Flexbox justifyContent='center' flexDirection='row'>
      {children}
    </Flexbox>
  </div>
)

const Info = ({id, title, subtitle}) => (
  <div>
    <p id={id} className={infoTitle}>{title}</p>
    <p id={id} className={infoSubtitle}>{subtitle}</p>
  </div>
)

const CenteredElements = ({ children, stageIndex, className }) => (
  <Flexbox alignItems='center' className={[outerFlex, animated, fadeIn].join(' ')} style={{width: '100%', height: '100%'}} flexDirection='column' justifyContent='center'>
    {children}
  </Flexbox>
)

const ConstrainedSourceInfo = ({ id, value, label }) => (
  <WidthConstraint>
    <SourceInfo id={id} value={value} label={label} />
  </WidthConstraint>
)

const SourceInfo = ({ id, label, value }) => (
  <div style={{ fontSize: '16px', lineHeight: '24px', width: '100%', display: 'inline-block', position: 'relative', height: '200ms' }}>
    <label htmlFor={id} style={{ position: 'absolute', lineHeight: '22px', top: '30px', transform: 'scale(0.75) translate(0px, -28px)', transformOrigin: 'left top 0px' }}>{label}</label>
    <p id={id} style={{ position: 'relative', height: '100%', margin: '28px 0px 7px', whiteSpace: 'nowrap' }}>{value}</p>
  </div>
)

let SourceRadioButtons = ({ id, disabled, path, value = { value: { displayName: 'undefined' } }, options = [], onEdit, defaultOption = {} }) => {
  return (
    <div style={{display: 'inline-block', margin: '10px'}}>
      {options.map((item, i) => (
        <SourceRadioButton key={i} value={item.displayName} disabled={disabled} valueSelected={value.displayName} item={item} onSelect={() => onEdit(options[i])} />
    ))}
    </div>
  )
}
SourceRadioButtons = connect(mapStateToProps, mapDispatchToProps)(SourceRadioButtons)

const alertMessage = 'SSL certificate is untrusted and possibly insecure'

const SourceRadioButton = ({ disabled, value, valueSelected, onSelect, item }) => {
  if (item.trustedCertAuthority) {
    return (
      <div>
        <RadioButtonGroup name={value} valueSelected={valueSelected} onChange={onSelect}>
          <RadioButton disabled={disabled}
            style={{whiteSpace: 'nowrap', padding: '3px', fontSize: '16px'}}
            value={value}
            label={value} />
        </RadioButtonGroup>
      </div>
    )
  } else {
    return (
      <div>
        <RadioButtonGroup style={{ display: 'inline-block', color: '#f90' }} name={value} valueSelected={valueSelected} onChange={onSelect}>
          <RadioButton disabled={disabled}
            style={{
              display: 'inline-block',
              whiteSpace: 'nowrap',
              padding: '3px',
              fontSize: '16px'
            }}
            value={value}
            labelStyle={{ color: '#f90' }}
            label={value} />
        </RadioButtonGroup>
        <IconButton
          touch
          iconStyle={{
            color: '#f90'
          }}
          style={{
            display: 'inline-block',
            color: '#f00',
            width: '24px',
            height: '24px',
            padding: '0px'
          }}
          tooltip={alertMessage}
          tooltipPosition='top-left'>
          <AlertIcon />
        </IconButton>
      </div>
    )
  }
}

let BackNav = ({onClick, disabled}) => {
  if (!disabled) {
    return (
      <span className={navButtonStyles} onClick={onClick}>
        <LeftIcon style={{height: '100%', width: '100%'}} />
      </span>
    )
  } else {
    return (
      <span className={navButtonStylesDisabled}>
        <LeftIcon style={{color: 'lightgrey', height: '100%', width: '100%'}} />
      </span>
    )
  }
}
BackNav = connect(null, {})(BackNav)

let ForwardNav = ({onClick, clean, currentStage, maxStage, disabled}) => {
  if (clean && (currentStage !== maxStage) && !disabled) {
    return (
      <span className={navButtonStyles} onClick={onClick}>
        <RightIcon style={{height: '100%', width: '100%'}} />
      </span>
    )
  } else {
    return (
      <span className={navButtonStylesDisabled}>
        <RightIcon style={{color: 'lightgrey', height: '100%', width: '100%'}} />
      </span>
    )
  }
}
ForwardNav = connect((state) => ({
  clean: getStagesClean(state),
  maxStage: getStageProgress(state),
  currentStage: getSourceStage(state)}))(ForwardNav)

let NavPanes = ({ children, backClickTarget, forwardClickTarget, setNavStage, backDisabled = false, forwardDisabled = false }) => (
  <Flexbox className={stageStyle} justifyContent='center' flexDirection='row'>
    <BackNav disabled={backDisabled} onClick={() => setNavStage(backClickTarget)} />
    {children}
    <ForwardNav disabled={forwardDisabled} onClick={() => setNavStage(forwardClickTarget)} />
  </Flexbox>
)
NavPanes = connect(null, { setNavStage: setNavStage })(NavPanes)

// Welcome Stage
const welcomeTitle = 'Welcome to the Source Configuration Wizard'
const welcomeSubtitle = 'This guide will walk you through the discovery and configuration of the ' +
  "various sources that are used to query metadata in other DDF's or external systems. " +
  'To begin, make sure you have the hostname and port of the source you plan to configure.'

let WelcomeStage = ({ changeStage }) => (
  <Flexbox className={stageStyle} justifyContent='center' flexDirection='row'>
    <CenteredElements>
      <Info title={welcomeTitle} subtitle={welcomeSubtitle} />
      <Submit label='Begin Source Setup' onClick={() => changeStage('discoveryStage')} />
    </CenteredElements>
  </Flexbox>
)
WelcomeStage = connect(null, { changeStage: changeStage })(WelcomeStage)

// Discovery Stage
const discoveryTitle = 'Discover Available Sources'
const discoverySubtitle = 'Enter source information to scan for available sources'
const discoveryStageDefaults = {
  sourceHostName: 'localhost',
  sourcePort: 8993
}
let DiscoveryStage = ({ testConfig, setDefaults }) => (
  <Mount on={() => setDefaults(discoveryStageDefaults)}>
    <NavPanes backClickTarget='welcomeStage' forwardClickTarget='sourceSelectionStage'>
      <CenteredElements>
        <Info title={discoveryTitle} subtitle={discoverySubtitle} />
        <ConstrainedHostnameInput id='sourceHostName' label='Hostname' />
        <ConstrainedPortInput id='sourcePort' label='Port' />
        <ConstrainedInput id='sourceUserName' label='Username (optional)' />
        <ConstrainedPasswordInput id='sourceUserPassword' label='Password (optional)' />
        <Submit label='Check' onClick={() => testConfig('/admin/wizard/probe/sources/discoverSources', 'sourceConfiguration', 'sourceSelectionStage')} />
      </CenteredElements>
    </NavPanes>
  </Mount>
)
DiscoveryStage = connect(null, { testConfig, setDefaults })(DiscoveryStage)

// Source Selection Stage
const sourceSelectionTitle = 'Sources Found!'
const sourceSelectionSubtitle = 'Choose which source to add'
const noSourcesFoundTitle = 'No Sources Were Found'
const noSourcesFoundSubtitle = 'Click below to enter source information manually, or go back to enter a different hostname/port.'

let SourceSelectionStage = ({sourceSelections = [], changeStage, selectedSource}) => {
  if (sourceSelections.length !== 0) {
    return (<NavPanes backClickTarget='discoveryStage' forwardClickTarget='confirmationStage'>
      <CenteredElements>
        <Info title={sourceSelectionTitle} subtitle={sourceSelectionSubtitle} />
        <SourceRadioButtons options={sourceSelections}
          selectedValue={sourceSelections[0]}
          id='selectedSource' />
        <Submit label='Next' disabled={selectedSource === undefined} onClick={() => changeStage('confirmationStage')} />
      </CenteredElements>
    </NavPanes>)
  } else {
    return (<NavPanes backClickTarget='discoveryStage' forwardClickTarget='manualEntryStage'>
      <CenteredElements>
        <Info title={noSourcesFoundTitle} subtitle={noSourcesFoundSubtitle} />
        <Submit label='Enter Information Manually' onClick={() => changeStage('manualEntryStage')} />
      </CenteredElements>
    </NavPanes>)
  }
}
SourceSelectionStage = connect((state) => ({sourceSelections: getSourceSelections(state), selectedSource: getSelectedSource(state)}), { changeStage })(SourceSelectionStage)

// Confirmation Stage
const confirmationTitle = 'Finalize Source Configuration'
const confirmationSubtitle = 'Name your source, confirm details, and press finish to add source'
const sourceNameDescription = 'Use something descriptive to distinguish it from your other sources'
let ConfirmationStage = ({selectedSource, changeStage, sourceName}) => (
  <NavPanes backClickTarget='sourceSelectionStage' forwardClickTarget='completedStage'>
    <CenteredElements>
      <Info title={confirmationTitle} subtitle={confirmationSubtitle} />
      <ConstrainedInput id='sourceName' label='Source Name' description={sourceNameDescription} />
      <ConstrainedSourceInfo label='Source Address' value={selectedSource.endpointUrl} />
      <ConstrainedSourceInfo label='Username' value={selectedSource.sourceUserName || 'none'} />
      <ConstrainedSourceInfo label='Password' value={selectedSource.sourceUserPassword || 'none'} />
      <Submit label='Finish' disabled={sourceName === undefined || sourceName === ''} onClick={() => changeStage('completedStage')} />
    </CenteredElements>
  </NavPanes>
)
ConfirmationStage = connect((state) => ({selectedSource: getSelectedSource(state), sourceName: getSourceName(state)}), { changeStage })(ConfirmationStage)

// Completed Stage
const completedTitle = 'All Done!'
const completedSubtitle = 'Your source has been added successfully'
let CompletedStage = ({ changeStage, clearConfiguration }) => (
  <Flexbox className={stageStyle} justifyContent='center' flexDirection='row'>
    <CenteredElements>
      <Info title={completedTitle} subtitle={completedSubtitle} />
      <StatusPage succeeded />
      <ButtonBox>
        <Link to='/'>
          <Submit label='Go Home' onClick={clearConfiguration} />
        </Link>
        <Submit label='Add Another Source' onClick={() => { clearConfiguration(); changeStage('welcomeStage') }} />
      </ButtonBox>
    </CenteredElements>
  </Flexbox>
)
CompletedStage = connect(null, { changeStage, clearConfiguration })(CompletedStage)

// Manual Entry Page
const manualEntryTitle = 'Manual Source Entry'
const manualEntrySubtitle = 'Choose a source configuration type and enter a source URL'
const ManualEntryPage = () => (
  <NavPanes backClickTarget='sourceSelectionStage' forwardClickTarget='confirmationStage'>
    <CenteredElements>
      <Info title={manualEntryTitle} subtitle={manualEntrySubtitle} />
      <ConstrainedSelectInput id='ManualEntryStageSourceConfigurationTypeInput' label='Source Configuration Type' options={['Mock CSW Source Type', 'Mock OpenSearch Source Type', 'Mock Other Source Type']} />
      <ConstrainedInput id='ManualEntryStageSourceUrlInput' label='Source URL' />
      <Submit label='Next' nextStage='confirmationStage' />
    </CenteredElements>
  </NavPanes>
)

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
    manualEntryStage: <ManualEntryPage />
  }
  return (stageMapping[stage])
}
StageRouter = connect((state) => ({ stage: getSourceStage(state) }))(StageRouter)

let Submit = ({ label = 'Submit', onClick, disabled = false }) => (
  <RaisedButton className={submit} label={label} disabled={disabled} primary onClick={onClick} />
)

const SourceApp = ({ value = {}, setDefaults }) => (
  <div style={{width: '100%', height: '100%'}}>
    <StageRouter />
  </div>
)

export default SourceApp
