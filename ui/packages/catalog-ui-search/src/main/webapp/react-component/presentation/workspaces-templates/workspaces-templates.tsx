/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import * as React from 'react'
import styled from '../../styles/styled-components'
import { CustomElement } from '../../styles/mixins'
import { ChangeBackground } from '../../styles/mixins'
import { transparentize, readableColor } from 'polished'
import MarionetteRegionContainer from '../../container/marionette-region-container'
import WorkspaceTemplate from '../workspace-template'
const PropertyView = require('component/property/property.view')

type RootProps = {
  hasTemplatesExpanded: boolean
}

type Props = {
  startAdhocSearch: () => void
  toggleExpansion: () => void
  adhocModel: Backbone.Model
  createWorkspace: () => void
  createLocalWorkspace: () => void
  createAllWorkspace: () => void
  createGeoWorkspace: () => void
  createLatestWorkspace: () => void
}

const Root = styled<RootProps, 'div'>('div')`
    ${CustomElement}
    ${props => ChangeBackground(props.theme.backgroundAccentContent)}
    height: calc(2.5 * ${props => props.theme.minimumButtonSize} + ${props =>
  props.theme.minimumSpacing});
    overflow: hidden;
    transition: height ${props => props.theme.coreTransitionTime} ease-in-out;
    border-bottom: 1px solid ${props =>
      transparentize(0.9, readableColor(props.theme.backgroundAccentContent))};

    .home-templates-adhoc {
        text-align: center;
        padding: ${props => props.theme.minimumSpacing} 0px ${props =>
  props.theme.minimumSpacing} 0px;
        white-space: nowrap;

        .adhoc-search {
            display: inline-block;
            vertical-align: middle;
            width: calc(100% - ${props =>
              props.theme.minimumButtonSize} - 2 * ${props =>
  props.theme.mediumSpacing});
        }
        
        intrigue-property { /* stylelint-disable-line */
            input {
                text-align: left;
            }
            padding: ${props => props.theme.minimumSpacing} 0px ${props =>
  props.theme.minimumSpacing} 0px;
        }

        .adhoc-go {
        vertical-align: middle;
        width: calc(${props => props.theme.minimumSpacing} + 2 * ${props =>
  props.theme.mediumSpacing});
        max-width: 100%;
        }
    }

    .home-templates-center {
        margin: auto;
        max-width: 1200px;
        padding: 0px 100px;
    }

    .home-templates-header {
        font-size: ${props => props.theme.minimumFontSize};
        font-weight: bolder;
        line-height: ${props => props.theme.minimumButtonSize};
    }

    .home-templates-header-item {
        display: inline-block;
    }

    .home-templates-header-button {
        float: right;
        opacity: ${props => props.theme.minimumOpacity};
    }

    .home-templates-header-button:hover {
        opacity: 1;
    }

    .home-templates-choices {
        text-align: center;
    }

    .home-templates-choices-choice {
        display: inline-block;
        width: 9rem;
        margin-bottom: ${props => props.theme.minimumSpacing};
        margin-right: ${props => props.theme.largeSpacing};
        cursor: pointer;
    }

    .home-templates-choices-choice-preview {
        line-height: calc(2 * ${props => props.theme.minimumButtonSize});
        height: calc(2.5 * ${props => props.theme.minimumButtonSize});
        text-align: center;
        width: 100%;
        background: ${props =>
          transparentize(
            0.9,
            readableColor(props.theme.backgroundAccentContent)
          )};
        color: ${props => props.theme.primaryColor};
        font-size: calc(3 * ${props => props.theme.largeFontSize});
        cursor: pointer;
        position: relative;
    }

    .home-templates-choices-choice-preview-icon {
        line-height: calc(2.5 * ${props => props.theme.minimumButtonSize});
    }

    .home-templates-choices-choice-description {
        font-weight: bolder;
        text-align: center;
    }

    .home-templates-expanded {
        background: ${props => props.theme.backgroundAccentContent};
        position: absolute;
        top: 0px;
        left: 0px;
        width: 100%;
        height: calc(2 * ${props => props.theme.minimumLineSize});
        opacity: 0;
        transition: opacity ${props =>
          props.theme.coreTransitionTime} ease-in-out;
        transform: translateY(-200%);
        white-space: nowrap;
        line-height: calc(2 * ${props => props.theme.minimumLineSize});
    }

    ${props => {
      if (props.hasTemplatesExpanded) {
        return `
                .home-templates-expanded {
                    transform: translateY(0%);
                    opacity: 1;
                }

                height: calc(${props.theme.minimumLineSize} + 2*${
          props.theme.minimumButtonSize
        } + 2*${props.theme.minimumSpacing} + 2*${
          props.theme.minimumSpacing
        } + 3*${props.theme.minimumButtonSize});
                overflow: auto;
            `
      }
    }}

    ${props => {
      if (props.theme.screenBelow(props.theme.smallScreenSize)) {
        return `
                .home-templates-center,
                .expanded-content{
                    max-width: 100%;
                }
                .home-templates-adhoc {
                    padding-right: 0px;
                    padding-left: 0px;
                }
                .home-templates-center {
                    padding: 0px 20px;
                }
                .expanded-content {
                    padding: 0px 0px;
                }

                .home-templates-choices {
                    text-align: center;
                }
            `
      }
    }}
`

