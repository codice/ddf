export const home = require('!./routes-loader!./base/home.js')
export const newForm = require('!./routes-loader!./base/newForm.js')
export const openAbout = require('!./routes-loader!./base/openAbout.js')
export const openAlert = require('!./routes-loader!./base/openAlert.js')
export const openForms = require('!./routes-loader!./base/openForms.js')
export const openIngest = require('!./routes-loader!./base/openIngest.js')
export const openMetacard = require('!./routes-loader!./base/openMetacard.js')
export const openResultForm = require('!./routes-loader!./base/openResultForm.js')
export const openSources = require('!./routes-loader!./base/openSources.js')
export const openUpload = require('!./routes-loader!./base/openUpload.js')
export const openWorkspace = require('!./routes-loader!./base/openWorkspace.js')

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
