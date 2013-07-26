class MyCloudRouter extends Backbone.Router

  initialize: ()->
    @creationView=new app.views.AppCreationView()
    @detailView=new app.views.AppDetailView()
    @appListView=new app.views.AppListView()
    @envListView=new app.views.EnvListView()
    app.collections.Applications.fetch()

  routes:
    "application/:id": "getApplication"
    "newApplication": "newApplication"
    "cancel": "cancelNewApp"

  getApplication: (id)->
    @envListView.show()
    @creationView.hide()
    @detailView.displayDetail(id)
    @appListView.selectApplication(id)
    @appListView.render()
    @envListView.applicationSwitch(id)
    app.collections.Envs.fetch()
    @envListView.render()

  newApplication: ()->
    @detailView.hide()
    @unselect()
    @appListView.render()
    @creationView.show()

  cancelNewApp: ()->
    @creationView.hide()

  unselect: ()->
    @envListView.unselect()
    @envListView.hide()
    @appListView.unselect()


window.app=window.app || {}
window.app.routers=window.app.routers || {}
window.app.routers.MyCloudRouter=new MyCloudRouter()

$ ->
  Backbone.history.start()