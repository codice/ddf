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
import { FormattedMessage } from 'react-intl'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
const SaveView = require('../../component/save/workspaces/workspaces-save.view')
const UnsavedIndicatorView = require('../../component/unsaved-indicator/workspaces/workspaces-unsaved-indicator.view')
import styled from '../../react-component/styles/styled-components'
import {
  Button,
  buttonTypeEnum,
} from '../../react-component/presentation/button'

export type Props = {
  hasUnavailableSources: boolean
  isSaved: boolean
  branding: string
  product: string
  recentWorkspace?: any
  recentMetacard?: any
  uploadEnabled: boolean
  isDevelopment: boolean
  navigateToRoute: (route: string) => void
}

export const Divider = () => {
  return <div className="is-divider" />
}

export const Root = styled<Props, 'div'>('div')`
  height: 100%;
  width: 100%;
  overflow: auto;
`

export const WorkspacesSave = styled<
  {
    isSaved: Props['isSaved']
  },
  'div'
>('div')`
  position: absolute;
  right: 0px;
  top: 0px;
  height: auto;
  overflow: hidden;
  max-width: ${props => (props.isSaved ? '0%' : '100%')};
  font-size: ${props => props.theme.largeFontSize};
  line-height: ${props => props.theme.minimumButtonSize};
  ${props =>
    props.isSaved
      ? `
  transition: max-width 0s linear
  ${props.theme.multiple(10, props.theme.coreTransitionTime, 's')};
  opacity: 1;
  `
      : ''} > * {
    padding: ${props => props.theme.largeSpacing};
  }
`

export const WorkspacesIndicator = styled.div`
  padding-left: ${props => props.theme.minimumSpacing};
  display: inline-block;
`

export const SourcesIndicator = styled<
  {
    hasUnavailableSources: Props['hasUnavailableSources']
  },
  'span'
>('span')`
  opacity: ${props => (props.hasUnavailableSources ? '1' : '0')};
  transform: ${props =>
    props.hasUnavailableSources ? 'scale(1)' : 'scale(0)'};
  color: ${props => props.theme.warningColor};
  padding: 0px ${props => props.theme.minimumSpacing};
  transition: transform ${props => props.theme.coreTransitionTime} ease-out,
    opacity ${props => props.theme.coreTransitionTime} ease-out;
`

export const LinkBase = styled(Button)`
  display: block;
  padding: ${props => props.theme.largeSpacing};
  height: auto;
  text-align: left;
  width: 100%;
  word-wrap: normal;
  overflow: hidden;
  text-overflow: ellipsis;
  position: relative;

  > span:first-of-type {
    min-width: ${props => props.theme.minimumButtonSize};
    text-align: center;
  }

  .fa {
    padding-right: ${props => props.theme.minimumSpacing};
  }

  .dynamic-text {
    display: inline-block;
    vertical-align: top;
    width: calc(
      100% - ${props => props.theme.minimumButtonSize} -
        ${props => props.theme.minimumSpacing}
    );
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
`

export const Link = ({
  onClick,
  children,
  title,
}: {
  onClick: React.HTMLProps<HTMLButtonElement>['onClick']
  children: any
  title?: React.HTMLProps<HTMLButtonElement>['title']
}) => {
  return (
    <LinkBase
      fadeUntilHover
      buttonType={buttonTypeEnum.neutral}
      onClick={onClick}
      title={title}
    >
      {children}
    </LinkBase>
  )
}

export const ProductLink = ({ navigateToRoute, branding, product }: Props) => {
  return (
    <Link
      onClick={() => {
        navigateToRoute('workspaces')
      }}
    >
      <span className="is-bold">{branding} </span>
      <span className="">{product}</span>
    </Link>
  )
}

export const WorkspaceLink = ({
  navigateToRoute,
  isSaved,
}: {
  navigateToRoute: Props['navigateToRoute']
  isSaved: Props['isSaved']
}) => {
  return (
    <Link
      onClick={() => {
        navigateToRoute('workspaces')
      }}
    >
      <span className="fa fa-book" />
      <span>Workspaces</span>
      <WorkspacesIndicator>
        <MarionetteRegionContainer view={UnsavedIndicatorView} replaceElement />
      </WorkspacesIndicator>
      <WorkspacesSave
        isSaved={isSaved}
        onClick={(e: any) => {
          e.stopPropagation()
        }}
      >
        <MarionetteRegionContainer view={SaveView} replaceElement />
      </WorkspacesSave>
    </Link>
  )
}

export const UploadLink = ({
  navigateToRoute,
  uploadEnabled,
}: {
  navigateToRoute: Props['navigateToRoute']
  uploadEnabled: Props['uploadEnabled']
}) => {
  if (uploadEnabled) {
    return (
      <Link
        onClick={() => {
          navigateToRoute('ingest')
        }}
      >
        <span className="fa fa-upload" />
        <span>Upload</span>
      </Link>
    )
  }
  return null
}

