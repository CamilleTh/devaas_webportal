class App extends Backbone.Model

  defaults:
    storageType: ''
    groupid: ''
    type: ''
    env: ['dev','test','prod']
    users: []

window.app=window.app || {}
window.app.models=window.app.models || {}
window.app.models.App=App