/*
 * Copyright 2019 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.webank.wedatasphere.dss.orchestrator.loader;

import com.webank.wedatasphere.dss.common.label.DSSLabel;
import com.webank.wedatasphere.dss.orchestrator.core.DSSOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class OrchestratorManager {

    private final Map<String, DSSOrchestrator> cacheDssOrchestrator = new ConcurrentHashMap<>();

    @Autowired
    private DefaultOrchestratorLoader defaultOrchestratorLoader;

    public DSSOrchestrator getOrCreateOrchestrator(String userName,
                                                   String workspaceName,
                                                   String typeName,
                                                   List<DSSLabel> dssLabels) {
        String findKey = getCacheKey(userName, workspaceName, typeName);
        DSSOrchestrator dssOrchestrator = cacheDssOrchestrator.get(findKey);
        if (null == dssOrchestrator) {
            synchronized (cacheDssOrchestrator)  {
                dssOrchestrator = cacheDssOrchestrator.get(findKey);
                if(null == dssOrchestrator) {
                    dssOrchestrator = defaultOrchestratorLoader.loadOrchestrator(userName, workspaceName, typeName, dssLabels);
                    cacheDssOrchestrator.put(findKey, dssOrchestrator);
                }
            }
        }
        return dssOrchestrator;
    }

    protected String getCacheKey(String userName, String workspaceName, String typeName) {
        return userName + "_" + workspaceName + "_" + typeName;
    }
}
