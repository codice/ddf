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
import styled from '../../styles/styled-components'
import { CustomElement } from '../../styles/mixins'
import { ChangeBackground } from '../../styles/mixins'
import { transparentize, readableColor } from 'polished'
import Text from '../../container/input-wrappers/text'

type Props = {
  startAdhocSearch: () => void
  onChange: () => void
  value: string
  placeholder: string
}

const Root = styled.div`
    ${CustomElement}
    ${props => ChangeBackground(props.theme.backgroundAccentContent)}
    height: calc(2.5 * ${props => props.theme.minimumButtonSize} + ${props =>
  props.theme.minimumSpacing});
    overflow: hidden;
    transition: height ${props => props.theme.coreTransitionTime} ease-in-out;
    border-bottom: 1px solid ${props =>
      transparentize(0.9, readableColor(props.theme.backgroundAccentContent))};

    .home-templates-adhoc {
        display: flex;
        justify-content: center;
        align-items: center;
        flex-wrap: nowrap;

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
            padding: 0px;
            intrigue-input { /* stylelint-disable-line */ 
              height: ${props => props.theme.minimumButtonSize};
            }
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

    ${props => {
      if (props.theme.screenBelow(props.theme.smallScreenSize)) {
        return `
                .home-templates-center {
                    max-width: 100%;
                }
                .home-templates-adhoc {
                    padding-right: 0px;
                    padding-left: 0px;
                }
                .home-templates-center {
                    padding: 0px 20px;
                }
            `
      }
    }}
`

const WorkspacesTemplates = (props: Props) => {
  return (
    <Root>
      <div className="home-templates-center">
        <div className="home-templates-header">
          <span className="home-templates-header-item home-templates-header-hint">
            Start a new workspace
          </span>
        </div>
        <div className="home-templates-adhoc">
          <div className="adhoc-search">
            <Text
              value={props.value}
              label=""
              showLabel={false}
              showValidationIssues={false}
              placeholder={props.placeholder}
              onChange={props.onChange}
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
      </div>
    </Root>
  )
}

export default WorkspacesTemplates
