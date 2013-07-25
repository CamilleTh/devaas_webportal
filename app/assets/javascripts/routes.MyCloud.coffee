class MyCloudRouter extends Backbone.Router

  initialize: ()->
    @creationView=new app.views.AppCreationView()
    @detailView=new app.views.AppDetailView()
    @listView=new app.views.AppListView()
    app.collections.Applications.fetch()

  routes:
    "application/:id": "getApplication"
    "newApplication": "newApplication"
    "cancel": "cancelNewApp"

  getApplication: (id)->
    @creationView.hide()
    @detailView.displayDetail(id)
    @listView.selectApplication(id)
    @listView.render()

  newApplication: ()->
    @detailView.hide()
    @listView.unselect()
    @listView.render()
    @creationView.show()

  cancelNewApp: ()->
    @creationView.hide()

window.app=window.app || {}
window.app.routers=window.app.routers || {}
window.app.routers.MyCloudRouter=new MyCloudRouter()

$ ->
  Backbone.history.start()