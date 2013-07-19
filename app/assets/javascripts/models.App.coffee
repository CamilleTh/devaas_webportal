class App extends Backbone.Model

  defaults:
    storageType: ''
    groupid: ''
    type: ''
    envs: [{"name":"Dev","version":"null"}]
    users: []

window.app=window.app || {}
window.app.models=window.app.models || {}
window.app.models.App=App