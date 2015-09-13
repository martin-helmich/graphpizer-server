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

define ['angular', '../Application'], (angular, app) ->

  app.directive 'className', () ->
    restrict: 'E'
    scope:
      fqcn: '=fqcn'
    link: (scope, elem, attrs) ->
      if typeof(scope.fqcn) is "string"
        namespaces = scope.fqcn.split '\\'
        scope.namespace = namespaces[0..-2].join "\\"
        scope.class = namespaces[-1..][0]
    template: '<span class="text-muted">{{namespace}} \\ </span>{{class}}'

#    (input, cls = 'mute') ->
#      if typeof(input) is "string"
#        namespaces = input.split '\\'
#        "<span class=\"#{cls}\">" + (namespaces[0..-1].join "\\") + "\\</span>" + namespaces[-1]