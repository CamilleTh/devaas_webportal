class App extends Backbone.Model

  defaults:
    storageType: ''
    groupid: ''
    type: ''
    envs: []
    users: []

window.app=window.app || {}
window.app.models=window.app.models || {}
window.app.models.App=App