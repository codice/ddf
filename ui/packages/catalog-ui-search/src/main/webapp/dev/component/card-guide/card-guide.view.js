const template = require('./card-guide.hbs');
const CustomElements = require('js/CustomElements');
const BaseGuideView = require('dev/component/base-guide/base-guide.view');

const WorkspaceItemView = require('component/workspace-item/workspace-item.view');
const WorkspaceModel = require('js/model/Workspace');

const ResultItemView = require('component/result-item/result-item.view');
const SelectionInterfaceModel = require('component/selection-interface/selection-interface.model.js');
const QueryResultModel = require('js/model/QueryResult');

const QueryItemView = require('component/query-item/query-item.view');
const QueryModel = require('js/model/Query');

module.exports = BaseGuideView.extend({
    template: template,
    tagName: CustomElements.register('dev-card-guide'),
    regions: {
        workspaceExample: '.example > .workspace',
        resultExample: '.example > .result',
        result2Example: '.example > .result2',
        queryExample: '.example > .query'
    },
    showComponents() {
        this.showWorkspaceExample();
        this.showResultExample();
        this.showResult2Example();
        this.showQueryExample();
    },
    showQueryExample() {
        this.queryExample.show(new QueryItemView({
            model: new QueryModel.Model()
        }));
    },
    showWorkspaceExample() {
        this.workspaceExample.show(new WorkspaceItemView({
            model: new WorkspaceModel({
                title: 'My Cool Workspace',
                owner: 'Cool new developer'
            })
        }));
        this.workspaceExample.currentView.activateGridDisplay();
    },
    showResult2Example() {
        this.result2Example.show(new ResultItemView({
            model: new QueryResultModel({
                actions: [{
                    description: 'example',
                    id: 'example',
                    title: 'example',
                    url: 'https://example.com'
                }],
                distance: null,
                hasThumbnail: false,
                isResourceLocal: true,
                metacard: {
                    id: 'blah blah blah',
                    cached: "2018-06-28T01:51:32.800+0000",
                    properties: {
                        title: 'Example Result',
                        id: 'example',
                        "metacard-tags": ['resource', 'VALID'],
                        "validation-warnings": ['this isonly sort of wrong'],
                        "source-id": 'banana land',
                        "resource-download-url": "https://example.com"
                    }
                },
                relevance: 11
            }),
            selectionInterface: new SelectionInterfaceModel()
        }));
    },
    showResultExample() {
        this.resultExample.show(new ResultItemView({
            model: new QueryResultModel({
                actions: [{
                    description: 'example',
                    id: 'example',
                    title: 'example',
                    url: 'https://www.google.com'
                }],
                distance: null,
                hasThumbnail: false,
                isResourceLocal: true,
                metacard: {
                    id: 'blah blah blah',
                    cached: "2018-06-28T01:51:32.800+0000",
                    properties: {
                        title: 'Example Result',
                        id: 'example',
                        "metacard-tags": ['deleted', 'VALID'],
                        "validation-errors": ['wow this is way wrong'],
                        "validation-warnings": ['this isonly sort of wrong'],
                        "source-id": 'banana land'
                    }
                },
                relevance: 11
            }),
            selectionInterface: new SelectionInterfaceModel()
        }));
    }
});