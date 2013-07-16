class App extends Backbone.Model

  defaults:
    storageType: ''
    type: ''
    users: []

window.app=window.app || {}
window.app.models=window.app.models || {}
window.app.models.App=App