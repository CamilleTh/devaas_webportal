class EnvItemView extends Backbone.View

  tagName:  'li'

  template: _.template( $('#template-env-item').html() )

  render: ()=>
    @$el.html( @template( @model.toJSON() ) )
    @$el.addClass(@model.get("name"))
    if @options.selected is true
      @$el.addClass("active")
    @

window.app=window.app || {}
window.app.views=window.app.views || {}
window.app.views.EnvItemView=EnvItemView