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
import { hot } from 'react-hot-loader'
import WorkspaceInteractions from '../../container/workspace-interactions'
const QueryAddView = require('../../../component/query-add/query-add.view.js')
import MarionetteRegionContainer from '../../container/marionette-region-container'
import SaveButton from '../save-button'
import WorkspaceTitle from '../workspace-title'
import Dropdown from '../dropdown'
import NavigationBehavior from '../navigation-behavior'
import {
  buttonTypeEnum,
  Button,
} from '../../../react-component/presentation/button'

const { createAction } = require('imperio')

type Props = {
  currentWorkspace: Backbone.Model
  saved: boolean
  branding: string
  product: string
}

const StyledSaveButton = styled.div`
  display: block;
`

const StyledWorkspaceTitle = styled.div`
  display: block;
`

const StyledDropdown = styled(Dropdown)`
  height: 100%;
  line-height: inherit;
  min-height: 0px;
`

const Icon = styled.span`
  display: inline-block;
  text-align: right;
  width: ${({ theme }) => theme.minimumButtonSize};
`

const Root = styled.div`
  padding: ${({ theme }) => theme.minimumSpacing};
  line-height: calc(
    2 *
      (
        ${({ theme }) => theme.minimumLineSize} -
          ${({ theme }) => theme.minimumSpacing}
      )
  );
  width: 100%;
  overflow: hidden;
  position: relative;
  height: 100%;
  white-space: nowrap;
  display: flex;
  justify-content: flex-start;
  > .content-adhoc {
    flex-shrink: 20;
    min-width: ${({ theme }) => theme.minimumButtonSize};
    height: 100%;
    text-align: center;
  }
  > ${StyledWorkspaceTitle /* sc-selector*/} {
    text-overflow: ellipsis;
  }
`

const Grouping = styled.div`
  height: 100%;
  border: 1px solid ${({ theme }) => theme.primaryColor};
  white-space: nowrap;
  display: flex;
  justify-content: flex-start;
  position: relative;
  max-width: calc(
    100% - ${({ theme }) => theme.minimumButtonSize} -
      ${({ theme }) => theme.largeSpacing}
  );
  margin-right: ${({ theme }) => theme.largeSpacing};
  input {
    background: inherit;
  }
  > .content-interactions {
    min-width: ${({ theme }) => theme.minimumButtonSize};
    height: 100%;
    text-align: center;
  }
  > .content-interactions,
  > ${StyledWorkspaceTitle /* sc-selector*/}, > .content-adhoc,
  > ${StyledSaveButton /* sc-selector*/} {
    overflow: hidden;
    height: 100%;
  }
  > .content-interactions,
  > ${StyledSaveButton /* sc-selector */} {
    flex-shrink: 0;
  }
`

const AdhocButton = styled(Button)`
  height: 100%;
  padding-right: ${({ theme }) => theme.minimumSpacing};
  min-height: 0px;
  line-height: inherit;
`

const { withAction: withSearch } = createAction({
  type: 'workspace/query/TOGGLE-DROPDOWN',
  docs: 'Start a new search in the current workspace.',
})

const StartSearch = withSearch({
  fn: ({ props }: any) => {
    props.onClick()
  },
})(({ branding, product }: any) => {
  return (
    <AdhocButton buttonType={buttonTypeEnum.primary}>
      <Icon className="fa fa-search" /> Search {branding} {product}
    </AdhocButton>
  )
})

const { withAction: withToggle } = createAction({
  type: 'workspace/interactions/TOGGLE-INTERACTIONS',
  docs: 'Toggle the current workspace interactions.',
})

const Interactions = withToggle({
  fn: ({ props }: any) => {
    props.onClick()
  },
})(() => <span className="fa fa-ellipsis-v" />)

const { withAction: withSave } = createAction({
  type: 'workspace/SAVE-WORKSPACE',
  docs: 'Save the current workspace.',
})

const Save = withSave({
  fn: ({ props }: any) => {
    props.onClick()
  },
})(SaveButton)

const render = (props: Props) => {
  const { currentWorkspace, saved, branding, product } = props
  return (
    <Root>
      <Grouping className="grouping">
        <StyledWorkspaceTitle>
          <WorkspaceTitle
            title={currentWorkspace.get('title')}
            saved={saved}
            onChange={(title: string) => {
              currentWorkspace.set('title', title)
            }}
          />
        </StyledWorkspaceTitle>
        <StyledSaveButton>
          <Save
            isSaved={saved}
            onClick={() => {
              currentWorkspace.save()
            }}
          />
        </StyledSaveButton>
        <StyledDropdown
          className="content-interactions"
          content={() => (
            <NavigationBehavior>
              <WorkspaceInteractions workspace={currentWorkspace} />
            </NavigationBehavior>
          )}
        >
          <Interactions />
        </StyledDropdown>
      </Grouping>
      <StyledDropdown
        className="content-adhoc"
        content={() => (
          <MarionetteRegionContainer
            view={QueryAddView}
            viewOptions={() => {
              return {
                model: currentWorkspace,
              }
            }}
          />
        )}
      >
        <StartSearch branding={branding} product={product} />
      </StyledDropdown>
    </Root>
  )
}

export default hot(module)(render)
