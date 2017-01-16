import React from 'react'

import {
  Stage,
  StageControls,
  Title,
  Description,
  Save,
  Back
} from '../../components/stage'

export default ({ id }) => (
  <Stage id={id}>
    <Title>LDAP Confirm</Title>

    <Description>
      All of the values have been successfully verified. Would you like to
      save the LDAP configuration?
    </Description>

    <StageControls>
      <Back />
      <Save id={id} url='/admin/beta/config/persist/ldap/create' nextStageId='final-stage' />
    </StageControls>
  </Stage>
)
