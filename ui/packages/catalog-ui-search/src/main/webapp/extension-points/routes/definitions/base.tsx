export const home = require('!./loader!./base/home.js')
export const newForm = require('!./loader!./base/newForm.js')
export const openAbout = require('!./loader!./base/openAbout.js')
export const openAlert = require('!./loader!./base/openAlert.js')
export const openForms = require('!./loader!./base/openForms.js')
export const openIngest = require('!./loader!./base/openIngest.js')
export const openMetacard = require('!./loader!./base/openMetacard.js')
export const openResultForm = require('!./loader!./base/openResultForm.js')
export const openSources = require('!./loader!./base/openSources.js')
export const openUpload = require('!./loader!./base/openUpload.js')
export const openWorkspace = require('!./loader!./base/openWorkspace.js')

const base = {
  ...home,
  ...newForm,
  ...openAbout,
  ...openAlert,
  ...openForms,
  ...openIngest,
  ...openMetacard,
  ...openResultForm,
  ...openSources,
  ...openUpload,
  ...openWorkspace,
}

export default base
