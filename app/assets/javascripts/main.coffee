requirejs.config
  paths:
    'angular': ['../lib/angularjs/angular']
    'angular-route': ['../lib/angularjs/angular-route']
    'angular-resource': ['../lib/angularjs/angular-resource']
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
  './services/ProjectService'
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
    .otherwise redirectTo: '/'
  ]

  angular.bootstrap document, ['graphizerApp']