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

package com.webank.wedatasphere.dss.workflow.io.export.impl;


import com.webank.wedatasphere.dss.common.entity.IOType;
import com.webank.wedatasphere.dss.common.entity.Resource;
import com.webank.wedatasphere.dss.common.entity.node.DSSEdge;
import com.webank.wedatasphere.dss.common.entity.node.DSSEdgeDefault;
import com.webank.wedatasphere.dss.common.entity.node.DSSNode;
import com.webank.wedatasphere.dss.common.entity.node.Node;
import com.webank.wedatasphere.dss.common.exception.DSSErrorException;
import com.webank.wedatasphere.dss.common.label.DSSLabel;
import com.webank.wedatasphere.dss.common.utils.IoUtils;
import com.webank.wedatasphere.dss.common.utils.ZipHelper;
import com.webank.wedatasphere.dss.standard.app.sso.Workspace;
import com.webank.wedatasphere.dss.workflow.common.entity.DSSFlow;
import com.webank.wedatasphere.dss.workflow.common.entity.DSSFlowRelation;
import com.webank.wedatasphere.dss.workflow.common.parser.WorkFlowParser;
import com.webank.wedatasphere.dss.workflow.constant.DSSWorkFlowConstant;
import com.webank.wedatasphere.dss.workflow.dao.FlowMapper;
import com.webank.wedatasphere.dss.workflow.dao.NodeInfoMapper;
import com.webank.wedatasphere.dss.workflow.entity.NodeInfo;
import com.webank.wedatasphere.dss.workflow.io.export.MetaExportService;
import com.webank.wedatasphere.dss.workflow.io.export.NodeExportService;
import com.webank.wedatasphere.dss.workflow.io.export.WorkFlowExportService;
import com.webank.wedatasphere.dss.workflow.service.BMLService;
import com.webank.wedatasphere.dss.workflow.service.DSSFlowService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.webank.wedatasphere.dss.workflow.constant.DSSWorkFlowConstant.NODE_EXPORT_IMPORT_TIMEOUT_MINUTES;
import static com.webank.wedatasphere.dss.workflow.scheduler.DssJobThreadPool.nodeExportThreadPool;


