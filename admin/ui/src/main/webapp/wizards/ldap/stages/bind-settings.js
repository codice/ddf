import React from 'react'

import {
  Stage,
  StageControls,
  Title,
  Description,
  Back,
  Next
} from '../../components/stage'

import {
  Input,
  Password,
  Select
} from '../../inputs'

const BindSettings = ({ id, disabled, configs = {} }) => {
  const { bindUserMethod, encryptionMethod } = configs
  let bindUserMethodOptions = ['Simple']

  if (encryptionMethod === 'LDAPS' || encryptionMethod === 'StartTLS') {
    bindUserMethodOptions.push('Digest MD5 SASL')
  }

  return (
    <Stage id={id} defaults={{bindUserDn: 'cn=admin', bindUserPassword: 'secret', bindUserMethod: 'Simple'}}>
      <Title>LDAP Bind User Settings</Title>
      <Description>
        Now that we've figured out the network environment, we need to
        bind a user to the LDAP Store to retrieve additional information.
      </Description>

      <Input id='bindUserDn' disabled={disabled} label='Bind User DN' />
      <Password id='bindUserPassword' disabled={disabled} label='Bind User Password' />
      <Select id='bindUserMethod'
        label='Bind User Method'
        disabled={disabled}
        options={bindUserMethodOptions} />
      {/* removed options: 'SASL', 'GSSAPI SASL' */}
      {/* TODO GSSAPI SASL only */}
      {/* <Input id='bindKdcAddress' disabled={disabled} label='KDC Address (for Kerberos authentication)' /> */}
      {/* TODO GSSAPI and Digest MD5 SASL only */}
      {
        (bindUserMethod === 'Digest MD5 SASL')
          ? (<Input id='bindRealm' disabled={disabled} label='Realm (for Kerberos and Digest MD5 authentication)' />)
          : null
      }

      <StageControls>
        <Back disabled={disabled} />
        <Next id={id}
          disabled={disabled}
          url='/admin/beta/config/test/ldap/bind'
          nextStageId='directory-settings' />
      </StageControls>
    </Stage>
  )
}

export default BindSettings

