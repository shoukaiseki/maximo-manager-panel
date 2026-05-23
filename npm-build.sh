#!/bin/bash -
#===============================================================================
#
#          FILE: npm-run-dev.sh
#
#         USAGE: ./npm-run-dev.sh
#
#   DESCRIPTION:
#
#       OPTIONS: ---
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: YOUR NAME (),
#  ORGANIZATION:
#       CREATED: 2020年12月24日 14时49分16秒
#      REVISION:  ---
#===============================================================================

set -o nounset                              # Treat unset variables as an error
npm run build:prod
tar zcvf /tmp/win/wb-codeGenerator-dist.tar.gz dist/