@Service
public class WorkFlowExportServiceImpl implements WorkFlowExportService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private BMLService bmlService;

    @Autowired
    private WorkFlowParser workFlowParser;

    @Autowired
    private NodeExportService nodeExportService;

    @Autowired
    private MetaExportService metaExportService;

    @Autowired
    private FlowMapper flowMapper;

    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    @Autowired
    private DSSFlowService flowService;


    @Override
    public String exportFlowInfo(Long dssProjectId, String projectName, long rootFlowId, String userName, Workspace workspace, List<DSSLabel> dssLabels) throws Exception {
        //??????rootFlow,????????????Flow
        DSSFlow rootFlow = flowMapper.selectFlowByID(rootFlowId);
        List<DSSFlow> dssFlowList = new ArrayList<>();
        //??????rootflow????????????flow
        dssFlowList.add(rootFlow);
        getAllDssFlowsByRootflowId(rootFlow, dssFlowList);
        //??????rootflow????????????flow???Relations
        List<Long> flowIds = dssFlowList.stream().map(DSSFlow::getId).collect(Collectors.toList());
        List<DSSFlowRelation> flowRelations = flowIds.isEmpty() ? new ArrayList<>() : flowMapper.listFlowRelation(flowIds);
        String flowExportSaveBasePath = IoUtils.generateIOPath(userName, projectName, "");
        //?????????????????????project??????
        IoUtils.generateIOType(IOType.FLOW, flowExportSaveBasePath);
        //????????????????????????
        IoUtils.generateIOEnv(flowExportSaveBasePath);
        metaExportService.exportFlowBaseInfo(dssFlowList, flowRelations, flowExportSaveBasePath);
        logger.info(userName + "-????????????Flow???" + rootFlow.getName());
        List<DSSFlow> dssFlows = new ArrayList<>();

        for (DSSFlow dssFlow : dssFlowList) {
            if (dssFlow.getRootFlow()) {
                String savePath = flowExportSaveBasePath + File.separator + dssFlow.getName() + File.separator + dssFlow.getName() + ".json";
                //???????????????json??????
                String flowJson = downloadFlowJsonFromBml(userName, dssFlow.getResourceId(), dssFlow.getBmlVersion(), savePath);
                if (!dssFlow.getHasSaved()) {
                    logger.info("?????????{}????????????????????????", dssFlow.getName());
                } else if (StringUtils.isNotBlank(flowJson)) {
                    exportFlowResources(userName, dssProjectId, projectName, flowExportSaveBasePath, flowJson, dssFlow.getName(), workspace, dssLabels);
                    exportAllSubFlows(userName, dssFlow, dssProjectId, projectName, flowExportSaveBasePath, workspace, dssLabels);
                    dssFlows.add(dssFlow);
                } else {
                    String warnMsg = String.format(DSSWorkFlowConstant.PUBLISH_FLOW_REPORT_FORMATE, dssFlow.getName(), dssFlow.getBmlVersion());
                    logger.info(warnMsg);
                    throw new DSSErrorException(90033, warnMsg);
                }
            }
        }
        if (dssFlows.isEmpty()) {
            throw new DSSErrorException(90037, "???????????????????????????????????????,?????????????????????????????????");
        }
        //??????????????????
        return ZipHelper.zipExportProject(flowExportSaveBasePath);
    }


    private void exportAllSubFlows(String userName, DSSFlow dssFlowParent, Long projectId, String projectName,
                                   String projectExportBasePath, Workspace workspace, List<DSSLabel> dssLabels) throws Exception {
        List<? extends DSSFlow> subFlows = dssFlowParent.getChildren();
        if (subFlows != null) {
            for (DSSFlow subFlow : subFlows) {
                String savePath = projectExportBasePath + File.separator + subFlow.getName() + File.separator + subFlow.getName() + ".json";
                //?????????flow???json??????
                String flowJson = downloadFlowJsonFromBml(userName, subFlow.getResourceId(), subFlow.getBmlVersion(), savePath);
                if (!subFlow.getHasSaved()) {
                    logger.info("?????????{}????????????????????????", subFlow.getName());
                } else if (StringUtils.isNotBlank(flowJson)) {
                    exportFlowResources(userName, projectId, projectName, projectExportBasePath, flowJson, subFlow.getName(), workspace, dssLabels);
                    exportAllSubFlows(userName, subFlow, projectId, projectName, projectExportBasePath, workspace, dssLabels);
                } else {
                    String warnMsg = String.format(DSSWorkFlowConstant.PUBLISH_FLOW_REPORT_FORMATE, subFlow.getName(), subFlow.getBmlVersion());
                    logger.info(warnMsg);
                    throw new DSSErrorException(90014, warnMsg);
                }
            }
        }
    }


    private String genWorkFlowExportDir(String projectExportPath, String flowName) {
        return projectExportPath + File.separator + flowName;
    }

    @Override
    public void exportFlowResources(String userName, Long projectId, String projectName,
                                    String projectSavePath, String flowJson, String flowName,
                                    Workspace workspace, List<DSSLabel> dssLabels) throws Exception {
        String workFlowExportPath = genWorkFlowExportDir(projectSavePath, flowName);
        String workFlowResourceSavePath = workFlowExportPath + File.separator + "resource";
        String appConnResourceSavePath = workFlowExportPath + File.separator + "appconn-resource";
        if (StringUtils.isNotEmpty(workFlowExportPath)) {
            //???????????????????????????
            List<Resource> resources = workFlowParser.getWorkFlowResources(flowJson);
            if (resources != null) {
                resources.forEach(resource -> {
                    downloadFlowResourceFromBml(userName, resource, workFlowResourceSavePath);
                });
            }

            //?????????????????????????????????,???????????????appconn??????
            List<DSSNode> nodes = workFlowParser.getWorkFlowNodes(flowJson);
            if (nodes != null) {
                for (DSSNode node : nodes) {
                    nodeExportService.downloadNodeResourceToLocal(userName, node, workFlowResourceSavePath);
                    NodeInfo nodeInfo = nodeInfoMapper.getWorkflowNodeByType(node.getNodeType());
                    if (Boolean.TRUE.equals(nodeInfo.getSupportJump()) && nodeInfo.getJumpType() == 1) {
                        logger.info("node.getJobContent() is :{}", node.getJobContent());
                        nodeExportService.downloadAppConnResourceToLocal(userName, projectId, projectName, node, appConnResourceSavePath, workspace, dssLabels);
                    }
                }
            }

        } else {
            throw new DSSErrorException(90067, "?????????????????????????????????");
        }
    }

    public void exportFlowResources_for_multi_thread(String userName, Long projectId, String projectName,
                                                     String projectSavePath, String flowJson, String flowName,
                                                     Workspace workspace, List<DSSLabel> dssLabels) throws Exception {
        String workFlowExportPath = genWorkFlowExportDir(projectSavePath, flowName);
        String workFlowResourceSavePath = workFlowExportPath + File.separator + "resource";
        String appConnResourceSavePath = workFlowExportPath + File.separator + "appconn-resource";
        if (StringUtils.isNotEmpty(workFlowExportPath)) {
            //???????????????????????????
            List<Resource> resources = workFlowParser.getWorkFlowResources(flowJson);
            if (resources != null) {
                resources.forEach(resource -> {
                    downloadFlowResourceFromBml(userName, resource, workFlowResourceSavePath);
                });
            }

            //?????????????????????????????????,???????????????appconn??????
            List<DSSNode> nodes = workFlowParser.getWorkFlowNodes(flowJson);
            if (CollectionUtils.isNotEmpty(nodes)) {
                List<DSSEdge> edges = workFlowParser.getWorkFlowEdges(flowJson);
                Map<String, DSSNode> waitingNodes = nodes.stream().collect(Collectors.toConcurrentMap(Node::getId, t -> t));
                Set<String> completedNodes = Collections.synchronizedSet(new HashSet<>());
                List<String> failedNodes = Collections.synchronizedList(new ArrayList<>());
                CountDownLatch countDownLatch = new CountDownLatch(nodes.size());
                List<FutureTask> futureTaskList = Collections.synchronizedList(new ArrayList<>());
//                Map.Entry<String, DSSNode> entry;
                while (!waitingNodes.isEmpty()) {
                    //????????????????????????????????????????????????????????????
                    if (!failedNodes.isEmpty()) {
                        for (FutureTask futureTask : futureTaskList) {
                            futureTask.cancel(true);
                        }
                        throw new DSSErrorException(90070, "?????????????????????????????????: " + failedNodes);
                    }
                    for (Map.Entry<String, DSSNode> entry : waitingNodes.entrySet()) {
//                        entry = stringDSSNodeEntry;
                        String nodeId = entry.getKey();
                        DSSNode node = entry.getValue();
                        //todo ??????
                        Set<String> dependNodes = edges.stream().filter(l -> l.getTarget().equals(nodeId)).map(DSSEdge::getSource).collect(Collectors.toSet());
                        boolean canSubmit = true;
                        if (!dependNodes.isEmpty()) {
                            for (String n : dependNodes) {
                                if (!completedNodes.contains(n)) {
                                    canSubmit = false;
                                    break;
                                }
                            }
                        }
                        if (canSubmit) {
                            FutureTask<Boolean> futureTask = new FutureTask<>(() -> {
                                boolean result = false;
                                try {
                                    nodeExportService.downloadNodeResourceToLocal(userName, node, workFlowResourceSavePath);
                                    NodeInfo nodeInfo = nodeInfoMapper.getWorkflowNodeByType(node.getNodeType());
                                    if (Boolean.TRUE.equals(nodeInfo.getSupportJump()) && nodeInfo.getJumpType() == 1) {
                                        logger.info("node.getJobContent() is :{}", node.getJobContent());
                                        nodeExportService.downloadAppConnResourceToLocal(userName, projectId, projectName, node, appConnResourceSavePath, workspace, dssLabels);
                                    }
                                    completedNodes.add(nodeId);
                                    result = true;
                                } catch (Exception e) {
                                    failedNodes.add(node.getName());
                                    logger.error("failed to export node:{}", node.getName(), e);
                                } finally {
                                    countDownLatch.countDown();
                                }
                                return result;
                            });
                            waitingNodes.remove(nodeId);
                            nodeExportThreadPool.submit(futureTask);
                            futureTaskList.add(futureTask);
                        }
                    }
                    Thread.sleep(10L);
                }
                boolean success = false;
                try {
                    success = countDownLatch.await(NODE_EXPORT_IMPORT_TIMEOUT_MINUTES.getValue(), TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    logger.error("failed to export node for workflow:{}", flowName, e);
                    throw new DSSErrorException(90071, "?????????????????????");
                }
                if (!failedNodes.isEmpty()) {
                    throw new DSSErrorException(90070, "?????????????????????????????????: " + failedNodes);
                }
            }

        } else {
            throw new DSSErrorException(90067, "?????????????????????????????????");
        }
    }

    @Override
    public String downloadFlowJsonFromBml(String userName, String resourceId, String version, String savePath) {
        return bmlService.downloadAndGetFlowJson(userName, resourceId, version, savePath);
    }

    private String downloadFlowResourceFromBml(String userName, Resource resource, String savePath) {
        String flowResourcePath = savePath + File.separator + resource.getResourceId() + ".re";
        return bmlService.downloadToLocalPath(userName, resource.getResourceId(), resource.getVersion(), flowResourcePath);
    }

    private void getAllDssFlowsByRootflowId(DSSFlow parentFlow, List<DSSFlow> flowList) {
        List<Long> subFlowIds = flowMapper.selectSavedSubFlowIDByParentFlowID(parentFlow.getId());
        if (subFlowIds.size() > 0) {
            for (Long subFlowId : subFlowIds) {

                DSSFlow subDssFlow = flowMapper.selectFlowByID(subFlowId);
                if (!subDssFlow.getHasSaved()) {
                    logger.info("?????????{}????????????????????????", subDssFlow.getName());
                } else {
                    flowList.add(subDssFlow);
                    parentFlow.addChildren(subDssFlow);
                    getAllDssFlowsByRootflowId(subDssFlow, flowList);
                }
            }
        }
    }
}
