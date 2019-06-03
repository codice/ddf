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
const CustomElements = require('../../js/CustomElements.js')
const wreqr = require('../../js/wreqr.js')
const properties = require('../../js/properties.js')
const store = require('../../js/store.js')
const metacard = require('../metacard/metacard.js')
const SaveView = require('../save/workspaces/workspaces-save.view.js')
const UnsavedIndicatorView = require('../unsaved-indicator/workspaces/workspaces-unsaved-indicator.view.js')
const sources = require('../singletons/sources-instance.js')
const plugin = require('plugins/navigator')
import ExtensionPoints from '../../extension-points'
const $ = require('jquery')
import * as React from 'react'
import { FormattedMessage } from 'react-intl'

const visitFragment = fragment =>
  wreqr.vent.trigger('router:navigate', {
    fragment,
    options: {
      trigger: true,
    },
  })

module.exports = plugin(
  Marionette.LayoutView.extend({
    template(props) {
      return (
        <React.Fragment>
          <button
            className="navigation-choice is-neutral choice-product is-button"
            data-fragment="workspaces"
          >
            <span className="is-bold">{props.properties.branding} </span>
            <span className="">{props.properties.product}</span>
          </button>
          <div className="is-divider" />
          <div className="navigation-links">
            <button
              className="navigation-choice is-neutral choice-workspaces is-button"
              data-fragment="workspaces"
            >
              <span className="fa fa-book" />
              <span>Workspaces</span>
              <div className="workspaces-indicator" />
            </button>
            <div className="workspaces-save" />
            <button
              className="navigation-choice is-neutral choice-upload is-button"
              data-fragment="ingest"
            >
              <span className="fa fa-upload" />
              <span>Upload</span>
            </button>
            <button
              className="navigation-choice is-neutral choice-sources is-button"
              data-fragment="sources"
            >
              <span className="fa fa-cloud" />
              <FormattedMessage id="sources.title" defaultMessage="Sources" />
              <span className="sources-indicator fa fa-bolt" />
            </button>
            <button
              className="navigation-choice is-neutral choice-search-forms is-button"
              data-fragment="forms"
            >
              <span className="fa cf cf-search-forms" />
              <span>Search Forms</span>
              <div className="forms-indicator" />
            </button>
            <button
              className="navigation-choice is-neutral choice-result-forms is-button"
              data-fragment="resultForms"
            >
              <span className="fa cf cf-result-forms" />
              <span>Result Forms</span>
              <div className="forms-indicator" />
            </button>
            <div className="navigation-extensions" />
            {ExtensionPoints.navigator}
          </div>
          {props.recent && (
            <React.Fragment>
              <div className="is-divider" />
              <div className="navigation-links">
                {props.workspace && (
                  <button
                    className="navigation-choice is-neutral choice-previous-workspace is-button"
                    title={`Most Recent Workspace: ${props.workspace.title}`}
                    data-fragment={`workspaces/${props.workspace.id}`}
                  >
                    <div>Most Recent Workspace</div>
                    <span className="fa fa-history" />
                    <span className="dynamic-text">
                      {props.workspace.title}
                    </span>
                  </button>
                )}
                {props.metacard ? (
                  <button
                    className="navigation-choice is-neutral choice-previous-metacard is-button"
                    title={`Most Recent Metacard: ${
                      props.metacard.metacard.properties.title
                    }`}
                    data-fragment={`metacards/${props.metacard.metacard.id}`}
                  >
                    <div className="">Most Recent Metacard</div>
                    <span className="fa fa-history" />
                    <span className="dynamic-text">
                      {props.metacard.metacard.properties.title}
                    </span>
                  </button>
                ) : null}
              </div>
            </React.Fragment>
          )}
          <div className="is-divider" />
          <div className="navigation-links">
            <button
              className="navigation-choice is-neutral choice-about is-button"
              data-fragment="about"
            >
              <span className="fa fa-info" />
              <span>About</span>
            </button>
            <button
              className="navigation-choice is-neutral choice-dev is-button"
              data-fragment="_dev"
            >
              <span className="fa fa-user-md" />
              <span>Developer</span>
            </button>
            <div className="is-divider" />
            <button
              className="navigation-choice is-neutral choice-home is-button"
              data-fragment="_home"
            >
              <span className="fa fa-home" />
              <span>{props.properties.branding} Home</span>
            </button>
          </div>
        </React.Fragment>
      )
    },
    tagName: CustomElements.register('navigator'),
    regions: {
      workspacesIndicator: '.workspaces-indicator',
      workspacesSave: '.workspaces-save',
      extensions: '.navigation-extensions',
    },
    events: {
      'click .navigation-choice': 'handleChoice',
    },
    initialize() {
      this.listenTo(
        store.get('workspaces'),
        'change:saved update add remove',
        this.handleSaved
      )
      this.listenTo(sources, 'all', this.handleSourcesChange)
      this.handleSaved()
      this.handleSourcesChange()
    },
    onBeforeShow() {
      this.workspacesSave.show(new SaveView())
      this.workspacesIndicator.show(new UnsavedIndicatorView())
      const extensions = this.getExtensions()
      if (extensions) {
        this.extensions.show(extensions)
      }
    },
    getExtensions() {},
    handleSaved() {
      const hasUnsaved = store
        .get('workspaces')
        .find(workspace => !workspace.isSaved())
      this.$el.toggleClass('is-saved', !hasUnsaved)
    },
    handleSourcesChange() {
      const hasDown = sources.some(source => !source.get('available'))
      this.$el.toggleClass('has-unavailable', hasDown)
    },
    handleChoice(e) {
      const fragment = $(e.currentTarget).attr('data-fragment')
      if (fragment === '_home') {
        window.location = '/'
      } else {
        visitFragment(fragment)
        this.closeSlideout()
      }
    },
    closeSlideout() {
      this.$el.trigger('closeSlideout.' + CustomElements.getNamespace())
    },
    serializeData() {
      const currentWorkspace = store.getCurrentWorkspace()
      let workspaceJSON
      if (currentWorkspace) {
        workspaceJSON = currentWorkspace.toJSON()
      }
      const currentMetacard = metacard.get('currentMetacard')
      let metacardJSON
      if (currentMetacard) {
        metacardJSON = currentMetacard.toJSON()
      }
      return {
        properties,
        workspace: workspaceJSON,
        metacard: metacardJSON,
        recent: workspaceJSON || metacardJSON,
      }
    },
  })
)
