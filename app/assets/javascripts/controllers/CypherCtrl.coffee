define ['angular', '../Application', '../resources/Project'], (angular, app) ->
  app.controller 'CypherCtrl', ['$scope', '$location', '$http', 'ProjectService',
    ($scope, $location, $http, ProjectService) ->
      ProjectService.current().then (project) ->
        $scope.execute = (cypher, graph) ->
          q =
            cypher: cypher
            params: {}
            graph: graph

          $http.post '/projects/' + project.slug + '/cypher', q
          .success (data, status, headers, config) ->
            $scope.inpError = false
            $scope.inpErrorMsg = null

            console.log data
            $scope.data = data
          .error (data, status) ->
            if status == 400
              $scope.inpError = true
              $scope.inpErrorMsg = data.message
              $scope.data = null
  ]