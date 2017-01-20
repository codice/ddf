import React from 'react'
import { connect } from 'react-redux'

import { getSourceStage, getStagesClean, getConfig, getStageProgress, getConfigTypeById } from './reducer'
import { setNavStage, setConfigSource } from './actions'

import IconButton from 'material-ui/IconButton'
import {RadioButton, RadioButtonGroup} from 'material-ui/RadioButton'
import Flexbox from 'flexbox-react'
import CheckIcon from 'material-ui/svg-icons/action/check-circle'
import CloseIcon from 'material-ui/svg-icons/navigation/cancel'
import AlertIcon from 'material-ui/svg-icons/alert/warning'
import InfoIcon from 'material-ui/svg-icons/action/info'
import {green500, red500} from 'material-ui/styles/colors'
import RaisedButton from 'material-ui/RaisedButton'
import { editConfigs } from '../../actions'

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

import ldapStyles from '../ldap/styles.less'

import {
  Input,
  Password,
  Hostname,
  Port,
  Select
} from '../inputs'

export const WidthConstraint = ({ children }) => (
  <div className={widthConstraintStyle}>
    {children}
  </div>
)

export const ConstrainedInput = ({ id, label, description, value }) => (
  <WidthConstraint>
    <Input id={id} label={label} />
    <DescriptionIcon description={description} />
  </WidthConstraint>
)

export const ConstrainedPasswordInput = ({ id, label, description }) => (
  <WidthConstraint>
    <Password id={id} label={label} />
    <DescriptionIcon description={description} />
  </WidthConstraint>
)

export const ConstrainedHostnameInput = ({ id, label, description }) => (
  <WidthConstraint>
    <Hostname id={id} label={label} />
    <DescriptionIcon description={description} />
  </WidthConstraint>
)

export const ConstrainedPortInput = ({ id, label, description }) => (
  <WidthConstraint>
    <Port id={id} label={label} value={8993} />
    <DescriptionIcon description={description} />
  </WidthConstraint>
)

export const ConstrainedSelectInput = ({ id, label, description, options }) => (
  <WidthConstraint>
    <Select id={id} label={label} options={options} />
    <DescriptionIcon description={description} />
  </WidthConstraint>
)

export const Message = ({type, message}) => (
  <div className={type === 'FAILURE' ? ldapStyles.error : ldapStyles.success}>{message}</div>
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

export const StatusPage = ({succeeded}) => {
  if (succeeded) {
    return (
      <CheckIcon style={statusPageStyles} color={green500} />
    )
  }
  return (
    <CloseIcon style={statusPageStyles} color={red500} />
  )
}

export const ButtonBox = ({ disabled, id, path = [], label, description, children = [] }) => (
  <div>
    <Flexbox justifyContent='center' flexDirection='row'>
      {children}
    </Flexbox>
  </div>
)

export const Info = ({id, title, subtitle}) => (
  <div>
    <p id={id} className={infoTitle}>{title}</p>
    <p id={id} className={infoSubtitle}>{subtitle}</p>
  </div>
)

export const CenteredElements = ({ children, stageIndex, className }) => (
  <Flexbox alignItems='center' className={[outerFlex, animated, fadeIn].join(' ')} style={{width: '100%', height: '100%'}} flexDirection='column' justifyContent='center'>
    {children}
  </Flexbox>
)

export const ConstrainedSourceInfo = ({ id, value, label }) => (
  <WidthConstraint>
    <SourceInfo id={id} value={value} label={label} />
  </WidthConstraint>
)

export const SourceInfo = ({ id, label, value }) => (
  <div style={{ fontSize: '16px', lineHeight: '24px', width: '100%', display: 'inline-block', position: 'relative', height: '200ms' }}>
    <label htmlFor={id} style={{ position: 'absolute', lineHeight: '22px', top: '30px', transform: 'scale(0.75) translate(0px, -28px)', transformOrigin: 'left top 0px' }}>{label}</label>
    <p id={id} style={{ position: 'relative', height: '100%', margin: '28px 0px 7px', whiteSpace: 'nowrap' }}>{value}</p>
  </div>
)

const prettyName = (id) => id.replace('-', ' ')

const SourceRadioButtonsView = ({ disabled, options = [], onEdits, configurationType, setSource, displayName }) => {
  return (
    <div style={{display: 'inline-block', margin: '10px'}}>
      {options.map((item, i) => (
        <SourceRadioButton key={i} label={prettyName(item.configurationType)} value={item.configurationType} disabled={disabled} valueSelected={configurationType} item={item} onSelect={() => setSource(options[i])} />
      ))}
    </div>
  )
}

const mapStateToProps = (state) => {
  const config = getConfig(state, 'configurationType')

  return {
    configurationType: config === undefined ? undefined : config.value,
    displayName: (id) => getConfigTypeById(state, id)
  }
}

const mapDispatchToProps = (dispatch, { id }) => ({
  setSource: (source) => dispatch(setConfigSource(source)),
  onEdits: (values) => dispatch(editConfigs(values))
})

export const SourceRadioButtons = connect(mapStateToProps, mapDispatchToProps)(SourceRadioButtonsView)

const alertMessage = 'SSL certificate is untrusted and possibly insecure'

const SourceRadioButton = ({ disabled, value, label, valueSelected = 'undefined', onSelect, item }) => {
  if (item.trustedCertAuthority) {
    return (
      <div>
        <RadioButtonGroup name={value} valueSelected={valueSelected} onChange={onSelect}>
          <RadioButton disabled={disabled}
            style={{whiteSpace: 'nowrap', padding: '3px', fontSize: '16px'}}
            value={value}
            label={label} />
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
            label={label} />
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

export const BackNav = ({onClick, disabled}) => {
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

const ForwardNavView = ({onClick, clean, currentStage, maxStage, disabled}) => {
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
export const ForwardNav = connect((state) => ({
  clean: getStagesClean(state),
  maxStage: getStageProgress(state),
  currentStage: getSourceStage(state)}))(ForwardNavView)

const NavPanesView = ({ children, backClickTarget, forwardClickTarget, setNavStage, backDisabled = false, forwardDisabled = false }) => (
  <Flexbox className={stageStyle} justifyContent='center' flexDirection='row'>
    <BackNav disabled={backDisabled} onClick={() => setNavStage(backClickTarget)} />
    {children}
    <ForwardNav disabled={forwardDisabled} onClick={() => setNavStage(forwardClickTarget)} />
  </Flexbox>
)
export const NavPanes = connect(null, { setNavStage: setNavStage })(NavPanesView)

export const Submit = ({ label = 'Submit', onClick, disabled = false }) => (
  <RaisedButton className={submit} label={label} disabled={disabled} primary onClick={onClick} />
)
