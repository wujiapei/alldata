package com.webank.wedatasphere.dss.data.common.atlas;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.internal.LinkedTreeMap;
import com.webank.wedatasphere.dss.data.common.conf.AtlasConf;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.lineage.AtlasLineageInfo;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service("atlasService")
public class AtlasService {
    private AtlasClient atlasClient;
    private final Gson gson;

    public AtlasService() {
        String[] urls = new String[]{AtlasConf.ATLAS_REST_ADDRESS.getValue()};
        String[] basicAuthUsernamePassword = new String[]{AtlasConf.ATLAS_USERNAME.getValue(), AtlasConf.ATLAS_PASSWORD.getValue()};
        atlasClient = new AtlasClient(urls, basicAuthUsernamePassword);

        GsonBuilder builder = new GsonBuilder();
        // Register an adapter to manage the date types as long values
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return new Date(json.getAsJsonPrimitive().getAsLong());
            }
        });

        gson = builder.create();
    }

    /**
     * hive db??????
     */
    public long getHiveDbCnt() throws AtlasServiceException {
        String jsonStr = atlasClient.getHiveDbs();
        if (StringUtils.isNotEmpty(jsonStr)) {
            JsonObject jsonObject = gson.fromJson(jsonStr, JsonObject.class);
            if (jsonObject != null) {
                return jsonObject.get("count").getAsLong();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * hive table??????
     */
    public long getHiveTableCnt() throws AtlasServiceException {
        String jsonStr = atlasClient.getHiveTables();
        if (StringUtils.isNotEmpty(jsonStr)) {
            JsonObject jsonObject = gson.fromJson(jsonStr, JsonObject.class);
            if (jsonObject != null) {
                return jsonObject.get("count").getAsLong();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }


    /**
     * ?????????????????????hive table
     */
    public List<AtlasEntityHeader> searchHiveTable(String classification, String query,
                                                   boolean excludeDeletedEntities, int limit, int offset) throws AtlasServiceException {
        String jsonStr = atlasClient.basicSearchForString("hive_table", classification, query, excludeDeletedEntities, limit, offset);
        AtlasSearchResult atlasSearchResult = gson.fromJson(jsonStr, AtlasSearchResult.class);

        return atlasSearchResult.getEntities();
    }

    /**
     * ??????hive table??????
     */
    public AtlasEntity getHiveTblByGuid(String guid) throws AtlasServiceException {
        String jsonStr = atlasClient.getEntityByGuidForString(guid, false, false);

        AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = gson.fromJson(jsonStr, AtlasEntity.AtlasEntityWithExtInfo.class);

        return atlasEntityWithExtInfo.getEntity();
    }

    /**
     * ??????hive table??????
     */
    public AtlasEntity getHiveTblByAttribute(Map<String, String> uniqAttributes, boolean minExtInfo, boolean ignoreRelationship) throws AtlasServiceException {
        String jsonStr = atlasClient.getEntityByAttributeForString("hive_table", uniqAttributes, minExtInfo, ignoreRelationship);

        AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = gson.fromJson(jsonStr, AtlasEntity.AtlasEntityWithExtInfo.class);

        return atlasEntityWithExtInfo.getEntity();
    }

    /**
     * ??????hive column??????
     */
    public AtlasEntity getHiveColumn(String guid) throws AtlasServiceException {
        String jsonStr = atlasClient.getEntityByGuidForString(guid, true, true);

        AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = gson.fromJson(jsonStr, AtlasEntity.AtlasEntityWithExtInfo.class);

        return atlasEntityWithExtInfo.getEntity();
    }

    /**
     * ????????????hive column??????
     */
    public List<AtlasEntity> getHiveColumnsByGuids(List<String> guids) throws AtlasServiceException {
        String jsonStr = atlasClient.getEntitiesByGuidsForString(guids, true, true);
        AtlasEntity.AtlasEntitiesWithExtInfo atlasEntitiesWithExtInfo = gson.fromJson(jsonStr, AtlasEntity.AtlasEntitiesWithExtInfo.class);

        return atlasEntitiesWithExtInfo.getEntities();
    }


    /**
     * ?????????????????????
     */
    public void modifyComment(String guid, String commentStr) throws AtlasServiceException {
        atlasClient.modifyComment(guid, commentStr);
    }

    /**
     * ?????????????????????
     */
    public void setLabels(String guid, Set<String> labels) throws AtlasServiceException {
        atlasClient.setLabels(guid, labels);
    }

    /**
     * ?????????????????????
     */
    public void removeLabels(String guid, Set<String> labels) throws AtlasServiceException {
        atlasClient.removeLabels(guid, labels);
    }

    /**
     * ????????????
     */
    public AtlasLineageInfo getLineageInfo(final String guid, final AtlasLineageInfo.LineageDirection direction, final int depth) throws AtlasServiceException {
        String jsonStr = atlasClient.getLineageInfoForString(guid, direction, depth);

        AtlasLineageInfo atlasLineageInfo = gson.fromJson(jsonStr, AtlasLineageInfo.class);

        return atlasLineageInfo;
    }

    /**
     * ??????guid?????????hive tbl????????? db.table
     */
    public String getHiveTblNameById(String guid) throws AtlasServiceException {
        String jsonStr = atlasClient.getHeaderByIdForString(guid);
        AtlasEntityHeader atlasEntityHeader = gson.fromJson(jsonStr, AtlasEntityHeader.class);

        return atlasEntityHeader.getAttribute("qualifiedName").toString().split("@")[0];
    }

    /**
     * ??????guid?????????hive tbl?????? ??? ???????????????????????????????????????????????????
     */
    public Map<String, Object> getHiveTblAttributesByGuid(String guid) throws AtlasServiceException {
        Map<String, Object> result = new HashMap<>(4);

        String jsonStr = atlasClient.getEntityByGuidForString(guid, false, false);
        AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = gson.fromJson(jsonStr, AtlasEntity.AtlasEntityWithExtInfo.class);

        result.put("tblName", atlasEntityWithExtInfo.getEntity().getAttribute("qualifiedName").toString().split("@")[0]);
        result.put("isPartition", ((List) atlasEntityWithExtInfo.getEntity().getAttribute("partitionKeys")).size() > 0);
        result.put("tableType",atlasEntityWithExtInfo.getEntity().getAttribute("tableType"));
        Map<String,Object> sdMap = (LinkedTreeMap)atlasEntityWithExtInfo.getEntity().getRelationshipAttribute("sd");
        if(null != sdMap) {
            result.put("location",atlasEntityWithExtInfo.getReferredEntities().get(sdMap.get("guid")).getAttribute("location"));
        }
        return result;
    }

    /**
     * ?????????????????????
     */
    public AtlasTypesDef getClassificationDef() throws AtlasServiceException {
        String jsonStr = atlasClient.getClassificationDefForString();
        AtlasTypesDef atlasTypesDef = gson.fromJson(jsonStr, AtlasTypesDef.class);

        return atlasTypesDef;
    }

    /**
     * ????????????????????????
     */
    public AtlasClassificationDef getClassificationDefByName(String name) throws AtlasServiceException {
        String jsonStr = atlasClient.getClassificationDefByNameForString(name);
        AtlasClassificationDef atlasClassificationDef = gson.fromJson(jsonStr, AtlasClassificationDef.class);

        return atlasClassificationDef;
    }

    /**
     * ??????????????????????????????????????????
     */
    public List<AtlasClassificationDef> getClassificationDefListByName(String name) throws AtlasServiceException {
        List<AtlasClassificationDef> atlasClassificationDefList = new ArrayList<AtlasClassificationDef>();

        AtlasClassificationDef atlasClassificationDef = this.getClassificationDefByName(name);
        //atlasClassificationDefList.add(atlasClassificationDef);

        Set<String> subTypes = atlasClassificationDef.getSubTypes();
        subTypes.forEach(item -> {
            try {
                atlasClassificationDefList.add(this.getClassificationDefByName(item));
            } catch (AtlasServiceException exception) {
                exception.printStackTrace();
            }

        });

        return atlasClassificationDefList;
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????
     */
    public List<AtlasClassificationDef> getClassificationDefListForLayer() throws AtlasServiceException {
        List<AtlasClassificationDef> atlasClassificationDefList = new ArrayList<AtlasClassificationDef>();
        // ????????????????????????????????????
        atlasClassificationDefList.addAll(this.getClassificationDefListByName(AtlasConf.ATLAS_CLASSIFICATION_LAYER_SYSTEM.getValue()));

        // ???????????????????????????????????????
        atlasClassificationDefList.addAll(this.getClassificationDefListByName(AtlasConf.ATLAS_CLASSIFICATION_LAYER_USER.getValue()));

        return atlasClassificationDefList;
    }

    /**
     * ????????????????????????
     */
    public AtlasTypesDef createAtlasTypeDefs(final AtlasTypesDef typesDef) throws AtlasServiceException {
        String jsonStr = atlasClient.createAtlasTypeDefsForString(typesDef);
        AtlasTypesDef atlasTypesDef = gson.fromJson(jsonStr, AtlasTypesDef.class);

        return atlasTypesDef;
    }

    /**
     * ?????????????????????
     */
    public void updateAtlasTypeDefs(final AtlasTypesDef typesDef) throws AtlasServiceException {
        atlasClient.updateAtlasTypeDefsV2(typesDef);
    }

    /**
     * ????????????????????????(???????????????Enum-Struct-Classification-Entity-Relationship-BusinessMetadata)
     */
    public void deleteTypedefByName(String name) throws AtlasServiceException {
        atlasClient.deleteTypedefByName(name);
    }

    /**
     * ?????????????????????
     */
    public void addClassifications(String guid, List<AtlasClassification> classifications) throws AtlasServiceException {
        atlasClient.addClassifications(guid, classifications);
    }

    /**
     * ?????????????????????
     */
    public void deleteClassification(String guid, String classificationName) throws AtlasServiceException {
        atlasClient.deleteClassification(guid, classificationName);
    }

    /**
     * ?????????????????????
     */
    public void deleteClassifications(String guid, List<AtlasClassification> classifications) throws AtlasServiceException {
        atlasClient.deleteClassifications(guid, classifications);
    }

    /**
     * ?????????????????????
     */
    public void updateClassifications(String guid, List<AtlasClassification> classifications) throws AtlasServiceException {
        atlasClient.updateClassifications(guid, classifications);
    }

    /**
     * ???????????????????????????????????????????????????
     */
    public void removeAndAddClassifications(String guid, List<AtlasClassification> newClassifications) throws AtlasServiceException {
        AtlasClassificationV2.AtlasClassificationsV2 oldAtlasClassifications = this.getClassifications(guid);
        if (oldAtlasClassifications != null && oldAtlasClassifications.getList() != null) {
            oldAtlasClassifications.getList().stream().forEach(ele -> {
                try {
                    this.deleteClassification(guid, ele.getTypeName());
                } catch (AtlasServiceException exception) {
                    exception.printStackTrace();
                }
            });
        }
        if (newClassifications != null && newClassifications.size() > 0) {
            atlasClient.addClassifications(guid, newClassifications);
        }
    }


    /**
     * ?????????????????????
     */
    public AtlasClassificationV2.AtlasClassificationsV2 getClassifications(String guid) throws AtlasServiceException {
        String jsonStr = atlasClient.getClassificationsForString(guid);
        // org.codehaus.jackson.map.JsonMappingException: Conflicting getter definitions for property "propagate"
        AtlasClassificationV2.AtlasClassificationsV2 atlasClassifications = gson.fromJson(jsonStr, AtlasClassificationV2.AtlasClassificationsV2.class);

        return atlasClassifications;
    }
}
