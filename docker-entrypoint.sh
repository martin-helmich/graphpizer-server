#!/bin/bash

set -x

if [ ! -d ${GRAPHPIZER_DATA} ] ; then
    mkdir -p "${GRAPHPIZER_DATA}"
fi

chown -R graphpizer ${GRAPHPIZER_DATA}

if [ -z "${GRAPHPIZER_OPTS}" ] ; then
    GRAPHPIZER_OPTS="-DapplyEvolutions.default=true"
fi

if [ "$1" = "graphpizer-server" ] ; then
    shift 1
    su -s /bin/sh -c "/opt/graphpizer-server-${GRAPHPIZER_VERSION}/bin/graphpizer-server ${GRAPHPIZER_OPTS} $@" - graphpizer
fi