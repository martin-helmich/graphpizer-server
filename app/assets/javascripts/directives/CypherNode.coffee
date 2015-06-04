define ['angular', '../Application'], (angular, app) ->

  app.directive 'cypherNode', () ->
    restrict: 'E'
    scope:
      node: '=node'
    templateUrl: 'assets/partials/cypher/node.html'

#    (input, cls = 'mute') ->
#      if typeof(input) is "string"
#        namespaces = input.split '\\'
#        "<span class=\"#{cls}\">" + (namespaces[0..-1].join "\\") + "\\</span>" + namespaces[-1]