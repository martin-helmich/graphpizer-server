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

requirejs.config
  paths:
    'angular': ['../lib/angularjs/angular']
    'angular-route': ['../lib/angularjs/angular-route']
    'angular-resource': ['../lib/angularjs/angular-resource']
    'd3': ['../lib/d3js/d3']
    'vis': ['../lib/visjs/vis']
    'jquery': ['../lib/jquery/jquery']
    'bootstrap': ['../lib/bootstrap/js/bootstrap']
  shim:
    'angular':
      exports: 'angular'
    'angular-route':
      deps: ['angular']
      exports: 'angular'
    'angular-resource':
      deps: ['angular']
      exports: 'angular'
    'bootstrap':
      deps: ['jquery']

require [
  'angular',
  './Application',
  'angular-route',
  'angular-resource',
  'jquery',
  'bootstrap',
  './controllers/all',
  './resources/all',
  './services/ProjectService',
  './directives/all'
], (angular, app) ->
  app.config ['$routeProvider', ($routeProvider) ->
    $routeProvider
    .when '/',
      templateUrl: 'assets/partials/index.html'
      controller: 'IndexCtrl'
    .when '/projects/new',
      templateUrl: 'assets/partials/project/new.html'
      controller: 'ProjectNewCtrl'
    .when '/projects/:project',
      templateUrl: 'assets/partials/project/details.html'
      controller: 'ProjectDetailCtrl'
    .when '/projects/:project/delete',
      templateUrl: 'assets/partials/project/confirmdelete.html'
      controller: 'ProjectDeleteCtrl'
    .when '/projects/:project/source/files',
      templateUrl: 'assets/partials/source/files.html'
      controller: 'FileCtrl'
    .when '/projects/:project/model/packages',
      templateUrl: 'assets/partials/model/packages.html'
      controller: 'PackageCtrl'
    .when '/projects/:project/model/classes',
      templateUrl: 'assets/partials/model/classes.html'
      controller: 'ClassCtrl'
    .when '/projects/:project/model/classes/graph',
      templateUrl: 'assets/partials/model/classgraph.html'
      controller: 'ClassGraphCtrl'
    .when '/projects/:project/cypher',
      templateUrl: 'assets/partials/cypher.html'
      controller: 'CypherCtrl'
    .otherwise redirectTo: '/'
  ]

  angular.bootstrap document, ['graphizerApp']