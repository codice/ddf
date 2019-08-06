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
import WorkspacesTemplatesContainer from '../../container/workspaces-templates-container'
import WorkspacesItemsContainer from '../../container/workspaces-items-container'
import { Button, buttonTypeEnum } from '../button'

type Props = {
  saveAllWorkspaces: () => void
}

type RootProps = {
  hasUnsaved: boolean
}

const Root = styled<RootProps, 'div'>('div')`
  ${CustomElement} ${props =>
    ChangeBackground(
      props.theme.backgroundContent
    )}
    > .home-content,
    > .home-save {
    display: inline-block;
    width: 100%;
    vertical-align: top;
  }

  .home-content {
    max-height: 100%;
    overflow: auto;
  }

  .home-items {
    transition: padding ${props => props.theme.coreTransitionTime} ease-out
      ${props => props.theme.coreTransitionTime};
    padding-bottom: ${props => props.theme.minimumButtonSize};
  }

  .home-save {
    position: relative;
    left: 0px;
    opacity: 1;
    transform: scale(1) translateY(-100%);
    transition: transform ${props => props.theme.coreTransitionTime} ease-out,
      opacity ${props => props.theme.coreTransitionTime} ease-out,
      left 0s ease-out ${props => props.theme.coreTransitionTime};
  }

  ${props => {
    if (!props.hasUnsaved) {
      return `
                .home-items  {
                    padding-bottom: 0px;
                }

                .home-save {
                    transform: scale(2) translateY(-100%);
                    left: -200%;
                    opacity: 0;
                }
            `
    }
  }};
`

const Workspaces = (props: Props & RootProps) => {
  return (
    <Root hasUnsaved={props.hasUnsaved}>
      <div className="home-content">
        <div className="home-templates">
          <WorkspacesTemplatesContainer hasUnsaved={props.hasUnsaved} />
        </div>
        <div className="home-items">
          <WorkspacesItemsContainer />
        </div>
      </div>
      <Button
        buttonType={buttonTypeEnum.positive}
        icon="fa fa-floppy-o"
        text="Save all"
        className="home-save"
        onClick={props.saveAllWorkspaces}
      />
    </Root>
  )
}

export default Workspaces
