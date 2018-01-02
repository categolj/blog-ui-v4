#!/bin/sh
fly -t home sp -p blog-blog-ui \
    -c `dirname $0`/pipeline.yml \
    -l `dirname $0`/../../credentials.yml