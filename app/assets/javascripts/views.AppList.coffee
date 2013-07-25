class AppListView extends Backbone.View

  el: '#app-list'

  initialize: ()->
    app.collections.Applications.on('reset', @addAll)

  render: ()->
    @addAll()

  addAll: ()=>
    $("#app-list").html('')
    @addOne app for app in window.app.collections.Applications.models

  addOne: (app)=>
    view=new window.app.views.AppItemView(
      model: app
      selected: (app.get("id") is @selected)
    )
    $("#app-list").append(view.render().el)

  selectApplication: (id)=>
    @selected=id

  unselect: ()=>
    @selected=false

window.app=window.app || {}
window.app.views=window.app.views || {}
window.app.views.AppListView=AppListView