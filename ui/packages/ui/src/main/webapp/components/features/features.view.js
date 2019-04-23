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

define([
  'backbone.marionette',
  './features.hbs',
  'js/CustomElements',
  'components/feature-item/feature-item.collection.view',
  'js/models/features/feature',
  'underscore',
], function(
  Marionette,
  template,
  CustomElements,
  FeatureItemCollectionView,
  FeatureModel,
  _
) {
  return Marionette.Layout.extend({
    tagName: CustomElements.register('features'),
    template: template,
    regions: {
      collectionRegion: '.features',
    },
    events: {
      'change > .features-header select': 'updateFiltering',
      'keyup > .features-header input': 'updateFiltering',
    },
    updateFiltering: function() {
      this.collectionRegion.currentView.updateFilter({
        status: this.$el.find('> .features-header select').val(),
        name: this.$el.find('> .features-header input').val(),
      })
    },
    initialize: function(options) {
      this.updateFiltering = _.debounce(this.updateFiltering, 200)
      this.collection = new FeatureModel.Collection()
      this.listenTo(this.collection, 'selected', this.onFeatureAction)
      this.collection.fetch()
    },
    onRender: function() {
      this.collectionRegion.show(
        new FeatureItemCollectionView({
          collection: this.collection,
          showWarnings: true,
          filter: {
            status: this.$el.find('> .features-header select').val(),
            name: this.$el.find('> .features-header input').val(),
          },
        })
      )
    },
    focus: function() {
      this.$el.find('> .features-header input').focus()
    },
    getFeatureView: function(options) {
      if (options.collection && options.collection.length) {
        return new FeaturesView(options)
      }
      return new EmptyView.view({
        message:
          'No features are available for the "' +
          this.appName +
          '" application.',
      })
    },
    onFeatureAction: function(model) {
      const self = this;
      const status = model.get('status');
      const featureModel = new FeatureModel.Model({
        name: model.get('name'),
      });
      //TODO: add loading div...
      if (status === 'Uninstalled') {
        const install = featureModel.install();
        if (install) {
          install
            .done(function() {
              self.collection.fetch()
            })
            .fail(function() {
              if (console) {
                console.log(
                  'install failed for feature: ' +
                    featureModel.name +
                    ' app: ' +
                    self.appName
                )
              }
            })
        }
      } else {
        const uninstall = featureModel.uninstall();
        if (uninstall) {
          uninstall
            .done(function() {
              self.collection.fetch()
            })
            .fail(function() {
              if (console) {
                console.log(
                  'uninstall failed for feature: ' +
                    featureModel.name +
                    ' app: ' +
                    self.appName
                )
              }
            })
        }
      }
    },
  });
})
