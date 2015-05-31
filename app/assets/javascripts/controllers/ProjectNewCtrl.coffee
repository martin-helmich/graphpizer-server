define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'ProjectNewCtrl', ['$scope', '$location', 'Project', ($scope, $location, Project) ->
    $scope.save = (projectData) ->
      project = new Project(projectData)
      project.$save ->
        $scope.$emit 'projectCreated', project
        $location.path "/projects/#{project.slug}"
  ]