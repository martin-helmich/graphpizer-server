define ['angular', '../Application'], (angular, app) ->
  app.directive 'visGraph', ['$window', ($window) ->
    restrict: 'EA'
    scope:
      nodes: '=nodes'
      edges: '=edges'
    link: (scope, element, attrs) ->
      nodes = new vis.DataSet(scope.nodes)
      edges = new vis.DataSet(scope.edges)
]