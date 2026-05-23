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
#npm run dev --port=8083 --target=http://gitlab.shoukaiseki.cn:10731
#npm run dev --port=8083 --target=http://test6.shoukaiseki.cn:8080
#npm run dev --port=8083 --target=http://test6.shoukaiseki.cn:10777
npm run dev --port=28765 --target=http://test6.shoukaiseki.cn:8765

