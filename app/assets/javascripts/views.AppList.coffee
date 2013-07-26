class AppListView extends Backbone.View

  el: '#app-list'

  initialize: ()->
    app.collections.Applications.on('reset', @addAll)

  render: ()->
    @addAll()

  addAll: ()=>
    $("#app-list").html('<li class="nav-header">Applications</li>')
    @addOne app for app in window.app.collections.Applications.models
    $("#app-list").append('<li><a href="#newApplication"><button class="btn btn-warning btn-mini" style="outline:0;width:100%;margin-top:0px"><i class="icon-white icon-plus"></i></button></a></li>')

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