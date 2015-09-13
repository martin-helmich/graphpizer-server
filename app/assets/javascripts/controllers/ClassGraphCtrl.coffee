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

  app.controller 'ClassGraphCtrl', ['$scope', '$http', 'ProjectService', ($scope, $http, ProjectService) ->
    ProjectService.current().then (p) ->
      $http.get "/projects/#{p.slug}/model/classes/graph"
      .success (data) ->
        $scope.nodes = data.nodes
        $scope.edges = data.edges
  ]