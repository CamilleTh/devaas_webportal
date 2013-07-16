class AppList extends Backbone.Collection

  model: app.models.App

  url: "/applications"




window.app=window.app || {}
window.app.collections=window.app.collections || {}
window.app.collections.Applications=new AppList()