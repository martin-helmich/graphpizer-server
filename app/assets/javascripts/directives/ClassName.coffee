define ['angular', '../Application'], (angular, app) ->

  app.directive 'className', () ->
    restrict: 'E'
    scope:
      fqcn: '=fqcn'
    link: (scope, elem, attrs) ->
      if typeof(scope.fqcn) is "string"
        namespaces = scope.fqcn.split '\\'
        scope.namespace = namespaces[0..-2].join "\\"
        scope.class = namespaces[-1..][0]
    template: '<span class="text-muted">{{namespace}} \\ </span>{{class}}'

#    (input, cls = 'mute') ->
#      if typeof(input) is "string"
#        namespaces = input.split '\\'
#        "<span class=\"#{cls}\">" + (namespaces[0..-1].join "\\") + "\\</span>" + namespaces[-1]