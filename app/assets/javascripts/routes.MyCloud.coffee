class MyCloudRouter extends Backbone.Router

  initialize: ()->
    @creationView=new app.views.AppCreationView()
    @detailView=new app.views.AppDetailView()
    @listView=new app.views.AppListView()
    app.collections.Applications.fetch()

  routes:
    "application/:id": "getApplication"
    "newApplication": "newApplication"

  getApplication: (id)->
    @creationView.hide()
    @detailView.displayDetail(id)
    @listView.selectApplication(id)
    @listView.render()

  newApplication: ()->
    @detailView.hide()
    @creationView.show()

window.app=window.app || {}
window.app.routers=window.app.routers || {}
window.app.routers.MyCloudRouter=new MyCloudRouter()

$ ->
  Backbone.history.start()