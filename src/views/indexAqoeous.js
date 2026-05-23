

export function qrCodeInventoryLotTableQueryObjectList(params){
    const queryObjectList=[
        {
            resultName: "mainPage",
            qoName: 'getProTaskOrderByInventoryLotQO',
            // mergeParent: true,
            // listFirstMergeParent: true,
            qoJson:params,
        }
    ]
    return queryObjectList;
}
