#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

ARG HADOOP_VERSION=2.8.5
FROM apachekylin/kylin-ci-hadoop-base:hadoop_${HADOOP_VERSION}

ARG HADOOP_HISTORY_PORT=8188
ENV HADOOP_HISTORY_PORT ${HADOOP_HISTORY_PORT}
EXPOSE ${HADOOP_HISTORY_PORT}
EXPOSE 10020

HEALTHCHECK CMD curl -f http://localhost:${HADOOP_HISTORY_PORT}/ || exit 1

ENV YARN_CONF_yarn_timeline___service_leveldb___timeline___store_path=/hadoop/yarn/timeline
RUN mkdir -p /hadoop/yarn/timeline
VOLUME /hadoop/yarn/timeline

ADD run_history.sh /run_history.sh
RUN chmod a+x /run_history.sh

CMD ["/run_history.sh"]