const WorkspacesTemplates = (props: Props & RootProps) => {
  return (
    <Root hasTemplatesExpanded={props.hasTemplatesExpanded}>
      <div className="home-templates-center">
        <div className="home-templates-header">
          <span className="home-templates-header-item home-templates-header-hint">
            Start a new workspace
          </span>
          <div
            className="home-templates-header-item home-templates-header-button is-button"
            data-help="Click to see more choices for starting workspaces."
            onClick={props.toggleExpansion}
          >
            {!props.hasTemplatesExpanded ? (
              <div className="home-templates-header-button-closed">
                Show Templates
                <span className="fa fa-caret-down" />
              </div>
            ) : (
              <div className="home-templates-header-button-expanded">
                Hide Templates
                <span className="fa fa-caret-up" />
              </div>
            )}
          </div>
        </div>
        <div className="home-templates-adhoc">
          <div className="adhoc-search">
            <MarionetteRegionContainer
              view={PropertyView}
              viewOptions={() => {
                return {
                  model: props.adhocModel,
                }
              }}
              onKeyUp={event => {
                switch (event.keyCode) {
                  case 13:
                    props.startAdhocSearch()
                    break
                  default:
                    break
                }
              }}
            />
          </div>
          <button
            className="adhoc-go is-primary"
            onClick={props.startAdhocSearch}
          >
            <span className="fa fa-search" />
          </button>
        </div>
        <div className="home-templates-choices is-list has-list-highlighting is-inline">
          <WorkspaceTemplate
            onClick={props.createWorkspace}
            iconText="+"
            description="Blank"
            help="Click to create a new workspace from scratch."
          />
          <WorkspaceTemplate
            onClick={props.createLocalWorkspace}
            icon="fa fa-home"
            description="Local"
            help="Click to create a new workspace with an example of a local constrained search."
          />
          <WorkspaceTemplate
            onClick={props.createAllWorkspace}
            icon="fa fa-cloud"
            description="Federated"
            help="Click to create a new workspace with an example of a federated search."
          />
          <WorkspaceTemplate
            onClick={props.createGeoWorkspace}
            icon="fa fa-globe"
            description="Location"
            help="Click to create a new workspace with an example of a geographically constrained search."
          />
          <WorkspaceTemplate
            onClick={props.createLatestWorkspace}
            icon="fa fa-clock-o"
            description="Temporal"
            help="Click to create a new workspace with an example of a temporally constrained search."
          />
        </div>
      </div>
    </Root>
  )
}

export default WorkspacesTemplates
