define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'ProjectDetailCtrl', ['$scope', '$routeParams', 'Project', ($scope, $routeParams, Project) ->
    $scope.project = Project.get id: $routeParams['project']
  ]