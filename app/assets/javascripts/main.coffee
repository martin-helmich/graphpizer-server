requirejs.config
  paths:
    'angular': ['../lib/angularjs/angular']
    'angular-route': ['../lib/angularjs/angular-route']
    'jquery': ['../lib/jquery/jquery']
    'bootstrap': ['../lib/bootstrap/js/bootstrap']
  shim:
    'angular':
      exports: 'angular'
    'angular-route':
      deps: ['angular']
      exports: 'angular'
    'bootstrap':
      deps: ['jquery']

require ['angular', 'angular-route', 'jquery', 'bootstrap'], (angular) ->
  angular.module 'graphizerApp', ['ngRoute']
    .config ['$routeProvider', ($routeProvider) ->
      console.log "foo"
  ]

  angular.bootstrap document, ['graphizerApp']