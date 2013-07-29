class Env extends Backbone.Model

  defaults:
    users: []
    appId: ''

window.app=window.app || {}
window.app.models=window.app.models || {}
window.app.models.Env=Env