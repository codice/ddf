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

const DEFAULT_AUTO_MERGE_TIME = 1000

const $ = require('jquery')
const _ = require('underscore')

function match(regexList, attribute) {
  return (
    _.chain(regexList)
      .map(str => new RegExp(str))
      .find(regex => regex.exec(attribute))
      .value() !== undefined
  )
}

const properties = {
  commitHash: __COMMIT_HASH__,
  isDirty: __IS_DIRTY__,
  commitDate: __COMMIT_DATE__,
  canvasThumbnailScaleFactor: 10,
  slidingAnimationDuration: 150,

  defaultFlytoHeight: 15000.0,

  CQL_DATE_FORMAT: 'YYYY-MM-DD[T]HH:mm:ss[Z]',

  ui: {},

  filters: {
    METADATA_CONTENT_TYPE: 'metadata-content-type',
    SOURCE_ID: 'source-id',
    GEO_FIELD_NAME: 'anyGeo',
    ANY_GEO: 'geometry',
    ANY_TEXT: 'anyText',
    OPERATIONS: {
      string: ['contains', 'matchcase', 'equals'],
      xml: ['contains', 'matchcase', 'equals'],
      date: ['before', 'after'],
      number: ['=', '>', '>=', '<', '<='],
      geometry: ['intersects'],
    },
    numberTypes: ['float', 'short', 'long', 'double', 'integer'],
  },

  init() {
    // use this function to initialize variables that rely on others
    let props = this
    $.ajax({
      async: false, // must be synchronous to guarantee that no tests are run before fixture is loaded
      cache: false,
      dataType: 'json',
      url: './internal/config',
    })
      .done(data => {
        props = _.extend(props, data)

        $.ajax({
          async: false, // must be synchronous to guarantee that no tests are run before fixture is loaded
          cache: false,
          dataType: 'json',
          url: './internal/platform/config/ui',
        })
          .done(uiConfig => {
            props.ui = uiConfig
            return props
          })
          .fail((jqXHR, status, errorThrown) => {
            if (console) {
              console.log(
                'Platform UI Configuration could not be loaded: (status: ' +
                  status +
                  ', message: ' +
                  errorThrown.message +
                  ')'
              )
            }
          })
      })
      .fail((jqXHR, status, errorThrown) => {
        throw new Error(
          'Configuration could not be loaded: (status: ' +
            status +
            ', message: ' +
            errorThrown.message +
            ')'
        )
      })

    this.handleEditing()
    this.handleFeedback()
    this.handleExperimental()
    this.handleUpload()
    this.handleListTemplates()

    return props
  },
  handleListTemplates() {
    try {
      this.listTemplates = this.listTemplates.map(JSON.parse)
    } catch (error) {
      /*
                  would be a good to start reporting errors like this to a log that can alert admins
                  or update the admin interface to include validation that prevents errors like this
                  ideally both
              */
      this.listTemplates = []
    }
  },
  handleEditing() {
    $('html').toggleClass('is-editing-restricted', this.isEditingRestricted())
  },
  handleFeedback() {
    $('html').toggleClass('is-feedback-restricted', this.isFeedbackRestricted())
  },
  handleExperimental() {
    $('html').toggleClass('is-experimental', this.hasExperimentalEnabled())
  },
  handleUpload() {
    $('html').toggleClass('is-upload-enabled', this.isUploadEnabled())
  },
  isHidden(attribute) {
    return match(this.hiddenAttributes, attribute)
  },
  isReadOnly(attribute) {
    return match(this.readOnly, attribute)
  },
  isEditingRestricted() {
    return !this.isEditingAllowed
  },
  hasExperimentalEnabled() {
    return this.isExperimental
  },
  getAutoMergeTime() {
    return this.autoMergeTime || DEFAULT_AUTO_MERGE_TIME
  },
  isFeedbackRestricted() {
    return !this.queryFeedbackEnabled
  },
  isDisableLocalCatalog() {
    return this.disableLocalCatalog
  },
  isHistoricalSearchEnabled() {
    return !this.isHistoricalSearchDisabled
  },
  isArchiveSearchEnabled() {
    return !this.isArchiveSearchDisabled
  },
  isUploadEnabled() {
    return this.showIngest
  },
  isDevelopment() {
    return process.env.NODE_ENV !== 'production'
  },
  isSpellcheckEnabled() {
    return this.isSpellcheckEnabled
  },
  isPhoneticsEnabled() {
    return this.isPhoneticsEnabled
  },
  isMetacardPreviewEnabled() {
    return !this.isMetacardPreviewDisabled
  },
}

module.exports = properties
