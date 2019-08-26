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

const Marionette = require('marionette')
const IngestView = require('../ingest/ingest.view')
const BuilderView = require('../builder/builder.view')
import AttributeEditor from '../tabs/list-add/attribute-editor'
import { BuilderStart } from '../builder/builder-start'
import { InformalProductsTable } from '../../react-component/informal-products/informal-upload-table'
import React from 'react'
import MarionetteRegionContainer from '../../react-component/marionette-region-container'
import styled from 'styled-components'

const ItemCreationView = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  margin: ${props => props.theme.minimumSpacing};
  height: calc(100% - 1.8rem);
`
const UploadView = styled.div`
  width: 60%;
  height: calc(100% - 1.8rem);
`

const OrContainer = styled.div`
  align-items: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 6%;
`

const StyleLine = styled.div`
  align-self: center;
  width: 1px;
  height: 75px;
  box-shadow: 0px 0px 0px 1px;
`

const ManualView = styled.div`
  align-self: center;
  width: 30%;
`

function NewItem(props) {
  return (
    <React.Fragment>
      <ItemCreationView>
        <UploadView>
          <MarionetteRegionContainer
            className="upload-menu"
            view={IngestView}
            viewOptions={{
              handleUploadSuccess: props.handleUploadSuccess,
              url: props.url,
              extraHeaders: props.extraHeaders,
            }}
          />
        </UploadView>
        <OrContainer>
          <StyleLine />
          <div>OR</div>
          <StyleLine />
        </OrContainer>

        <ManualView>
          <BuilderStart onManualSubmit={props.onManualSubmit} />
        </ManualView>
      </ItemCreationView>
    </React.Fragment>
  )
}

export { NewItem }
