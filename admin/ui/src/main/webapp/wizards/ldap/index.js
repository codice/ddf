import React from 'react'

import { connect } from 'react-redux'

import { getDisplayedLdapStage } from '../../reducer'

import Wizard from '../components/wizard'

import IntroductionStage from './stages/introduction-stage'
import LdapTypeSelection from './stages/ldap-type-selection'
import ConfigureEmbeddedLdap from './stages/configure-embedded-ldap'
import NetworkSettings from './stages/network-settings'
import BindSettings from './stages/bind-settings'
import DirectorySettings from './stages/directory-settings'
import LdapAttributeMappingStage from './stages/attribute-mapping'
import Confirm from './stages/confirm'
import FinalStage from './stages/final-stage'

const stageMapping = {
  'introduction-stage': IntroductionStage,
  'ldap-type-selection': LdapTypeSelection,
  'configure-embedded-ldap': ConfigureEmbeddedLdap,
  'network-settings': NetworkSettings,
  'bind-settings': BindSettings,
  'directory-settings': DirectorySettings,
  'attribute-mapping': LdapAttributeMappingStage,
  'confirm': Confirm,
  'final-stage': FinalStage
}

const LdapWizardView = ({ id }) => (
  <Wizard id='ldap'>
    {React.createElement(stageMapping[id], { id })}
  </Wizard>
)

const LdapWizard = connect((state) => ({
  id: getDisplayedLdapStage(state)
}))(LdapWizardView)

export default LdapWizard
