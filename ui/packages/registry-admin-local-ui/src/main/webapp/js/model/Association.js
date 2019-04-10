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
  'backbone',
  'underscore',
  'js/model/FieldDescriptors.js',
  'js/model/Segment.js',
  'backboneassociation',
], function(Backbone, _, FieldDescriptors, Segment) {
  var counter = 0

  function generateId() {
    counter++
    return 'urn:association:id:' + new Date().getTime() + '-' + counter
  }

  function getSegmentModel(segment, id) {
    if (segment.get('segmentId') === id) {
      return segment
    }
    for (
      var index = 0;
      index < segment.get('segments').models.length;
      index++
    ) {
      var seg = getSegmentModel(segment.get('segments').models[index], id)
      if (seg) {
        return seg
      }
    }
  }

  var Association = {}

  Association.Association = Backbone.AssociatedModel.extend({
    defaults: {
      id: undefined,
      type: undefined,
      sourceId: undefined,
      sourceName: undefined,
      targetId: undefined,
      targetName: undefined,
      targetType: undefined,
    },
    populateFromModel: function(association, topLevelSegment) {
      if (association) {
        this.set('id', association.id)
        this.set('type', association.associationType)
        this.set('sourceId', association.sourceObject)
        this.set('targetId', association.targetObject)
      }
      var sourceSeg = getSegmentModel(topLevelSegment, this.get('sourceId'))
      var targetSeg = getSegmentModel(topLevelSegment, this.get('targetId'))
      this.set('targetType', targetSeg.get('segmentType'))
      this.set(
        'sourceName',
        sourceSeg.constructTitle
          ? sourceSeg.constructTitle()
          : sourceSeg.getField('Name').get('value')
      )
      this.set(
        'targetName',
        targetSeg.constructTitle
          ? targetSeg.constructTitle()
          : targetSeg.getField('Name').get('value')
      )
    },
  })
  Association.Associations = Backbone.Collection.extend({
    model: Association.Association,
  })

  Association.AssociationModel = Backbone.AssociatedModel.extend({
    relations: [
      {
        type: Backbone.Many,
        key: 'associations',
        collectionType: function() {
          return Association.Associations
        },
        relatedModel: function() {
          return Association.Association
        },
      },
      {
        type: Backbone.Many,
        key: 'associationSegments',
        collectionType: function() {
          return Association.SegmentIds
        },
        relatedModel: function() {
          return Association.SegmentId
        },
      },
      {
        type: Backbone.One,
        key: 'topSegment',
        relatedModel: function() {
          return Segment.Segment
        },
      },
    ],
    defaults: function() {
      return {
        associations: [],
        associationSegments: [],
        topSegment: null,
      }
    },
    populateFromModel: function(associations) {
      var model = this
      var collectionModel = this.get('associations')
      this.dataModel = associations
      _.each(associations, function(association) {
        var newAssociation = new Association.Association()
        newAssociation.populateFromModel(association, model.get('topSegment'))
        collectionModel.add(newAssociation)
      })
      this.populateAssociationSegmentIds(this.get('topSegment'))
    },
    addAssociation: function(source, target, type) {
      var association = new Association.Association({
        id: generateId(),
        sourceId: source,
        targetId: target,
        type: type,
      })
      association.populateFromModel(undefined, this.get('topSegment'))
      this.get('associations').add(association)
      return association
    },
    removeAssociation: function(id) {
      var removedAssociation = _.find(this.get('associations').models, function(
        association
      ) {
        return association.get('id') === id
      })
      this.get('associations').remove(removedAssociation)
      return removedAssociation
    },
    removeSegment: function(segment) {
      var model = this
      var segId = segment.get('segmentId')
      this.get('associations').remove(
        this.get('associations').filter(function(association) {
          return (
            association.get('sourceId') === segId ||
            association.get('targetId') === segId
          )
        })
      )

      var seg = this.get('associationSegments').find(function(seg) {
        return seg.get('segmentId') === segId
      })
      this.get('associationSegments').remove(seg)
      segment.get('segments').forEach(function(curSeg) {
        if (FieldDescriptors.isCustomizableSegment(curSeg.get('segmentType'))) {
          model.removeSegment(curSeg)
        }
      })
    },
    getAssociationsForId: function(id) {
      var associations = this.get('associations').models
      var results = []
      _.each(associations, function(association) {
        if (association.get('sourceId') === id) {
          results.push(association)
        }
      })
      return results
    },
    getAvailableAssociationSegments: function(id) {
      var segs = []
      var curAssociations = this.getAssociationsForId(id)
      var model = this
      this.get('associationSegments').forEach(function(seg) {
        if (
          seg.get('segmentId') !== id &&
          !model.existingAssociation(curAssociations, seg.get('segmentId'))
        ) {
          segs.push(seg.attributes)
        }
      })
      return segs
    },
    existingAssociation: function(associations, id) {
      for (var index = 0; index < associations.length; index++) {
        if (associations[index].get('targetId') === id) {
          return true
        }
      }
      return false
    },
    populateAssociationSegmentIds: function(segment) {
      if (
        segment &&
        FieldDescriptors.isCustomizableSegment(segment.get('segmentType')) &&
        segment.get('fields').models.length > 0
      ) {
        this.get('associationSegments').push(
          new Association.SegmentId({
            segmentId: segment.get('segmentId'),
            segmentType: segment.get('segmentType'),
            segmentName: segment.constructTitle
              ? segment.constructTitle()
              : segment.getField('Name').get('value'),
          })
        )
      }
      var model = this
      if (segment.get('segments') && segment.get('segments').models) {
        segment.get('segments').models.forEach(function(seg) {
          model.populateAssociationSegmentIds(seg)
        })
      }
    },
    addAssociationSegment: function(id, type, name) {
      if (FieldDescriptors.isCustomizableSegment(type)) {
        this.get('associationSegments').add(
          new Association.SegmentId({
            segmentId: id,
            segmentType: type,
            segmentName: name,
          })
        )
      }
    },
    saveData: function() {
      var data = this.dataModel
      this.dataModel.length = 0
      var associations = this.get('associations').models
      _.each(associations, function(association) {
        data.push({
          targetObject: association.get('targetId'),
          associationType: association.get('type'),
          sourceObject: association.get('sourceId'),
          id: association.get('id'),
        })
      })
    },
  })

  Association.SegmentId = Backbone.AssociatedModel.extend({
    defaults: {
      segmentId: undefined,
      segmentType: undefined,
      segmentName: undefined,
    },
  })

  Association.SegmentIds = Backbone.Collection.extend({
    model: Association.SegmentId,
  })

  return Association
})
