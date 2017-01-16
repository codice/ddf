import React from 'react'

import { Link } from 'react-router'

import {
  Stage,
  StageControls,
  Title,
  Description,
  Submit
} from '../../components/stage'

export default ({ id }) => (
  <Stage id={id}>
    <Title>Success!</Title>

    <Description>
      The LDAP configuration has been successfully saved! Now that your
      LDAP is configured, the final step is to use it to secure REST
      endpoints.
    </Description>

    <StageControls>
      <Link to='/web-context-policy-manager'>
        <Submit label='Go to Web Context Policy Manager' />
      </Link>
    </StageControls>
  </Stage>
)
