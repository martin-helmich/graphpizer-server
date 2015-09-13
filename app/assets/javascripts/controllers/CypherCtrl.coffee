# GraPHPizer source code analytics engine
# Copyright (C) 2015  Martin Helmich <kontakt@martin-helmich.de>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

define ['angular', '../Application', '../resources/Project'], (angular, app) ->
  app.controller 'CypherCtrl', ['$scope', '$location', '$http', 'ProjectService', 'StoredQuery',
    ($scope, $location, $http, ProjectService, StoredQuery) ->
      $scope.storedQueries = StoredQuery.query()
      $scope.loadQuery = (query) ->
        if $scope.activeQuery == query
          $scope.cypher = undefined
          $scope.activeQuery = undefined
        else
          $scope.cypher = query.cypher
          $scope.activeQuery = query

      $scope.saveQuery = (cypher) ->
        if not $scope.activeQuery?
          query = new StoredQuery cypher: cypher
          query.$save -> $scope.storedQueries = StoredQuery.query()

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