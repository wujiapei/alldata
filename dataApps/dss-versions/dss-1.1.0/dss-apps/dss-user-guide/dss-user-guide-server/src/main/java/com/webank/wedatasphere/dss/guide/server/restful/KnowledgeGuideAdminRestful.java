package com.webank.wedatasphere.dss.guide.server.restful;

import com.webank.wedatasphere.dss.guide.server.conf.GuideConf;
import com.webank.wedatasphere.dss.guide.server.entity.GuideCatalog;
import com.webank.wedatasphere.dss.guide.server.entity.GuideChapter;
import com.webank.wedatasphere.dss.guide.server.service.GuideCatalogService;
import com.webank.wedatasphere.dss.guide.server.service.GuideChapterService;
import com.webank.wedatasphere.dss.guide.server.service.GuideGroupService;
import com.webank.wedatasphere.dss.guide.server.util.FileUtils;
import com.webank.wedatasphere.dss.guide.server.util.ShellUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.linkis.common.utils.Utils;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(path = "/dss/guide/admin", produces = {"application/json"})
@AllArgsConstructor
public class KnowledgeGuideAdminRestful {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGuideAdminRestful.class);

    private GuideCatalogService guideCatalogService;
    private GuideChapterService guideChapterService;

    private GuideGroupService guideGroupService;

    private final static String SUMMARY = "SUMMARY.md";

    private final static String SHELL_COMMAND_HOST_IP = "hostname -i";

    private final static String MODEL_GITBOOK_SYNC = "gitbook";


    /**
     * ?????????????????????
     */
    @RequestMapping(path = "/guidecatalog", method = RequestMethod.POST)
    public Message saveGuideCatalog(HttpServletRequest request, @RequestBody GuideCatalog guideCatalog) {
        String userName = SecurityFilter.getLoginUsername(request);
        if (null == guideCatalog.getId()) {
            guideCatalog.setCreateBy(userName);
            guideCatalog.setCreateTime(new Date(System.currentTimeMillis()));
        } else {
            guideCatalog.setUpdateBy(userName);
            guideCatalog.setUpdateTime(new Date(System.currentTimeMillis()));
        }

        boolean flag = guideCatalogService.saveGuideCatalog(guideCatalog);
        if (flag) {
            return Message.ok("????????????");
        } else {
            return Message.error("????????????");
        }
    }

    @RequestMapping(path = "/guidecatalog/{id}/delete", method = RequestMethod.POST)
    public Message deleteGroup(@PathVariable Long id) {
        guideCatalogService.deleteGuideCatalog(id);
        Message message = Message.ok("????????????");
        return message;
    }

    @RequestMapping(path = "/guidecatalog/top", method = RequestMethod.GET)
    public Message queryGuideCatalogListForTop() {
        return Message.ok().data("result", guideCatalogService.queryGuideCatalogListForTop());
    }

    @RequestMapping(path = "/guidecatalog/{id}/detail", method = RequestMethod.GET)
    public Message queryGuideCatalogDetailById(@PathVariable Long id) {
        return Message.ok().data("result", guideCatalogService.queryGuideCatalogDetailById(id));
    }


    /**
     * ?????????????????????
     */
    @RequestMapping(path = "/guidechapter", method = RequestMethod.POST)
    public Message saveGuideChapter(HttpServletRequest request, @RequestBody GuideChapter guideChapter) {
        String userName = SecurityFilter.getLoginUsername(request);
        if (null == guideChapter.getId()) {
            guideChapter.setCreateBy(userName);
            guideChapter.setCreateTime(new Date(System.currentTimeMillis()));
        } else {
            guideChapter.setUpdateBy(userName);
            guideChapter.setUpdateTime(new Date(System.currentTimeMillis()));
        }

        boolean flag = guideChapterService.saveGuideChapter(guideChapter);
        if (flag) {
            return Message.ok("????????????");
        } else {
            return Message.error("????????????");
        }
    }

    @RequestMapping(path = "/guidechapter/{id}/delete", method = RequestMethod.POST)
    public Message deleteGuideChapter(@PathVariable Long id) {
        guideChapterService.deleteGuideChapter(id);
        Message message = Message.ok("????????????");
        return message;
    }

    @RequestMapping(path = "/guidechapter/{id}", method = RequestMethod.GET)
    public Message queryGuideChapter(@PathVariable Long id) {
        return Message.ok().data("result", guideChapterService.queryGuideChapterById(id));
    }

    @RequestMapping(path = "/guidechapter/uploadImages", method = RequestMethod.POST)
    public Message multFileUpload(@RequestParam(required = true) List<MultipartFile> files) {
        if (null == files || files.size() == 0) {
            return Message.error("??????????????????");
        }

        List<Map<String, Object>> totalResult = new ArrayList<Map<String, Object>>();
        // ?????????????????????????????????????????????
        final String localPath = GuideConf.GUIDE_CHAPTER_IMAGES_PATH.getValue();
        for (MultipartFile file : files) {
            Map<String, Object> result = new HashMap<String, Object>();
            String result_msg = "";

            if (file.getSize() > 5 * 1024 * 1024) {
                result_msg = "????????????????????????5M";
            } else {
                //????????????????????????
                String fileType = file.getContentType();
                if (fileType.equals("image/jpeg") || fileType.equals("image/png") || fileType.equals("image/jpg")) {
                    //???????????????
                    String fileName = file.getOriginalFilename();
                    //?????????????????????
                    String suffixName = fileName.substring(fileName.lastIndexOf("."));
                    //?????????????????????
                    fileName = "knowledge-" + UUID.randomUUID() + suffixName;
                    if (FileUtils.upload(file, localPath, fileName)) {
                        String relativePath = fileName;
                        result.put("relativePath", relativePath);
                        result_msg = "??????????????????";
                    } else {
                        result_msg = "??????????????????";
                    }
                } else {
                    result_msg = "?????????????????????";
                }
            }
            result.put("result_msg", result_msg);
            totalResult.add(result);
        }
        return Message.ok().data("result", totalResult);
    }

    @RequestMapping(path = "/guidechapter/uploadImage", method = RequestMethod.POST)
    public Message fileUpload(@RequestParam(required = true) MultipartFile file) {
        if (null == file) {
            return Message.error("??????????????????");
        }

        final String imagesPath = GuideConf.GUIDE_CHAPTER_IMAGES_PATH.getValue();

        if (file.getSize() > 5 * 1024 * 1024) {
            return Message.error("????????????????????????5M");
        } else {
            //????????????????????????
            String fileType = file.getContentType();
            if (fileType.equals("image/jpeg") || fileType.equals("image/png") || fileType.equals("image/jpg")) {
                //???????????????
                String fileName = file.getOriginalFilename();
                //?????????????????????
                String suffixName = fileName.substring(fileName.lastIndexOf("."));
                //?????????????????????
                fileName = UUID.randomUUID() + suffixName;
                if (FileUtils.upload(file, imagesPath, fileName)) {
                    return Message.ok().data("result", fileName);
                } else {
                    return Message.error("??????????????????");
                }
            } else {
                return Message.error("?????????????????????");
            }
        }

    }

    @PostConstruct
    public void syncKnowledge() {
        final String summaryPath = GuideConf.HOST_GITBOOK_PATH.getValue() + File.separator + SUMMARY;
        final String savePath = GuideConf.TARGET_GITBOOK_PATH.getValue() + File.separator + "gitbook_books";
        final String scpCommand = "scp -r "
                + " hadoop@" + GuideConf.HOST_IP_ADDRESS.getValue() + ":"
                + GuideConf.HOST_GITBOOK_PATH.getValue() + " "
                + GuideConf.TARGET_GITBOOK_PATH.getValue();
        String delMkdir = "rm -rf " + savePath;
        logger.info("????????????????????????...");
        Utils.defaultScheduler().scheduleAtFixedRate(() -> {
            try {
                if (StringUtils.equals(GuideConf.GUIDE_SYNC_MODEL.getValue(), MODEL_GITBOOK_SYNC)) {
                    String hostIp = ShellUtils.callShellQuery(SHELL_COMMAND_HOST_IP);
                    //????????????????????????????????????????????????
                    if (!StringUtils.equals(hostIp, GuideConf.HOST_IP_ADDRESS.getValue())) {
                        //????????????????????????
                        boolean flag = FileUtils.fileExist(savePath);
                        if (flag) {
                            //????????????
                            ShellUtils.callShellByExec(delMkdir);
                        }
                        //???????????????????????????
                        ShellUtils.callShellByExec(scpCommand);
                    }
                }else {
                    guideCatalogService.syncKnowledge(summaryPath, GuideConf.SUMMARY_IGNORE_MODEL.getValue());
                    guideGroupService.asyncGuide(summaryPath, GuideConf.SUMMARY_IGNORE_MODEL.getValue());
                }
            } catch (Exception e) {
                logger.error("???????????????????????????" + e);
                throw new RuntimeException(e);
            }
            logger.info("?????????????????????????????????");
        }, 0, 2, TimeUnit.HOURS);
    }

}
