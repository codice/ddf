import * as React from 'react'
import { FormattedMessage } from 'react-intl'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
const SaveView = require('../../component/save/workspaces/workspaces-save.view')
const UnsavedIndicatorView = require('../../component/unsaved-indicator/workspaces/workspaces-unsaved-indicator.view')
import styled from '../../react-component/styles/styled-components'
import { hot } from 'react-hot-loader'

type Props = {
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

const Root = styled<Props, 'div'>('div')`
  height: 100%;
  width: 100%;
  overflow: auto;

  button.navigation-choice,
  .navigation-choice {
    display: block;
    padding: ${props => props.theme.largeSpacing};
    height: auto;
    text-align: left;
    width: 100%;
    word-wrap: normal;
    overflow: hidden;
    text-overflow: ellipsis;
    position: relative;
  }

  .navigation-choice > span:first-of-type {
    min-width: ${props => props.theme.minimumButtonSize};
    text-align: center;
  }

  .navigation-links {
    position: relative;
    .navigation-choice .fa {
      padding-right: ${props => props.theme.minimumSpacing};
    }
  }

  .choice-workspaces .workspaces-indicator {
    padding-left: ${props => props.theme.minimumSpacing};
    display: inline-block;
  }

  .workspaces-save {
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
        : ''}
  }
  /* stylelint-disable-next-line */
  .workspaces-save > intrigue-save {
    padding: ${props => props.theme.largeSpacing};
  }

  .sources-indicator {
    opacity: ${props => (props.hasUnavailableSources ? '1' : '0')};
    transform: ${props =>
      props.hasUnavailableSources ? 'scale(1)' : 'scale(0)'};
    color: ${props => props.theme.warningColor};
    padding: 0px ${props => props.theme.minimumSpacing};
    transition: transform ${props => props.theme.coreTransitionTime} ease-out,
      opacity ${props => props.theme.coreTransitionTime} ease-out;
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

export const ProductLink = ({ navigateToRoute, branding, product }: Props) => {
  return (
    <button
      className="navigation-choice is-neutral choice-product is-button"
      onClick={() => {
        navigateToRoute('workspaces')
      }}
    >
      <span className="is-bold">{branding} </span>
      <span className="">{product}</span>
    </button>
  )
}

export const UpperNavigationLinks = ({
  navigateToRoute,
  uploadEnabled,
}: Props) => {
  return (
    <div className="navigation-links">
      <button
        className="navigation-choice is-neutral choice-workspaces is-button"
        onClick={() => {
          navigateToRoute('workspaces')
        }}
      >
        <span className="fa fa-book" />
        <span>Workspaces</span>
        <div className="workspaces-indicator">
          <MarionetteRegionContainer
            view={UnsavedIndicatorView}
            replaceElement
          />
        </div>
      </button>
      <div className="workspaces-save">
        <MarionetteRegionContainer view={SaveView} replaceElement />
      </div>
      {uploadEnabled ? (
        <button
          className="navigation-choice is-neutral choice-upload is-button"
          onClick={() => {
            navigateToRoute('ingest')
          }}
        >
          <span className="fa fa-upload" />
          <span>Upload</span>
        </button>
      ) : (
        ''
      )}
      <button
        className="navigation-choice is-neutral choice-sources is-button"
        onClick={() => {
          navigateToRoute('sources')
        }}
      >
        <span className="fa fa-cloud" />
        <FormattedMessage id="sources.title" defaultMessage="Sources" />
        <span className="sources-indicator fa fa-bolt" />
      </button>
      <button
        className="navigation-choice is-neutral choice-search-forms is-button"
        onClick={() => {
          navigateToRoute('forms')
        }}
      >
        <span className="fa cf cf-search-forms" />
        <span>Search Forms</span>
        <div className="forms-indicator" />
      </button>
      <button
        className="navigation-choice is-neutral choice-result-forms is-button"
        onClick={() => {
          navigateToRoute('resultForms')
        }}
      >
        <span className="fa cf cf-result-forms" />
        <span>Result Forms</span>
        <div className="forms-indicator" />
      </button>
      <div className="navigation-extensions" />
    </div>
  )
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
          <div className="navigation-links">
            {recentWorkspace && (
              <button
                className="navigation-choice is-neutral choice-previous-workspace is-button"
                title={`Most Recent Workspace: ${recentWorkspace.title}`}
                onClick={() => {
                  navigateToRoute(`workspaces/${recentWorkspace.id}`)
                }}
              >
                <div>Most Recent Workspace</div>
                <span className="fa fa-history" />
                <span className="dynamic-text">{recentWorkspace.title}</span>
              </button>
            )}
            {recentMetacard ? (
              <button
                className="navigation-choice is-neutral choice-previous-metacard is-button"
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
              </button>
            ) : null}
          </div>
        </>
      )}{' '}
    </>
  )
}

export const LowerNavigationLinks = ({
  navigateToRoute,
  isDevelopment,
  branding,
}: Props) => {
  return (
    <div className="navigation-links">
      <button
        className="navigation-choice is-neutral choice-about is-button"
        onClick={() => {
          navigateToRoute('about')
        }}
      >
        <span className="fa fa-info" />
        <span>About</span>
      </button>
      {isDevelopment ? (
        <button
          className="navigation-choice is-neutral choice-dev is-button"
          onClick={() => {
            navigateToRoute('_dev')
          }}
        >
          <span className="fa fa-user-md" />
          <span>Developer</span>
        </button>
      ) : (
        ''
      )}
      <div className="is-divider" />
      <button
        className="navigation-choice is-neutral choice-home is-button"
        onClick={() => {
          navigateToRoute('_home')
        }}
      >
        <span className="fa fa-home" />
        <span>{branding} Home</span>
      </button>
    </div>
  )
}

const Divider = () => {
  return <div className="is-divider" />
}

const navigation = (props: Props) => {
  const { recentMetacard, recentWorkspace } = props
  return (
    <Root {...props}>
      <ProductLink {...props} />
      <Divider />

      <UpperNavigationLinks {...props} />
      {recentMetacard || recentWorkspace ? <Divider /> : ''}
      <RecentLinks {...props} />
      <Divider />
      <LowerNavigationLinks {...props} />
    </Root>
  )
}

export default hot(module)(navigation)
