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
import Enum from '../../container/enum'
import { Button, buttonTypeEnum } from '../button'
import styled from '../../styles/styled-components'
import Number from '../../container/input-wrappers/number'
const properties = require('../../../js/properties.js')

const Root = styled<{}, 'div'>('div')`
  display: block;
  height: 100%;
  width: 100%;
  overflow: auto;

  button {
    margin-top: ${props => props.theme.minimumSpacing};
    width: 100%;
  }

  .warning {
    text-align: center;
  }
`

type Option = {
  label: string
  value: string
}

type Props = {
  exportSize: string
  exportFormat: string
  handleExportSizeChange: (value: any) => void
  handleExportFormatChange: (value: any) => void
  handleCustomExportCountChange: (value: any) => void
  exportSizeOptions: Option[]
  exportFormatOptions: Option[]
  onDownloadClick: () => void
  warning: string
  customExportCount: number
}

export default hot(module)((props: Props) => {
  const {
    exportSize,
    exportFormat,
    exportSizeOptions,
    exportFormatOptions,
    handleExportFormatChange,
    handleExportSizeChange,
    handleCustomExportCountChange,
    onDownloadClick,
    warning,
    customExportCount,
  } = props
  return (
    <Root>
      <Enum
        options={exportSizeOptions}
        value={exportSize}
        label="Export"
        onChange={handleExportSizeChange}
      />
      {exportSize === 'custom' ? (
        <div>
          <Number
            label=""
            showLabel={false}
            placeholder="Enter number of results you would like to export"
            name="customExport"
            value={customExportCount.toString()}
            onChange={handleCustomExportCountChange}
          />
        </div>
      ) : (
        <div />
      )}
      <Enum
        options={exportFormatOptions}
        value={exportFormat}
        label="as"
        onChange={handleExportFormatChange}
      />
      {warning && (
        <div className="warning">
          <i className="fa fa-warning" />
          <span>{warning}</span>
        </div>
      )}
      <Button
        buttonType={buttonTypeEnum.primary}
        icon="fa fa-download"
        text="Download"
        disabled={
          exportSize === 'custom' &&
          customExportCount > properties.exportResultLimit
        }
        onClick={onDownloadClick}
      />
    </Root>
  )
})
