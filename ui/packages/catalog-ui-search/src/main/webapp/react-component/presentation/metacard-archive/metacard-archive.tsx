/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

import { hot } from 'react-hot-loader'
import * as React from 'react'
import styled from '../../styles/styled-components'
import { Button, buttonTypeEnum } from '../button'
import LoadingCompanion from '../../container/loading-companion'

type Props = {
  handleArchive: () => void
  handleRestore: () => void
  isDeleted: boolean
  loading: boolean
}

const MainText = styled.span`
  display: block;
`

const SubText = styled.span`
  display: block;
  font-size: ${props => props.theme.mediumFontSize};
`

const ArchiveButton = styled(Button)`
  width: 100%;
  height: auto;
`

const render = (props: Props) => {
  const { handleArchive, handleRestore, isDeleted, loading } = props
  return (
    <LoadingCompanion loading={loading}>
      {!isDeleted ? (
        <ArchiveButton
          buttonType={buttonTypeEnum.negative}
          onClick={handleArchive}
          data-help="This will remove the item(s) from standard search results.
To restore archived items, you can click on 'File' in the toolbar,
and then click 'Restore Archived Items'."
        >
          <MainText>Archive item(s)</MainText>
          <SubText>
            WARNING: This will remove the item(s) from standard search results.
          </SubText>
        </ArchiveButton>
      ) : (
        <ArchiveButton
          buttonType={buttonTypeEnum.positive}
          onClick={handleRestore}
          data-help="This will restore the item(s) to standard search results."
        >
          <MainText>Restore item(s)</MainText>
        </ArchiveButton>
      )}
    </LoadingCompanion>
  )
}

export default hot(module)(render)
