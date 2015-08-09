define ['angular', '../Application', 'vis'], (angular, app, vis) ->
  app.directive 'visGraph', ['$window', ($window) ->
    restrict: 'E'
    scope:
      nodes: '=nodes'
      edges: '=edges'
    link: (scope, element, attrs) ->

      console.log attrs

      height = attrs.height || 400

      labels = []
      labelToNumber = (label) ->
        i = labels.indexOf label
        if i >= 0 then i
        else
          labels.push label
          labels.length - 1

      groupFromNode = (node) ->
        if node['package']? then labelToNumber node.package
        else if node['labels']? and node.labels.length > 0 then labelToNumber node.labels[0]
        else if node['type']? then labelToNumber(node.type)
        else 0

      labelFromProperties = (rec, id) ->
        if rec['name']? then rec['name']
        else if rec['title']? then rec['title']
        else if rec['fqcn']? then rec['fqcn']
        else if rec['properties']? labelFromProperties(rec['properties'], id)
        else id

      titleFromNode = (node) ->
        if node['labels']? then str = "<div><strong>#{node.labels[0]}</strong><table>"
        else if node['type']? then str = "<div><strong>#{node.type} #{node.fqcn}</strong><table>"
        else str = "<div><strong>Unknown node</strong><table>"

        if node['properties']?
          for key of node.properties
            str += "<tr><th>#{key}</th><td>#{node.properties[key]}</td></tr>"

        str += "</table></div>"
        str

      mapNode = (node) ->
        id: node['id']
        title: titleFromNode node
        label: labelFromProperties node
        group: groupFromNode node
      mapEdge = (edge) ->
        from: edge['from']
        to: edge['to']
        label: edge['label']
        font:
          align: 'middle'
        arrows:
          to: true

      div = angular.element '<div></div>'
      element.append div

      draw = (nodes, edges) ->
        nodes = nodes || []
        edges = edges || []

        data =
          nodes: new vis.DataSet(nodes.map mapNode)
          edges: new vis.DataSet(edges.map mapEdge)

        console.log data
        console.log element

        options =
          interaction:
            hideEdgesOnDrag: true
          physics:
            stabilization: false
            barnesHut:
              gravitationalConstant: -80000
              springLength: 200
              springConstant: 0.01
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
          height: height

        network = new vis.Network(div[0], data, options)

      scope.$watch 'nodes', (newNodes) -> draw(newNodes, scope.edges)
      scope.$watch 'edges', (newEdges) -> draw(scope.nodes, newEdges)

      draw(scope.nodes, scope.edges)
]