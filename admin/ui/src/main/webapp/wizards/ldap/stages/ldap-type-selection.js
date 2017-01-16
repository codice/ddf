import React from 'react'

import {
  Stage,
  StageControls,
  Title,
  Description,
  Begin,
  Back
} from '../../components/stage'

import { RadioSelection } from '../../inputs'

// TODO Make the value selected from the radio button persist
const LdapTypes = [
  {
    value: 'activeDirectory',
    label: 'Active Directory'
  },
  {
    value: 'openDj',
    label: 'OpenDJ'
  },
  {
    value: 'openLdap',
    label: 'OpenLDAP'
  },
  {
    value: 'embeddedLdap',
    label: 'Embedded LDAP (For testing purposes only)'
  },
  {
    value: 'unknown',
    label: 'Not Sure/None Of The Above'
  }
]

const LdapTypeSelection = ({ id, disabled, configs: { ldapType } = {} }) => (
  <Stage id={id}>
    <Title>LDAP Type Selection</Title>
    <Description>
      Select the type of LDAP you plan to connect to.
    </Description>
    <RadioSelection
      id='ldapType'
      options={LdapTypes}
      name='LDAP Type Selections'
      disabled={disabled} />
    <StageControls>
      <Back disabled={disabled} />
      <Begin
        disabled={disabled || !ldapType}
        nextStageId={ldapType === 'embeddedLdap' ? 'configure-embedded-ldap' : 'network-settings'} />
    </StageControls>
  </Stage>
)

export default LdapTypeSelection
