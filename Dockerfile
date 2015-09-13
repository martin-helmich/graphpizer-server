FROM alpine:3.2
MAINTAINER Martin Helmich <kontakt@martin-helmich.de>

ENV GRAPHPIZER_VERSION=1.0.0-dev
ENV GRAPHPIZER_HOME=/opt/graphpizer-server-${GRAPHPIZER_VERSION}

RUN apk add --update openjdk7-jre-base
RUN apk add --update bash
RUN adduser -h ${GRAPHPIZER_HOME} -s /sbin/nologin -D -H graphpizer
ADD target/universal/graphpizer-server-${GRAPHPIZER_VERSION}.tgz /opt

VOLUME /var/lib/graphpizer
RUN mkdir -p /var/lib/graphpizer && chown -R graphpizer /var/lib/graphpizer

# Can't use USER here because Docker is TOO FUCKING STUPID to realize that
# volumes are ALWAYS OWNED BY FUCKING ROOT making running containers as USER
# COMPLETELY FUCKING-USELESS! So thanks to this I now have to write a FUCKING
# entrypoint script JUST TO SET THE FUCKING USER RIGHTS CORRECTLY and have to
# RUN MY FUCKING CONTAINER AS FUCKING ROOT!!!
# USER graphpizer

WORKDIR /opt/graphpizer-server
ADD docker-entrypoint.sh /opt/docker-entrypoint.sh

ENTRYPOINT ["/opt/docker-entrypoint.sh"]

ENV GRAPHPIZER_DATA=/var/lib/graphpizer
CMD ["graphpizer-server"]
