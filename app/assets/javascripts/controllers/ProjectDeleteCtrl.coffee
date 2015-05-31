define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'ProjectDeleteCtrl', ['$scope', '$routeParams', '$location', 'Project', ($scope, $routeParams, $location, Project) ->
    $scope.project = Project.get id: $routeParams['project']
    $scope.state = 'init'

    $scope.abortDelete = (project) -> $location.path "/projects/#{project.slug}"
    $scope.confirmDelete = (project) ->
      $scope.state = 'deleting'
      project.$delete -> $scope.state = 'deleted'
  ]