import React from 'react'

import { connect } from 'react-redux'

import { getConfig } from '../../../reducer'

import {
  Stage,
  StageControls,
  Title,
  Description,
  Save,
  Back
} from '../../components/stage'

const getLdapUseCase = (state) => {
  const useCase = getConfig(state, 'ldapUseCase')
  if (useCase !== undefined) {
    return useCase.value
  }
}

const useCaseDescription = (ldapUseCase) => {
  switch (ldapUseCase) {
    case 'loginAndCredentialStore':
      return 'login source & credential store'
    case 'login' :
      return 'login source'
    default:
      return 'credential store'
  }
}

const embeddedDefaults = {
  embeddedLdapPort: 1389,
  embeddedLdapsPort: 1636,
  embeddedLdapAdminPort: 4444,
  embeddedLdapStorageLocation: 'etc/org.codice.opendj/ldap',
  ldifPath: 'etc/org.codice.opendj/ldap'
}

const ConfigureEmbeddedLdap = ({ id, disabled, ldapUseCase }) => (
  <Stage id={id} defaults={embeddedDefaults}>
    <Title>Install Embedded LDAP</Title>
    <Description>
      Installing Embedded LDAP will start up the internal LDAP and
      configure it as a {useCaseDescription(ldapUseCase)}.
    </Description>

    <StageControls>
      <Back disabled={disabled} />
      <Save id={id} disabled={disabled} url='/admin/beta/config/persist/embedded-ldap/defaults' configType='embedded-ldap' nextStageId='final-stage' />
    </StageControls>
  </Stage>
)

export default connect(
  (state) => ({ ldapUseCase: getLdapUseCase(state) })
)(ConfigureEmbeddedLdap)
