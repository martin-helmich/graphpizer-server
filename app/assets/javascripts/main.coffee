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
  'd3',
  'vis',
  './controllers/all',
  './resources/all',
  './services/ProjectService',
  './directives/all'
], (angular, app) ->
  app.config ['$routeProvider', ($routeProvider) ->
    $routeProvider
    .when '/projects/new',
      templateUrl: 'assets/partials/project/new.html'
      controller: 'ProjectNewCtrl'
    .when '/projects/:project',
      templateUrl: 'assets/partials/project/details.html'
      controller: 'ProjectDetailCtrl'
    .when '/projects/:project/delete',
      templateUrl: 'assets/partials/project/confirmdelete.html'
      controller: 'ProjectDeleteCtrl'
    .when '/projects/:project/source/packages',
      templateUrl: 'assets/partials/source/packages.html'
      controller: 'PackageCtrl'
    .when '/projects/:project/source/files',
      templateUrl: 'assets/partials/source/files.html'
      controller: 'FileCtrl'
    .when '/projects/:project/model/classes',
      templateUrl: 'assets/partials/model/classes.html'
      controller: 'ClassCtrl'
    .when '/projects/:project/cypher',
      templateUrl: 'assets/partials/cypher.html'
      controller: 'CypherCtrl'
    .otherwise redirectTo: '/'
  ]

  angular.bootstrap document, ['graphizerApp']