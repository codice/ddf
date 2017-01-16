import React from 'react'

import {
  Stage,
  StageControls,
  Title,
  Description,
  Begin
} from '../../components/stage'

import { RadioSelection } from '../../inputs'

const LdapUseCases = [
  {
    value: 'login',
    label: 'Login'
  },
  {
    value: 'credentialStore',
    label: 'Credential store'
  },
  {
    value: 'loginAndCredentialStore',
    label: 'Login and Credential Store'
  }
]

// TODO update description to described LDAP as a login or credential store
// TODO Make the value selected from the radio button persist
const IntroductionStage = ({ id, disabled, configs: { ldapUseCase } = {} }) => (
  <Stage id={id}>
    <Title>Welcome to the LDAP Configuration Wizard</Title>
    <Description>
      This guide will walk through setting up the LDAP as an
      authentication source for users. To begin, make sure you
      have the hostname and port of the LDAP you plan to. How
      do you plan to use LDAP?
    </Description>
    <RadioSelection
      id='ldapUseCase'
      options={LdapUseCases}
      name='LDAP Use Cases'
      disabled={disabled} />
    <StageControls justifyContent='center'>
      <Begin disabled={disabled || !ldapUseCase} nextStageId='ldap-type-selection' />
    </StageControls>
  </Stage>
)

export default IntroductionStage
