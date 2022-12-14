package com.webank.wedatasphere.dss.framework.admin.service.impl;

import com.webank.wedatasphere.dss.framework.admin.pojo.entity.DssProxyUser;
import com.webank.wedatasphere.dss.framework.admin.service.DssProxyUserService;
import com.webank.wedatasphere.dss.framework.admin.xml.DSSProxyUserMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static com.webank.wedatasphere.dss.framework.admin.conf.AdminConf.DS_PROXY_SELF_ENABLE;

@Service
public class DssProxyUserServiceImpl implements DssProxyUserService {
    @Resource
    DSSProxyUserMapper dssProxyUserMapper;

    @Override
    public List<DssProxyUser> selectProxyUserList(String userName) {
        return dssProxyUserMapper.selectProxyUserList(userName);
    }

    @Override
    public List<String> getProxyUserNameList(String userName) {
        List<DssProxyUser> userList = dssProxyUserMapper.selectProxyUserList(userName);;
        List<String> proxyUserNameList=userList.stream().map(dssProxyUser -> dssProxyUser.getProxyUserName()).collect(Collectors.toList());
        return  proxyUserNameList;
    }

    @Override
    public int insertProxyUser(DssProxyUser dssProxyUser) {
        int rows = dssProxyUserMapper.insertUser(dssProxyUser);
        return rows;
    }

    @Override
    public boolean isExists(String userName, String proxyUserName) {
        List<DssProxyUser> res= dssProxyUserMapper.getProxyUserList(userName,proxyUserName);
        if(DS_PROXY_SELF_ENABLE.getValue() && userName.equalsIgnoreCase(proxyUserName)){
           return true;
        }else if(res.size()==0){
            return false;
        }else {
            return true;
        }
    }
}
