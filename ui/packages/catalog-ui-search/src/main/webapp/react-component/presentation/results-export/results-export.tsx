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
import * as React from 'react'
import { hot } from 'react-hot-loader'
import { Button, buttonTypeEnum } from '../../presentation/button'
import styled from '../../styles/styled-components'
const { Menu, MenuItem } = require('../../menu')
const Dropdown = require('../../dropdown')

const Root = styled.div`
  padding: ${props => props.theme.largeSpacing};

  button {
    margin-top: ${props => props.theme.minimumSpacing};
    width: 100%;
  }

  .export-option {
    margin-bottom: ${props => props.theme.largeSpacing};
  }
`

type ExportFormat = {
  id: string
  displayName: string
}

type Props = {
  selectedFormat: string
  exportFormats: ExportFormat[]
  downloadDisabled: boolean
  onDownloadClick: () => void
  handleExportOptionChange: () => void
}

const ResultsExport = (props: Props) => {
  const {
    selectedFormat,
    exportFormats,
    downloadDisabled,
    onDownloadClick,
    handleExportOptionChange,
  } = props

  return (
    <Root>
      <div className="export-option">
        <p>Export Format:</p>
        <Dropdown label={selectedFormat}>
          <Menu value={selectedFormat} onChange={handleExportOptionChange}>
            {exportFormats.map(option => (
              <MenuItem key={option.id} value={option.displayName} />
            ))}
          </Menu>
        </Dropdown>
      </div>
      <Button
        disabled={downloadDisabled}
        buttonType={buttonTypeEnum.primary}
        icon="fa fa-download"
        text="Download"
        onClick={onDownloadClick}
      />
    </Root>
  )
}

export default hot(module)(ResultsExport)
