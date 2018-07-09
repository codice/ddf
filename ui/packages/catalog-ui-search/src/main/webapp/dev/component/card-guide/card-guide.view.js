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
        queryExample: '.example > .query'
    },
    showComponents() {
        this.showWorkspaceExample();
        this.showResultExample();
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
                    cached: "2018-06-28T01:51:32.800+0000",
                    properties: {
                        title: 'Example Result',
                        id: 'example',
                        "metacard-tags": ['resource', 'VALID']
                    }
                },
                relevance: 11
            }),
            selectionInterface: new SelectionInterfaceModel()
        }));
    }
});