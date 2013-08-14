class App extends Backbone.Model

  defaults:
    storageType: ''
    groupid: ''
    port: ''
    type: ''
    envs: [{"name":"Dev","version":"null"},{"name":"Test","version":"null"},{"name":"Prod","version":"null"}]
    users: []

window.app=window.app || {}
window.app.models=window.app.models || {}
window.app.models.App=App