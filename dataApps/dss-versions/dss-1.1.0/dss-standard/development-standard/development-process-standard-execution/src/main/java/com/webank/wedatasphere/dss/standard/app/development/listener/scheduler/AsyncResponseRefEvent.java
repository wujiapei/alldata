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

package com.webank.wedatasphere.dss.standard.app.development.listener.scheduler;

import com.webank.wedatasphere.dss.standard.app.development.listener.ref.AsyncExecutionResponseRef;
import org.apache.linkis.common.listener.Event;


public class AsyncResponseRefEvent implements Event {

    private AsyncExecutionResponseRef response;
    private long lastAskTime = 0;

    public AsyncResponseRefEvent(AsyncExecutionResponseRef response) {
        this.response = response;
    }

    public AsyncExecutionResponseRef getResponse() {
        return response;
    }

    public void setResponse(AsyncExecutionResponseRef response) {
        this.response = response;
    }

    public long getLastAskTime() {
        return lastAskTime;
    }

    public void setLastAskTime() {
        this.lastAskTime = System.currentTimeMillis();
    }
}
