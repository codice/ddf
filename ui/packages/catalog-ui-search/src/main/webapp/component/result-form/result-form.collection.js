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
 /* global require */
 const Backbone = require('backbone')
 const ResultForm = require('component/search-form/search-form')
 const Common = require('js/Common')
 const user = require('component/singletons/user-instance')
 const Results = require('component/result-form/result-form')
 const wreqr = require('wreqr')

 module.exports = Backbone.AssociatedModel.extend({
   model: ResultForm,
   defaults: {
     doneLoading: false,
     resultForms: []
   },
   initialize: function () {
     this.addResultForm(new ResultForm({
       name: 'Create New Data View',
       type: 'newResult'
     }))
     this.addResultForms()
   },
   relations: [{
     type: Backbone.Many,
     key: 'resultForms',
     collectionType: Backbone.Collection.extend({
       model: ResultForm,
       initialize: function () {}
     })
   }],
   addResultForms: function () {
     if (!this.isDestroyed) {
       let filteredList = Results.getResultTemplatesProperties().filter(function (resultField) {
         return resultField.id !== 'allFields'
       })
       filteredList.forEach(element => {
         let utcSeconds = element.created / 1000
         let d = new Date(0)
         d.setUTCSeconds(utcSeconds)
         this.addResultForm(new ResultForm({
           createdOn: Common.getHumanReadableDate(d),
           id: element.id,
           name: element.label,
           type: 'result',
           descriptors: element.descriptors,
           accessIndividuals: element.accessIndividuals,
           accessGroups: element.accessGroups,
           description: element.description
         }))
       })
     };
   },
   checkIfOwnerOrSystem: function (template) {
     let myEmail = user.get('user').get('email')
     let templateCreator = template.creator
     return myEmail === templateCreator || templateCreator === 'System Template'
   },
   addResultForm: function (newForm) {
     this.get('resultForms').add(newForm)
   },
   getDoneLoading: function () {
     return this.get('doneLoading')
   },
   doneLoading: function () {
     this.set('doneLoading', true)
   },
   getCollection: function () {
     return this.get('resultForms')
   },
   deleteCachedTemplateById: function (id) {
     Results.deleteResultTemplateById(id)
   }
 })
