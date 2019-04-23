const Marionette = require('marionette')
const Backbone = require('backbone')
const wreqr = require('../wreqr.js')
const geocoderTemplate = require('templates/geocoder.handlebars')
const $ = require('jquery')

const geocoder = {};
const url = './internal/REST/v1/Locations';
const geocoderModel = new Backbone.Model();
geocoder.View = Marionette.ItemView.extend({
  template: geocoderTemplate,
  events: {
    'keyup #searchfield': 'searchOnEnter',
    'click #searchbutton': 'search',
  },
  initialize: function() {
    this.model = geocoderModel
    this.modelBinder = new Backbone.ModelBinder()
    this.listenTo(this.model, 'change', this.changedSearchText)
  },
  onRender: function() {
    const searchBinding = Backbone.ModelBinder.createDefaultBindings(
      this.el,
      'name'
    );
    this.modelBinder.bind(this.model, this.$el, searchBinding)
  },
  searchOnEnter: function(e) {
    if (e.keyCode === 13) {
      //user pushed enter, perform search
      this.model.set('searchText', this.$('#searchfield').val())
      if (this.model.get('searchText')) {
        this.search()
      }
      e.preventDefault()
    }
  },
  changedSearchText: function() {
    if (this.model.get('searchText')) {
      this.$('#searchfield').addClass('geocoder-input-wide')
    } else {
      this.$('#searchfield').removeClass('geocoder-input-wide')
    }
  },
  search: function() {
    const view = this;
    if (this.model.get('searchText')) {
      $.ajax({
        url: url,
        data: 'jsonp=jsonp&query=' + this.model.get('searchText'),
        contentType: 'application/javascript',
        dataType: 'jsonp',
        jsonp: 'jsonp',
        success: function(result) {
          if (result.resourceSets.length === 0) {
            view.model.set(
              'searchText',
              view.model.get('searchText') + ' (not found)'
            )
            return
          }
          const resourceSet = result.resourceSets[0];
          if (resourceSet.resources.length === 0) {
            view.model.set(
              'searchText',
              view.model.get('searchText') + ' (not found)'
            )
            return
          }
          const resource = resourceSet.resources[0];
          view.model.set('searchText', resource.name)
          const bbox = resource.bbox;
          const south = bbox[2];
          const west = bbox[1];
          const north = bbox[0];
          const east = bbox[3];
          wreqr.vent.trigger('search:maprectanglefly', [
            [west, north],
            [east, south],
          ])
        },
        error: function(data) {
          view.model.set('searchText', data)
        },
      })
    }
  },
})
module.exports = geocoder