export const SourcesLink = ({
  navigateToRoute,
  hasUnavailableSources,
}: {
  navigateToRoute: Props['navigateToRoute']
  hasUnavailableSources: Props['hasUnavailableSources']
}) => {
  return (
    <Link
      onClick={() => {
        navigateToRoute('sources')
      }}
    >
      <span className="fa fa-cloud" />
      <FormattedMessage id="sources.title" defaultMessage="Sources" />
      <SourcesIndicator
        hasUnavailableSources={hasUnavailableSources}
        className="fa fa-bolt"
      />
    </Link>
  )
}

export const SearchFormsLink = ({
  navigateToRoute,
}: {
  navigateToRoute: Props['navigateToRoute']
}) => {
  return (
    <Link
      onClick={() => {
        navigateToRoute('forms')
      }}
    >
      <span className="fa cf cf-search-forms" />
      <span>Search Forms</span>
      <div className="forms-indicator" />
    </Link>
  )
}

export const ResultFormsLink = ({
  navigateToRoute,
}: {
  navigateToRoute: Props['navigateToRoute']
}) => {
  return (
    <Link
      onClick={() => {
        navigateToRoute('resultForms')
      }}
    >
      <span className="fa cf cf-result-forms" />
      <span>Result Forms</span>
      <div className="forms-indicator" />
    </Link>
  )
}

export const UpperNavigationLinks = ({
  navigateToRoute,
  uploadEnabled,
  hasUnavailableSources,
  isSaved,
}: Props) => {
  return (
    <>
      <WorkspaceLink navigateToRoute={navigateToRoute} isSaved={isSaved} />
      <UploadLink
        navigateToRoute={navigateToRoute}
        uploadEnabled={uploadEnabled}
      />
      <SourcesLink
        navigateToRoute={navigateToRoute}
        hasUnavailableSources={hasUnavailableSources}
      />
      <SearchFormsLink navigateToRoute={navigateToRoute} />
      <ResultFormsLink navigateToRoute={navigateToRoute} />
    </>
  )
}

export const RecentWorkspaceLink = ({
  recentWorkspace,
  navigateToRoute,
}: {
  recentWorkspace: Props['recentWorkspace']
  navigateToRoute: Props['navigateToRoute']
}) => {
  if (recentWorkspace) {
    return (
      <Link
        title={`Most Recent Workspace: ${recentWorkspace.title}`}
        onClick={() => {
          navigateToRoute(`workspaces/${recentWorkspace.id}`)
        }}
      >
        <div>Most Recent Workspace</div>
        <span className="fa fa-history" />
        <span className="dynamic-text">{recentWorkspace.title}</span>
      </Link>
    )
  }
  return null
}

export const RecentMetacardLink = ({
  recentMetacard,
  navigateToRoute,
}: {
  recentMetacard: Props['recentMetacard']
  navigateToRoute: Props['navigateToRoute']
}) => {
  if (recentMetacard) {
    return (
      <Link
        title={`Most Recent Metacard: ${
          recentMetacard.metacard.properties.title
        }`}
        onClick={() => {
          navigateToRoute(`metacards/${recentMetacard.metacard.id}`)
        }}
      >
        <div className="">Most Recent Metacard</div>
        <span className="fa fa-history" />
        <span className="dynamic-text">
          {recentMetacard.metacard.properties.title}
        </span>
      </Link>
    )
  }
  return null
}

export const RecentLinks = ({
  recentMetacard,
  recentWorkspace,
  navigateToRoute,
}: Props) => {
  return (
    <>
      {(recentMetacard || recentWorkspace) && (
        <>
          <RecentWorkspaceLink
            recentWorkspace={recentWorkspace}
            navigateToRoute={navigateToRoute}
          />
          <RecentMetacardLink
            recentMetacard={recentMetacard}
            navigateToRoute={navigateToRoute}
          />
        </>
      )}
    </>
  )
}

export const AboutLink = ({
  navigateToRoute,
}: {
  navigateToRoute: Props['navigateToRoute']
}) => {
  return (
    <Link
      onClick={() => {
        navigateToRoute('about')
      }}
    >
      <span className="fa fa-info" />
      <span>About</span>
    </Link>
  )
}
export const DevelopmentLink = ({
  navigateToRoute,
  isDevelopment,
}: {
  navigateToRoute: Props['navigateToRoute']
  isDevelopment: Props['isDevelopment']
}) => {
  if (isDevelopment) {
    return (
      <Link
        onClick={() => {
          navigateToRoute('_dev')
        }}
      >
        <span className="fa fa-user-md" />
        <span>Developer</span>
      </Link>
    )
  }
  return null
}

export const HomeLink = ({
  navigateToRoute,
  branding,
}: {
  navigateToRoute: Props['navigateToRoute']
  branding: Props['branding']
}) => {
  return (
    <Link
      onClick={() => {
        navigateToRoute('_home')
      }}
    >
      <span className="fa fa-home" />
      <span>{branding} Home</span>
    </Link>
  )
}

export const LowerNavigationLinks = ({
  navigateToRoute,
  isDevelopment,
  branding,
}: Props) => {
  return (
    <>
      <AboutLink navigateToRoute={navigateToRoute} />
      <DevelopmentLink
        navigateToRoute={navigateToRoute}
        isDevelopment={isDevelopment}
      />
      <Divider />
      <HomeLink branding={branding} navigateToRoute={navigateToRoute} />
    </>
  )
}
