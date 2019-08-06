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
import React from 'react'
import styled from '../../../react-component/styles/styled-components'
import { readableColor, darken, lighten } from 'polished'
import MarionetteRegionContainer from '../../../react-component/container/marionette-region-container'
const TabsModel = require('../../../component/tabs/tabs.js')
const TabsView = require('../../../component/tabs/tabs.view.js')
const GuideView = require('../guide/guide.view.js')
const AboutView = require('../about/about.view.js')
const VideosView = require('../videos/videos.view.js')

const Root = styled.div`
    height: 100%;

    .content {
        height: 100%;
    }

    .section {
        padding: ${props => props.theme.minimumSpacing};
    }
    .is-header {
        text-align: left;
        padding: ${props => props.theme.minimumSpacing} 0px;
    }
    .example,
    .instance {
        margin: ${props => props.theme.minimumSpacing};
    }
    .example .title {
        font-size: ${props => props.theme.mediumFontSize}
        padding: ${props => props.theme.minimumSpacing} 0px;
    }
    .limit-to-center {
        max-width: 800px;
        margin: auto;
        padding: ${props => props.theme.minimumSpacing};
    }
    .pad-bottom {
        padding-bottom: 30%;
    }
    .editor + .editor {
        margin-top: ${props => props.theme.minimumSpacing};
    }

    .editor[data-html]::before,
    .editor[data-js]::before,
    .editor[data-css]::before {
        padding: 0px ${props => props.theme.minimumSpacing};
        opacity: ${props => props.theme.minimumOpacity};
    }

    .editor[data-html]::before {
        content: 'HTML';
    }

    .editor[data-js]::before {
        content: 'Javascript';
    }

    .editor[data-css]::before {
        content: 'CSS';
    }
`

const Dev = props => {
  return (
    <Root {...props}>
      <MarionetteRegionContainer
        className="content"
        view={TabsView}
        viewOptions={() => {
          return {
            model: new TabsModel({
              tabs: {
                About: AboutView,
                Guide: GuideView,
                Videos: VideosView,
              },
            }),
          }
        }}
      />
    </Root>
  )
}

export default Dev
