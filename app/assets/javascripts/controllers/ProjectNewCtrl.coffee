define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'ProjectNewCtrl', ['$scope', 'Project', ($scope, Project) ->
    $scope.save = (projectData) ->
      project = new Project(projectData)
      project.$save -> $scope.$emit 'projectCreated', project
  ]