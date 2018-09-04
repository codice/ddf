({
    openWorkspace: {
        patterns: ['workspaces/:id'],
        component: 'component/content/content.view',
        menu: {
            component: 'component/workspace-menu/workspace-menu.view'
        }
    },
    home: {
        patterns: ['(?*)', 'workspaces(/)'],
        component: 'react-component/container/workspaces-container/workspaces-container',
        menu: {
            component: 'component/workspaces-menu/workspaces-menu.view'
        }
    },
    openMetacard: {
        patterns: ['metacards/:id'],
        component: 'component/metacard/metacard.view',
        menu: {
            component: 'component/metacard-menu/metacard-menu.view'
        }
    },
    openAlert: {
        patterns: ['alerts/:id'],
        component: 'component/alert/alert.view',
        menu: {
            component: 'component/alert-menu/alert-menu.view'
        }
    },
    openIngest: {
        patterns: ['ingest(/)'],
        component: 'component/ingest/ingest.view',
        menu: {
            text: 'Upload',
            classes: 'is-bold'
        }
    },
    openUpload: {
        patterns: ['uploads/:id'],
        component: 'component/upload/upload.view',
        menu: {
            component: 'component/upload-menu/upload-menu.view'
        }
    },
    openSources: {
        patterns: ['sources(/)'],
        component: 'react-component/presentation/sources/sources',
        menu: {
            text: 'Sources',
            classes: 'is-bold'
        }
    },
    openAbout: {
        patterns: ['about(/)'],
        component: 'react-component/container/about-container/about-container',
        menu: {
            text: 'About',
            classes: 'is-bold'
        }
    }
})