export interface AqoeousRelationshipOptions {
    sourceDatas: Array<string>;
    paramName: string;
    /**
     * 如果为 stringArray  则会使用逗号分割成string数组
     * 如果为 longArray    则会使用逗号分割成Long数组
     * 如果为 intArray     则会使用逗号分割成Integer数组
     */
    sourceType: string;
}

export interface AqoeousOptions {

    /**
     * 返回map中的key名称,如果为空,则设置为qoName名称
     */

    resultName?: string;
    /**
     * qo名称
     */
    qoName: string;
    /**
     * qoJson数据,查询的参数值
     */
    qoJson?: Object;
    /**
     * 子级
     */
    qoChilds?: [AqoeousOptions];

    /**
     * 关联关系
     */
    relationships:[AqoeousRelationshipOptions];

    /**
     * 将数据合并到父级,该qo在子级时才生效,合并到父级使用的字段名为 resultName
     *
     */
    mergeParent: boolean;

    /**
     * mergeParent 为 true 时生效,适合无分页
     * <br>
     * 将首条记录合并到父级,该qo在子级时才生效,合并到父级使用的字段名为 resultName
     */
    listFirstMergeParent : boolean;
    /**
     * mergeParent 为 true 时生效,适合无分页
     * <br>
     *  listFirstMergeParent 启用后该功能失效
     *
     */
    listMergeParent : boolean;

    /**
     * 仅仅显示包含的字段,如果该字段设置了值, excludeFieldList 将失效
     */
    includeFieldList: [string];
    /**
     * 返回值 排除一些字段
     * 先进行包含设置,再进行排除
     */
    excludeFieldList: [string];
}

export interface aqoeousQueryObjectChild {
    resultName: string;
    sourceData: string;
}
