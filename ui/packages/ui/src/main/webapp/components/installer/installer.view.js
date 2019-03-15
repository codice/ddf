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

/** Main view page for add. */
define([
  'backbone.marionette',
  'underscore',
  'backbone',
  'js/wreqr.js',
  'js/views/installer/Welcome.view',
  'js/views/installer/Profile.view.js',
  'js/views/installer/SsoConfiguration.view.js',
  'js/views/installer/Configuration.view.js',
  'js/views/installer/GuestClaims.view.js',
  'js/views/installer/Finish.view.js',
  'components/installer-navigation/installer-navigation.view.js',
  './installer.hbs',
  'js/application',
  'js/CustomElements',
  'js/models/Service',
  'js/models/installer/Configuration.js',
], function(
  Marionette,
  _,
  Backbone,
  wreqr,
  WelcomeView,
  ProfileView,
  SsoConfigurationView,
  ConfigurationView,
  GuestClaimsView,
  FinishView,
  NavigationView,
  mainTemplate,
  Application,
  CustomElements,
  Service,
  ConfigurationModel
) {
  var guestClaimsServiceResponse = new Service.Response()
  guestClaimsServiceResponse.fetch({
    url:
      './jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/getClaimsConfiguration/(service.pid%3Dddf.security.guest.realm)',
  })

  var ssoConfigurationServiceResponses = new Service.Response()
  ssoConfigurationServiceResponses.listenTo(
    // when the config is modified in the installer
    wreqr.vent,
    'ssoConfigModified',
    function() {
      ssoConfigurationServiceResponses.attributes.modified = true
    }
  )
  ssoConfigurationServiceResponses.listenTo(
    // when the config is persisted in the installer
    wreqr.vent,
    'ssoConfigPersisted',
    function() {
      ssoConfigurationServiceResponses.attributes.fetched = false
      ssoConfigurationServiceResponses.attributes.modified = false
      ssoConfigurationServiceResponses.fetch({
        url:
          './jolokia/read/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/SsoConfigurations',
      })
    }
  )
  ssoConfigurationServiceResponses.fetch({
    url:
      './jolokia/read/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/SsoConfigurations',
  })

  var systemPropertiesWrapped = new ConfigurationModel.SystemPropertiesWrapped()
  systemPropertiesWrapped.fetch()

  var InstallerMainView = Marionette.Layout.extend({
    template: mainTemplate,
    tagName: CustomElements.register('installer'),
    className: 'container well well-main',
    regions: {
      welcome: '#welcome',
      profiles: '#profiles',
      guestClaims: '#guestClaims',
      configuration: '#configuration',
      ssoConfiguration: '#ssoConfiguration',
      finish: '#finish',
      navigation: '#navigation',
    },
    onRender: function() {
      this.navigation.show(new NavigationView({ model: this.model }))
      this.changePage()
      this.listenTo(this.model, 'change', this.changePage)
    },
    changePage: function() {
      //close whatever view is open
      this.$el.toggleClass('is-loading', false)
      var welcomeStep = 0,
        profileStep = 1,
        guestClaimsStep = 2,
        configStep = 3,
        ssoConfigurationStep = 4,
        finishStep = 5

      var stepNumber = this.model.get('stepNumber')

      if (this.welcome.currentView && stepNumber !== welcomeStep) {
        this.hideWelcome()
      }
      if (this.profiles.currentView && stepNumber !== profileStep) {
        this.hideProfiles()
      }
      if (this.guestClaims.currentView && stepNumber !== guestClaimsStep) {
        this.hideGuestClaims()
      }
      if (this.configuration.currentView && stepNumber !== configStep) {
        this.hideConfiguration()
      }
      if (
        this.ssoConfiguration.currentView &&
        stepNumber != ssoConfigurationStep
      ) {
        this.hideSsoConfiguration()
      }
      if (this.finish.currentView && stepNumber !== finishStep) {
        this.hideFinish()
      }
      //show the next or previous view
      if (!this.welcome.currentView && stepNumber <= welcomeStep) {
        this.showWelcome()
      } else if (!this.profiles.currentView && stepNumber === profileStep) {
        this.showProfiles()
      } else if (
        !this.guestClaims.currentView &&
        stepNumber === guestClaimsStep
      ) {
        this.showGuestClaims()
      } else if (!this.configuration.currentView && stepNumber === configStep) {
        this.showConfiguration()
      } else if (
        !this.ssoConfiguration.currentView &&
        stepNumber === ssoConfigurationStep
      ) {
        this.showSsoConfiguration()
      } else if (!this.finish.currentView && stepNumber >= finishStep) {
        this.showFinish()
      }
    },
    showLoading: function() {
      this.$el.toggleClass('is-loading', true)
    },
    showWelcome: function() {
      if (this.welcome.currentView) {
        this.welcome.show()
      } else {
        this.welcome.show(new WelcomeView({ navigationModel: this.model }))
      }
      this.$(this.welcome.el).show()
    },
    showProfiles: function() {
      var self = this
      if (this.profiles.currentView) {
        this.profiles.show()
      } else {
        Application.App.submodules.Installation.installerMainController
          .fetchInstallProfiles()
          .then(function(profiles) {
            // set initial selected profile if null.
            var profileKey = self.model.get('selectedProfile')
            if (!profileKey && !profiles.isEmpty()) {
              profileKey = profiles.first().get('name')
              self.model.set('selectedProfile', profileKey)
            }

            self.profiles.show(
              new ProfileView({
                navigationModel: self.model,
                collection: profiles,
              })
            )
          })
          .fail(function(error) {
            if (console) {
              console.log(error)
            }
          })
          .done()
      }
      this.$(this.profiles.el).show()
    },
    showGuestClaims: function() {
      if (guestClaimsServiceResponse.get('fetched')) {
        if (this.guestClaims.currentView) {
          this.guestClaims.show()
        } else {
          this.guestClaims.show(
            new GuestClaimsView({
              model: guestClaimsServiceResponse,
              navigationModel: this.model,
            })
          )
        }
        this.$(this.guestClaims.el).show()
      } else {
        this.listenToOnce(
          guestClaimsServiceResponse,
          'change:fetched',
          this.changePage
        )
        this.showLoading()
      }
    },
    showConfiguration: function() {
      if (systemPropertiesWrapped.get('fetched')) {
        if (this.configuration.currentView) {
          this.configuration.show()
        } else {
          this.configuration.show(
            new ConfigurationView({
              model: systemPropertiesWrapped.get('systemProperties'),
              navigationModel: this.model,
            })
          )
        }
        this.$(this.configuration.el).show()
      } else {
        this.listenToOnce(
          systemPropertiesWrapped,
          'change:fetched',
          this.changePage
        )
        this.showLoading()
      }
    },
    showSsoConfiguration: function() {
      if (ssoConfigurationServiceResponses.get('fetched') != true) {
        // wait for fetch
        this.listenToOnce(
          ssoConfigurationServiceResponses,
          'change:fetched',
          this.changePage
        )
        this.showLoading()
        return
      }

      if (ssoConfigurationServiceResponses.get('modified') === true) {
        // fetch and wait
        ssoConfigurationServiceResponses.attributes.fetched = false
        ssoConfigurationServiceResponses.fetch({
          url:
            './jolokia/read/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/SsoConfigurations',
        })
        this.listenToOnce(
          ssoConfigurationServiceResponses,
          'change:fetched',
          this.changePage
        )
        this.showLoading()
        return
      }

      if (this.ssoConfiguration.currentView) {
        this.ssoConfiguration.show()
      } else {
        this.ssoConfiguration.show(
          new SsoConfigurationView({
            metatypes: ssoConfigurationServiceResponses.get('value').models,
            navigationModel: this.model,
          })
        )
      }
      this.$(this.ssoConfiguration.el).show()
    },
    showFinish: function() {
      if (this.finish.currentView) {
        this.finish.show()
      } else {
        this.finish.show(new FinishView({ navigationModel: this.model }))
      }
      this.$(this.finish.el).show()
    },
    hideWelcome: function() {
      this.welcome.close()
      this.$(this.welcome.el).hide()
    },
    hideProfiles: function() {
      this.profiles.close()
      this.$(this.profiles.el).hide()
    },
    hideGuestClaims: function() {
      this.guestClaims.close()
      this.$(this.guestClaims.el).hide()
    },
    hideConfiguration: function() {
      this.configuration.close()
      this.$(this.configuration.el).hide()
    },
    hideSsoConfiguration: function() {
      this.ssoConfiguration.close()
      this.$(this.ssoConfiguration.el).hide()
    },
    hideFinish: function() {
      this.finish.close()
      this.$(this.finish.el).hide()
    },
  })

  return InstallerMainView
})
