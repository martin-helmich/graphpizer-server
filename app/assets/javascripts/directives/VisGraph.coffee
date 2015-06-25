define ['angular', '../Application', 'vis'], (angular, app, vis) ->
  app.directive 'visGraph', ['$window', ($window) ->
    restrict: 'E'
    scope:
      nodes: '=nodes'
      edges: '=edges'
    link: (scope, element, attrs) ->

      labels = []
      labelToNumber = (label) ->
        i = labels.indexOf label
        if i >= 0 then i
        else
          labels.push label
          labels.length - 1

      labelFromProperties = (node) ->
        props = node['properties'] or {}
        if props['name']? then props['name']
        else if props['title']? then props['title']
        else if props['fqcn']? then props['fqcn']
        else node.id

      titleFromNode = (node) ->
        str = "<div><strong>#{node.labels[0]}</strong><table>"
        for key of node.properties
          str += "<tr><th>#{key}</th><td>#{node.properties[key]}</td></tr>"
        str += "</table></div>"
        str

      mapNode = (node) ->
        id: node['id']
        title: titleFromNode node
        label: labelFromProperties node
        group: labelToNumber node['labels'][0]
      mapEdge = (edge) ->
        from: edge['from']
        to: edge['to']
        label: edge['label']
        font:
          align: 'middle'
        arrows:
          to: true

      data =
        nodes: new vis.DataSet(scope.nodes.map mapNode)
        edges: new vis.DataSet(scope.edges.map mapEdge)

      console.log data
      console.log element

      div = angular.element '<div></div>'
      options =
        physics:
          barnesHut:
            gravitationalConstant: -8000
            springLength: 150
        nodes:
          shape: 'dot'
          font:
            size: 12
            face: 'Sans Serif'
        edges:
          color:
            inherit: 'from'
          smooth:
            type: 'continuous'
        height: '400px'

      network = new vis.Network(div[0], data, options)
      element.append div
]