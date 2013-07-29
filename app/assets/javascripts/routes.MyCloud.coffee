class MyCloudRouter extends Backbone.Router

  initialize: ()->
    @creationView=new app.views.AppCreationView()
    @detailView=new app.views.AppDetailView()
    @appListView=new app.views.AppListView()
    @envListView=new app.views.EnvListView()
    app.collections.Applications.fetch()

  routes:
    "application/:id": "getApplication"
    "application/:id/:env": "getEnvDetails"
    "newApplication": "newApplication"
    "cancel": "cancelNewApp"

  getApplication: (id)->
    @creationView.hide()
    @detailView.hide()
    @appListView.selectApplication(id)
    @appListView.render()
    @envListView.applicationSwitch(id)
    @envListView.show()
    @envListView.render()

  getEnvDetails: (id,env)->
    @creationView.hide()
    @appListView.selectApplication(id)
    @appListView.render()
    @envListView.applicationSwitch(id)
    @envListView.show()
    @envListView.render()
    @envListView.selectEnvironment(env)
    @detailView.displayDetail(id)

  newApplication: ()->
    @detailView.hide()
    @envListView.unselect()
    @envListView.hide()
    @appListView.unselect()
    @appListView.render()
    @creationView.show()

  cancelNewApp: ()->
    @creationView.hide()

window.app=window.app || {}
window.app.routers=window.app.routers || {}
window.app.routers.MyCloudRouter=new MyCloudRouter()

$ ->
  Backbone.history.start()