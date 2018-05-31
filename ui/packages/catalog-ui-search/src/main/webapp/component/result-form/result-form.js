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
 /*global define,window*/

 define([
     'jquery',
     'backbone'
 ], function ($, Backbone) {

  let resultTemplates = [];
  let resultTemplateProperties = [];
  let resultTemplatePromise = $.ajax({
     type: 'GET',
     url: '/search/catalog/internal/forms/result',
     contentType: 'application/json',
     success: function (data) {
         resultTemplates = data;
     }
  });

  return new (Backbone.Model.extend({
      initialize: function () {
          resultTemplatePromise.then(() => {
              if (!this.isDestroyed) {
                  const customResultTemplates = _.map(resultTemplates, function(resultForm) {
                      return {
                        label: resultForm.title,
                        value: resultForm.id,
                        id: resultForm.id,
                        descriptors: resultForm.descriptors,
                        description: resultForm.description
                      };
                  });

                  customResultTemplates.push({
                    label: 'All Fields',
                    value: 'allFields',
                    id: 'allFields',
                    descriptors: [],
                    description: 'All Fields'
                  });
                  resultTemplateProperties = customResultTemplates;
              }
          });
      },
      getResultTemplatesProperties: function() {
          return resultTemplateProperties;
      }
    }))();
});