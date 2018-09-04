/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global require,__dirname*/
var express = require('express'),
    server = require('./server-impl');

var app = express();
// uncomment to get some debugging
//app.use(express.logger());
//enable the live reload
app.use(require('connect-livereload')());

// our compiled css gets moved to /target/webapp/css so use it there
app.use('/css',express.static(__dirname + '/target/webapp/css'));
app.use('/lib',express.static(__dirname + '/target/webapp/lib'));
app.use(express.static(__dirname + '/src/main/webapp'));

//if we're mocking, it is being run by grunt
console.log('setting up mock query endpoint');
app.all('./internal/catalog/sources', server.mockSources);
app.all('/admin/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listModules', server.mockRequest);
app.all('/admin/jolokia/exec/org.codice.ddf.admin.insecure.defaults.service.InsecureDefaultsServiceBean:service=insecure-defaults-service/validate', server.mockRequest);
app.all('/admin/jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/InstallationProfiles/', server.mockInstallationProfiles);
app.all('/admin/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/getService/*', server.mockGetService);
app.all('/admin/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/getClaimsConfiguration/*', server.mockClaims);
app.all('/admin/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/add', server.mockRequest);
app.all('/admin/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listServices', server.mockRequest);
app.all('/admin/jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/ApplicationTree/', server.mockAppTree);
app.all('/admin/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/startApplication/*', server.mockStart);

exports = module.exports = app;

exports.use = function() {
	app.use.apply(app, arguments);
};