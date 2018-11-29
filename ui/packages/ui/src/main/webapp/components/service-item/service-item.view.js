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
/*global define, window*/
import * as React from 'react'
import styled from '../../styles/styled-components'
import MarionetteRegionContainer from '../container/marionette-region-container'
define([
  'underscore',
  'marionette',
  'js/models/Service',
  'components/configuration-edit/configuration-edit.view',
  'js/wreqr.js',
  'js/CustomElements',
  'components/configuration-item/configuration-item.collection.view',
], function(
  _,
  Marionette,
  Service,
  ConfigurationEdit,
  wreqr,
  CustomElements,
  ConfigurationItemCollectionView
) {
  const Link = styled.a`
    text-decoration: none !important;
    width: 100%;
    font-weight: bold;
  `
  const ServiceDivider = styled.hr`
    border-bottom: solid 3px #eee;
    margin: 0;
  `
  return Marionette.Layout.extend({
    template: function({ name }) {
      const cid = this.cid
      return (
        <React.Fragment>
          <Link
            href={`#${cid}`}
            className="newLink"
            data-toggle="modal"
            data-backdrop="static"
            data-keyboard="false"
          >
            {name}
          </Link>
          <ServiceDivider />
          <MarionetteRegionContainer
            view={ConfigurationItemCollectionView}
            viewOptions={{
              collection: this.model.get('configurations'),
            }}
          />
          <div id="configurationRegion" />
          <div
            id={cid}
            className="service-modal modal"
            tabIndex="-1"
            role="dialog"
            aria-hidden="true"
          />
        </React.Fragment>
      )
    },
    tagName: CustomElements.register('service-item'),
    events: {
      'click .newLink': 'newConfiguration',
    },
    regions: {
      editModal: '.service-modal',
    },
    /**
     * If it is a factory OR it has no existing configurations, generate a configuration from the model.
     * Otherwise, use the existing configuration.
     * Then show an editing modal with the configuration.
     */
    newConfiguration: function() {
      var configuration
      var model = this.model
      var hasFactory = model.get('factory')
      var existingConfigurations = model.get('configurations')

      if (hasFactory || existingConfigurations.isEmpty()) {
        wreqr.vent.trigger('poller:stop')
        configuration = new Service.Configuration().initializeFromModel(model)
      } else {
        configuration = existingConfigurations.at(0)
      }

      this.editModal.show(
        new ConfigurationEdit.View({ model: configuration, service: model })
      )
    },

    serializeData: function() {
      return _.extend(this.model.toJSON(), {
        cid: this.cid,
      })
    },
  })
})
