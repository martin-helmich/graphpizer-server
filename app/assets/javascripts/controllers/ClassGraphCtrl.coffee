define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'ClassGraphCtrl', ['$scope', '$http', 'ProjectService', ($scope, $http, ProjectService) ->
    ProjectService.current().then (p) ->
      $http.get "/projects/#{p.slug}/model/classes/graph"
      .success (data) ->
        $scope.nodes = data.nodes
        $scope.edges = data.edges
  ]