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
const BuilderStart = require('../builder/builder-start.view')

import React from 'react'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import styled from '../../react-component/styles/styled-components'


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
  box-shadow: 0px 0px 1px 1px;
`

const ManualView = styled.div`
  align-self: center;
  overflow-y: scroll;
  width: 30%;
`

module.exports = Marionette.LayoutView.extend({
  template() {
    return (
      <React.Fragment>
        <ItemCreationView>
          <UploadView>
            <MarionetteRegionContainer
              className="upload-menu"
              view={IngestView}
            />
          </UploadView>
          <OrContainer>
            <StyleLine />
            <div>OR</div>
            <StyleLine />
          </OrContainer>

          <ManualView>
            <BuilderStart/>
            {/* <MarionetteRegionContainer
              className="manual-menu"
              view={
                new BuilderView({
                  handleNewMetacard: this.options.handleNewMetacard,
                  close: this.options.close,
                  model: this.model,
                })
              }
            /> */}
          </ManualView>
        </ItemCreationView>
      </React.Fragment>
    )
  },
})
