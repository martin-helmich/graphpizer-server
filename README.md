GraPHPizer: Analytics engine for PHP source code
================================================

**Disclaimer**: This project is actively developed and by no means stable. It is completely undocumented. I refuse to
take any responsibility for any kind of amok this program might do on your computer and to provide any kind of support.

What is GraPHPizer?
-------------------

GraPHPizer uses graph technologies to offer a source code analytics engine for PHP projects. This includes nifty
graph visualizations like the following one:

![Example of GraPHPizer graph visualization](documentation/graphpizer-demo.png)

Features include:

- Graph visualisations (~~completely useless~~ marginally useful, but they look pretty)
- Search your code base using the graph query language *Cypher*
- Automatic generation of UML diagrams from PHP code
- Type inference

Installation
------------

### Production (Docker)

The recommended way to install GraPHPizer is using [Docker](https://www.docker.com). Use the following command to
create and run a new GraPHPizer container:

    docker run --name graphpizer -d -p 9000:9000 -v /var/lib/graphizer martinhelmich/graphpizer-server:latest

### Production (manual)

Use one of the [binary distributions](https://github.com/martin-helmich/graphpizer-server/releases). Alternatively,
compile using the following steps:

    tbd

### Development quickstart

1. Clone this repository:

        git clone git://github.com/martin-helmich/graphizer-server.git

2. Build using Activator:

        cd graphizer-server
        activator run
