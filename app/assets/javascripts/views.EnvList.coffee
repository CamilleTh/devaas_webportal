class EnvListView extends Backbone.View

  el: '#env-list'
  currentAppId: ''

  initialize: ()->
    app.collections.Envs.on('reset', @addAll)

  render: ()->
    @addAll()

  applicationSwitch: (id)=>
    @$currentAppId=id
    @unselect()
    app.collections.Envs.url="/envs/"+id
    app.collections.Envs.fetch()

  addAll: ()=>
    $("#env-list").html('<li class="nav-header">Environments</li>')
    @addOne env for env in window.app.collections.Envs.models
    $("#env-list").append('<li><a href="#addEnvironment" style="outline:0"><button class="btn btn-warning btn-mini" style="outline:0;width:100%;margin-top:0px"><i class="icon-white icon-plus"></i></button></a></li>')

  addOne: (env)=>
    env.attributes.appId=@$currentAppId
    view=new window.app.views.EnvItemView(
      model: env
      selected: (env.get("name") is @selected)
    )
    $("#env-list").append(view.render().el)

  selectEnvironment: (name)=>
    @selected=name

  unselect: ()=>
    @selected=false

  hide: ()->
    if @$el.css("display") isnt "none"
      @$el.hide()

  show: ()->
    if @$el.css("display") is "none"
      @$el.show()

window.app=window.app || {}
window.app.views=window.app.views || {}
window.app.views.EnvListView=EnvListView