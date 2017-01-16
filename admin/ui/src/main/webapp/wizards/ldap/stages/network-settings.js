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
  Hostname,
  Port,
  Select
} from '../../inputs'

const NetworkSettings = ({ id, disabled }) => (
  <Stage id={id} defaults={{ port: 1636, encryptionMethod: 'LDAPS', hostName: 'localhost' }}>
    <Title>LDAP Network Settings</Title>
    <Description>
      Lets start with the network configurations of your LDAP store.
    </Description>

    <Hostname id='hostName' disabled={disabled} />
    <Port id='port' disabled={disabled} options={[389, 636]} />
    <Select id='encryptionMethod'
      label='Encryption Method'
      disabled={disabled}
      options={[ 'None', 'LDAPS', 'StartTLS' ]} />

    <StageControls>
      <Back disabled={disabled} />
      <Next id={id}
        disabled={disabled}
        url='/admin/beta/config/test/ldap/connection'
        nextStageId='bind-settings' />
    </StageControls>
  </Stage>
)

export default NetworkSettings
