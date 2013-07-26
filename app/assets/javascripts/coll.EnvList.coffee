class EnvList extends Backbone.Collection

  model: app.models.Env

  url: ""




window.app=window.app || {}
window.app.collections=window.app.collections || {}
window.app.collections.Envs=new EnvList()

