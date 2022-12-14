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

package com.webank.wedatasphere.dss.framework.project.restful;

import com.webank.wedatasphere.dss.common.utils.DSSExceptionUtils;
import com.webank.wedatasphere.dss.framework.project.entity.DSSProjectDO;
import com.webank.wedatasphere.dss.framework.project.entity.request.ProjectCreateRequest;
import com.webank.wedatasphere.dss.framework.project.entity.request.ProjectDeleteRequest;
import com.webank.wedatasphere.dss.framework.project.entity.request.ProjectModifyRequest;
import com.webank.wedatasphere.dss.framework.project.entity.request.ProjectQueryRequest;
import com.webank.wedatasphere.dss.framework.project.entity.response.ProjectResponse;
import com.webank.wedatasphere.dss.framework.project.entity.vo.DSSProjectVo;
import com.webank.wedatasphere.dss.framework.project.exception.DSSProjectErrorException;
import com.webank.wedatasphere.dss.framework.project.service.DSSFrameworkProjectService;
import com.webank.wedatasphere.dss.framework.project.service.DSSProjectService;
import com.webank.wedatasphere.dss.framework.project.utils.ApplicationArea;
import com.webank.wedatasphere.dss.standard.app.sso.Workspace;
import com.webank.wedatasphere.dss.standard.sso.utils.SSOHelper;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequestMapping(path = "/dss/framework/project", produces = {"application/json"})
@RestController
public class DSSFrameworkProjectRestfulApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(DSSFrameworkProjectRestfulApi.class);
    @Autowired
    DSSFrameworkProjectService dssFrameworkProjectService;
    @Autowired
    private DSSProjectService projectService;
    @Autowired
    private DSSProjectService dssProjectService;

    /**
     * ????????????????????????????????????
     *
     * @param request
     * @param projectRequest
     * @return
     */
    @RequestMapping(path = "getAllProjects", method = RequestMethod.POST)
    public Message getAllProjects(HttpServletRequest request, @RequestBody ProjectQueryRequest projectRequest) {
        String username = SecurityFilter.getLoginUsername(request);
        projectRequest.setUsername(username);
        List<ProjectResponse> dssProjectVos = projectService.getListByParam(projectRequest);
        Message message = Message.ok("?????????????????????????????????").data("projects", dssProjectVos);
        return message;
    }

    /**
     * ????????????,???????????????AppConn??????????????????????????????????????????????????????appconn??????????????????
     */
    @RequestMapping(path = "createProject", method = RequestMethod.POST)
    public Message createProject(HttpServletRequest request, @RequestBody ProjectCreateRequest projectCreateRequest) throws Exception {
        String username = SecurityFilter.getLoginUsername(request);
        Workspace workspace = SSOHelper.getWorkspace(request);
        //????????????????????????????????????????????????
        if (!projectCreateRequest.getEditUsers().contains(username)) {
            projectCreateRequest.getEditUsers().add(username);
        }
        List<String> releaseUsers = projectCreateRequest.getReleaseUsers();
        if (releaseUsers == null) {
            releaseUsers = new ArrayList<>();
            projectCreateRequest.setReleaseUsers(releaseUsers);
        }
        if (!releaseUsers.contains(username)) {
            releaseUsers.add(username);
        }
        DSSProjectVo dssProjectVo = dssFrameworkProjectService.createProject(projectCreateRequest, username, workspace);
        if (dssProjectVo != null) {
            return Message.ok("??????????????????").data("project", dssProjectVo);
        } else {
            return Message.error("??????????????????");
        }
    }

    /**
     * ????????????
     *
     * @param request
     * @param projectModifyRequest
     * @return
     */
    @RequestMapping(path = "modifyProject", method = RequestMethod.POST)
    public Message modifyProject(HttpServletRequest request, @RequestBody ProjectModifyRequest projectModifyRequest) throws Exception {
        String username = SecurityFilter.getLoginUsername(request);
        Workspace workspace = SSOHelper.getWorkspace(request);
        DSSProjectDO dbProject = dssProjectService.getProjectById(projectModifyRequest.getId());
        //???????????????
        if (dbProject == null) {
            LOGGER.error("{} project id is null, can not modify", projectModifyRequest.getName());
            DSSExceptionUtils.dealErrorException(60021,
                    String.format("%s project id is null, can not modify", projectModifyRequest.getName()), DSSProjectErrorException.class);
        }
        String createUsername = dbProject.getUsername();
        //????????????????????????????????????????????????
        if (!projectModifyRequest.getEditUsers().contains(createUsername)) {
            projectModifyRequest.getEditUsers().add(createUsername);
        }
        List<String> releaseUsers = projectModifyRequest.getReleaseUsers();
        if (releaseUsers == null) {
            releaseUsers = new ArrayList<>();
            projectModifyRequest.setReleaseUsers(releaseUsers);
        }
        if (!releaseUsers.contains(createUsername)) {
            releaseUsers.add(createUsername);
        }
        dssFrameworkProjectService.modifyProject(projectModifyRequest, dbProject, username, workspace);
        return Message.ok("??????????????????");
    }

    /**
     * ????????????
     *
     * @param request
     * @param projectDeleteRequest
     * @return
     */
    @RequestMapping(path = "deleteProject", method = RequestMethod.POST)
    public Message deleteProject(HttpServletRequest request, @RequestBody ProjectDeleteRequest projectDeleteRequest) throws Exception {
        String username = SecurityFilter.getLoginUsername(request);
        Workspace workspace = SSOHelper.getWorkspace(request);
        // ????????????????????????????????????
        projectService.isDeleteProjectAuth(projectDeleteRequest.getId(), username);
        projectService.deleteProject(username, projectDeleteRequest, workspace);
        return Message.ok("??????????????????");
    }

    @RequestMapping(path = "listApplicationAreas", method = RequestMethod.GET)
    public Message listApplicationAreas(HttpServletRequest req) {
        String header = req.getHeader("Content-language").trim();
        ApplicationArea[] applicationAreas = ApplicationArea.values();
        List<String> areas = new ArrayList<>();
        Arrays.stream(applicationAreas).forEach(item -> {
            if ("zh-CN".equals(header)) {
                areas.add(item.getName());
            } else {
                areas.add(item.getEnName());
            }
        });
        return Message.ok().data("applicationAreas", areas);
    }

    @RequestMapping(path = "getProjectAbilities", method = RequestMethod.GET)
    public Message getProjectAbilities(HttpServletRequest request) {
        //?????????????????????????????????????????? ??????  ?????????
        String username = SecurityFilter.getLoginUsername(request);
        try {
            List<String> projectAbilities = projectService.getProjectAbilities(username);
            return Message.ok("????????????????????????").data("projectAbilities", projectAbilities);
        } catch (final Throwable t) {
            LOGGER.error("failed to get project ability for user {}", username, t);
            return Message.error("????????????????????????");
        }
    }


    /**
     * ??????????????????????????????
     */
    @RequestMapping(path = "getDeletedProjects", method = RequestMethod.POST)
    public Message getDeletedProjects(HttpServletRequest request, @Valid @RequestBody ProjectQueryRequest projectRequest) {
        String username = SecurityFilter.getLoginUsername(request);
        projectRequest.setUsername(username);
        List<ProjectResponse> dssProjectVos = projectService.getDeletedProjects(projectRequest);
        Message message = Message.ok("??????????????????????????????????????????").data("projects", dssProjectVos);
        return message;
    }


}
