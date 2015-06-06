define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'ProjectDetailCtrl', ['$scope', '$routeParams', 'Project', 'ProjectModel', ($scope, $routeParams, Project, ProjectModel) ->
    $scope.project = Project.get id: $routeParams['project']
    $scope.model = ProjectModel.get id: $routeParams['project']

    $scope.generateModel = (project) ->
      Project.generateModel id: project.slug, {}
  ]