#!/bin/bash

#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.

. "/spark/sbin/spark-config.sh"

. "/spark/bin/load-spark-env.sh"


export SPARK_HOME=/opt/spark
export PRESTO_CLI_CMD="/usr/local/bin/presto --server presto-coordinator-1:8090"
export TRINO_CLI_CMD="/usr/local/bin/trino --server trino-coordinator-1:8091"

date
echo "SPARK HOME is : $SPARK_HOME"
echo "PRESTO CLI CMD is : $PRESTO_CLI_CMD"
echo "TRINO CLI CMD is : $TRINO_CLI_CMD"

tail -f /dev/null
