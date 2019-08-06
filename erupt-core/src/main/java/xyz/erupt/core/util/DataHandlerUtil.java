package xyz.erupt.core.util;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceEnum;
import xyz.erupt.core.bean.EruptFieldModel;
import xyz.erupt.core.bean.EruptModel;
import xyz.erupt.core.bean.TreeModel;
import xyz.erupt.core.service.CoreService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by liyuepeng on 2019-04-28.
 */
public class DataHandlerUtil {
    //内存计算的方式生成树结构
    public static List<TreeModel> treeModelToTree(List<TreeModel> treeModels) {
        List<TreeModel> resultTreeModels = new ArrayList<>();
        List<TreeModel> tempTreeModels = new LinkedList<>();
        tempTreeModels.addAll(treeModels);
        for (TreeModel treeModel : treeModels) {
            if (StringUtils.isBlank(treeModel.getPid())) {
                resultTreeModels.add(treeModel);
                tempTreeModels.remove(treeModel);
            }
        }
        for (TreeModel treeModel : resultTreeModels) {
            recursionTree(tempTreeModels, treeModel);
        }
        //TODO 如果最终结果size为0则直接返回原有参数
        return resultTreeModels;
    }

    private static void recursionTree(List<TreeModel> treeModels, TreeModel parentTreeModel) {
        List<TreeModel> childrenModel = new ArrayList<>();
        List<TreeModel> tempTreeModels = new LinkedList<>();
        tempTreeModels.addAll(treeModels);
        for (TreeModel treeModel : treeModels) {
            if (treeModel.getPid().equals(parentTreeModel.getId())) {
                childrenModel.add(treeModel);
                tempTreeModels.remove(treeModel);
                if (childrenModel.size() > 0) {
                    recursionTree(tempTreeModels, treeModel);
                }
            }
            parentTreeModel.setChildren(childrenModel);
        }
    }


    //清理序列化后对象所产生的默认值（通过json串进行校验）
    public static void clearObjectDefaultValueByJson(Object obj, JsonObject data) {
        ReflectUtil.findClassAllFields(obj.getClass(), field -> {
            try {
                field.setAccessible(true);
                if (null != field.get(obj)) {
                    if (!data.has(field.getName())) {
                        field.set(obj, null);
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }

    public static void convertDataToEruptView(EruptModel eruptModel, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (null != entry.getValue()) {
                String key = entry.getKey();
                if (entry.getKey().contains("_")) {
                    key = entry.getKey().split("_")[0];
                }
                EruptFieldModel fieldModel = eruptModel.getEruptFieldMap().get(key);
                Edit edit = fieldModel.getEruptField().edit();
                switch (edit.type()) {
                    case CHOICE:
                        if (edit.choiceType().type() == ChoiceEnum.SELECT_SINGLE || edit.choiceType().type() == ChoiceEnum.RADIO) {
                            map.put(entry.getKey(), fieldModel.getChoiceMap().get(entry.getValue().toString()));
                        }
                        break;
                    case BOOLEAN:
                        map.put(entry.getKey(), (Boolean) entry.getValue() ? edit.boolType().trueText() : edit.boolType().falseText());
                        break;
                    case REFERENCE_TREE:
                    case REFERENCE_TABLE:
                    case COMBINE:
                        for (View view : fieldModel.getEruptField().views()) {
                            String[] _keys = entry.getKey().split("_");
                            if (view.column().equals(_keys[_keys.length - 1])) {
                                EruptFieldModel vef = CoreService.getErupt(fieldModel.getFieldReturnName()).
                                        getEruptFieldMap().get(view.column());
                                switch (vef.getEruptField().edit().type()) {
                                    case CHOICE:
                                        if (vef.getEruptField().edit().choiceType().type() == ChoiceEnum.SELECT_SINGLE
                                                || vef.getEruptField().edit().choiceType().type() == ChoiceEnum.RADIO) {
                                            map.put(entry.getKey(), vef.getChoiceMap().get(entry.getValue().toString()));
                                        }
                                        break;
                                    case BOOLEAN:
                                        map.put(entry.getKey(), (Boolean) entry.getValue() ?
                                                vef.getEruptField().edit().boolType().trueText() :
                                                vef.getEruptField().edit().boolType().falseText());
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        break;
                }
            }
        }
    }
}