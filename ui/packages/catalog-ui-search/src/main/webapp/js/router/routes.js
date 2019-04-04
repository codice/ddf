;({
  openWorkspace: {
    patterns: ['workspaces/:id'],
    component: 'component/content/content.view',
    menu: {
      component: 'react-component/container/workspace-menu/workspace-menu',
    },
  },
  home: {
    patterns: ['(?*)', 'workspaces(/)'],
    component:
      'react-component/container/workspaces-container/workspaces-container',
    menu: {
      component: 'component/workspaces-menu/workspaces-menu.view',
    },
  },
  openMetacard: {
    patterns: ['metacards/:id'],
    component: 'component/metacard/metacard.view',
    menu: {
      component: 'component/metacard-menu/metacard-menu.view',
    },
  },
  openAlert: {
    patterns: ['alerts/:id'],
    component: 'component/alert/alert.view',
    menu: {
      component: 'component/alert-menu/alert-menu.view',
    },
  },
  openIngest: {
    patterns: ['ingest(/)'],
    component: 'component/ingest/ingest.view',
    menu: {
      text: 'Upload',
      classes: 'is-bold',
    },
  },
  openUpload: {
    patterns: ['uploads/:id'],
    component: 'component/upload/upload.view',
    menu: {
      component: 'component/upload-menu/upload-menu.view',
    },
  },
  openSources: {
    patterns: ['sources(/)'],
    component: 'react-component/container/sources-container/sources-container',
    menu: {
      i18n: true,
      id: 'sources.title',
      defaultMessage: 'Sources',
      classes: 'is-bold',
    },
  },
  openAbout: {
    patterns: ['about(/)'],
    component: 'react-component/container/about-container/about-container',
    menu: {
      text: 'About',
      classes: 'is-bold',
    },
  },
  openForms: {
    patterns: ['forms(/)'],
    component: 'component/tabs/search-form/tabs.search-form.view',
    menu: {
      text: 'Search Forms',
      classes: 'is-bold',
    },
  },
  newForm: {
    patterns: ['forms/:id'],
    component: 'component/search-form-editor/search-form-editor.view',
    menu: {
      text: 'Search Form Editor',
      classes: 'is-bold',
    },
  },
  openResultForms: {
    patterns: ['resultForms(/)'],
    component: 'component/tabs/result-form/tabs.result-form.view',
    menu: {
      text: 'Result Forms',
      classes: 'is-bold',
    },
  },
  searches: {
    patterns: ['searches(/)'],
    component: 'react-component/container/searches-container/search-root',
    menu: {
      text: 'Searches',
      classes: 'is-bold',
    },
  },
})
