/*global define*/

define(function (require) {
    "use strict";
    var Backbone = require('backbone'),
        _ = require('underscore'),
        ddf = require('ddf'),
        MetaCard = ddf.module();



    require('backbonerelational');
    MetaCard.Geometry = Backbone.RelationalModel.extend({

        isPoint : function(){
            return this.get('type') === 'Point';
        },
        getPoint : function(){
            if(!this.isPoint()){
                console.log('This is not a point!! ', this);
                return;
            }
            var coordinates = this.get('coordinates');

            return this.convertPointCoordinate(coordinates);

        },
        convertPointCoordinate : function(coordinate){
            return {latitude : coordinate[1], longitude : coordinate[0], altitude : coordinate[2]} ;
        },

        isPolygon : function(){
            return this.get('type') === 'Polygon';
        },
        getPolygon : function(){
            if(!this.isPolygon()){
                console.log('This is not a polygon!! ', this);
                return;
            }
            var coordinates = this.get('coordinates');
            return _.map(coordinates, this.convertPointCoordinate);
        }

    });

    MetaCard.Properties = Backbone.RelationalModel.extend({

    });

    MetaCard.Metacard = Backbone.RelationalModel.extend({
        initialize : function(){
            this.listenTo(this,'change:context', this.onChangeContext);
        },

        onChangeContext : function(){
            var eventBus = ddf.app,
                name = 'model:context';

            if(this.get('context')){
                eventBus.trigger(name, this);
                this.listenTo(eventBus,name,this.onAppContext);
            }
        },

        onAppContext : function(model){
            var eventBus = ddf.app,
                name = 'model:context';
            if(model !== this){
                this.stopListening(eventBus,name);
                this.set('context',false);

            }
        },

        relations: [
            {
                type: Backbone.HasOne,
                key: 'geometry',
                relatedModel: MetaCard.Geometry
            },
            {
                type: Backbone.HasOne,
                key: 'properties',
                relatedModel: MetaCard.Properties
            }

        ]
    });

    MetaCard.MetacardResult = Backbone.RelationalModel.extend({
        relations: [
            {
                type: Backbone.HasOne,
                key: 'metacard',
                relatedModel: MetaCard.Metacard
            }
        ]
    });

    MetaCard.MetacardList = Backbone.Collection.extend({
        model: MetaCard.MetacardResult
    });

    MetaCard.SearchResult = Backbone.RelationalModel.extend({
        relations: [
            {
                type: Backbone.HasMany,
                key: 'results',
                relatedModel: MetaCard.MetacardResult,
                collectionType: MetaCard.MetacardList
            }
        ],
        url: "/services/catalog/query",
        loadMoreResults: function () {
            var queryParams = this.get("queryParams");
            this.set("count", this.get("count") + this.get("itemsPerPage"));
            queryParams = queryParams.replace(new RegExp("&count=\\d*", "g"), "&count=" + this.get("count"));
            this.fetch({
                url: this.url,
                data: queryParams,
                dataType: "jsonp",
                timeout: 300000
            });
        }
    });
    return MetaCard;

});