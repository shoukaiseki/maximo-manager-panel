/**
 * Maximo Lookup 定义表(数据来源: Maximo 系统库 LOOKUPS.xml)
 *
 * 结构说明:
 *   objectname   列表数据来源对象(服务端未解析到时会返回错误提示)
 *   selectmode   single/multiple, Maximo 中的选择方式
 *   orderby      默认排序(listOrder 属性可覆盖)
 *   whereclause  默认 where(listWhere 属性可覆盖)
 *   relationship 关系名(关系型 lookup, 由调用方传入父对象数据后使用)
 *   columns      列定义: a=dataattribute(字段名/关系路径), l=label(标题),
 *                w=width(宽度), k=true 表示该列为关键列(默认 srcKeys),
 *                s=false 不可排序, f=false 不可过滤, h=true 显示列过滤框
 */
export const LOOKUP_MAP = {
  "action": {objectname:"ACTION",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"action",k:true},{a:"description"},{a:"objectname"}]},
  "actiongroup": {objectname:"ACTIONGROUP",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"action",l:"Action Group",k:true},{a:"description"}]},
  "actualci": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"actcinum",k:true},{a:"description"}]},
  "adapterlookup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"adaptername",k:true}]},
  "address": {objectname:"ADDRESS",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"addresscode",k:true},{a:"description"},{a:"orgid"}]},
  "alllists": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"listname",k:true}]},
  "alnattrlookup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"value",k:true},{a:"description"}]},
  "alndomain": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"value",k:true},{a:"description"}]},
  "altkeyindexes": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"name",k:true}]},
  "amcraftskill": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"craft",k:true},{a:"skilllevel"},{a:"description"},{a:"orgid"},{a:"vendor"},{a:"contractnum"}]},
  "amcrewcontract": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"vendor",k:true},{a:"contractnum"},{a:"revisionnum"},{a:"status"},{a:"orgid"}]},
  "amcrewmanitem": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"itemnum",l:"Tool",k:true},{a:"description"},{a:"commoditygroup"},{a:"commodity"}]},
  "amcrewposition": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"position",k:true},{a:"craft"},{a:"skilllevel"},{a:"CRAFTSKILL.description",s:false},{a:"remarks"},{a:"orgid"}]},
  "amcrewreport": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"amcrew",k:true},{a:"description"},{a:"amcrewtype"},{a:"vendor"},{a:"contractnum"},{a:"orgid"}]},
  "amcrews": {selectmode:"single",inputmode:"readonly",orderby:"amcrew asc",whereclause:"",relationship:"",columns:[{a:"amcrew",k:true},{a:"description"},{a:"amcrewtype"},{a:"vendor"},{a:"contractnum"},{a:"orgid"}]},
  "amcrewtoolsq": {objectname:"AMCREWTOOLSQ",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"toolseq",k:true}]},
  "amcrewtype": {selectmode:"single",inputmode:"readonly",orderby:"amcrewtype asc",whereclause:"",relationship:"",columns:[{a:"amcrewtype",k:true},{a:"description"},{a:"orgid"}]},
  "amcrewunrestrictedtoolsq": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"toolseq",k:true},{a:"itemnum"},{a:"item.description"},{a:"orgid"}]},
  "amlaborcraftskill": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"laborcode",k:true},{a:"labor.person.displayname"},{a:"craft"},{a:"skilllevel"},{a:"vendor"},{a:"CRAFTSKILL.description"},{a:"contractnum"}]},
  "apilinkrel": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"relation",s:false,k:true},{a:"relsqlwhere",s:false},{a:"dest",s:false}]},
  "appdoctype": {objectname:"APPDOCTYPE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"appdoctype",k:true},{a:"description"}]},
  "appintobjects": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"intobjectname",k:true},{a:"maxintobject.description"}]},
  "application": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"app",k:true},{a:"description"}]},
  "apptbookname": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"apptbookname",k:true},{a:"description"},{a:"workzone"},{a:"orgid"}]},
  "articles": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"articleid",k:true},{a:"description"}]},
  "asset": {objectname:"ASSET",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"assetnum",k:true},{a:"description"},{a:"location"},{a:"siteid"}]},
  "assetattribute": {objectname:"ASSETATTRIBUTE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"assetattrid",k:true},{a:"description"},{a:"datatype"},{a:"measureunitid"},{a:"orgid"},{a:"siteid"}]},
  "assetfeature": {objectname:"ASSETFEATURE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"feature",k:true},{a:"label"},{a:"startmeasure"},{a:"endmeasure"}]},
  "assetlrm": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"lrm",k:true},{a:"LINEARREFMETHOD.description",s:false}]},
  "assets_saadvsearch": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"addresscode",k:true},{a:"description"},{a:"streetaddress"},{a:"city"},{a:"stateprovince"},{a:"postalcode"}]},
  "astspecattribute": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"assetattrid",k:true},{a:"description"}]},
  "attnameandtype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"objectname",k:true},{a:"attributename"},{a:"maxtype"}]},
  "attributename": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"attributename",k:true},{a:"title"}]},
  "autokey": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"autokeyname",k:true}]},
  "autoscript": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"autoscript",k:true},{a:"description"}]},
  "billtoshiptoaddress": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"addresscode",k:true},{a:"address.description"},{a:"orgid"},{a:"siteid"}]},
  "bimattributemap": {objectname:"BIMATTRIBUTEMAP",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"mapname",k:true},{a:"description"}]},
  "bimfilter": {objectname:"BIMFILTER",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"filtername",k:true},{a:"description"},{a:"siteid"}]},
  "binnum": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"binnum",k:true},{a:"lotnum"},{a:"conditioncode"},{a:"curbal"},{a:"siteid"}]},
  "birtreport": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:" runtype in( select value from synonymdomain where synonymdomain.maxvalue in ('BIRT') and synonymdomain.domainid in ('REPORTTYPES') )",relationship:"",columns:[{a:"description",k:true},{a:"reportname"},{a:"reporttoapp.description",l:"Application"}]},
  "birtsecuritygroups": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"groupname",k:true},{a:"description"}]},
  "budget": {objectname:"BUDGET",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"budget",k:true},{a:"description"}]},
  "budgetaxis": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"objectname",k:true},{a:"description"},{a:"condition"}]},
  "calendar": {objectname:"CALENDAR",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"calnum",k:true},{a:"description"},{a:"orgid"}]},
  "calendarshift": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"calnum",k:true},{a:"calendar.description"},{a:"shiftnum"},{a:"orgid"}]},
  "cfgobjects": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"intobjectname",k:true},{a:"description"}]},
  "ci": {objectname:"CI",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"cinum",k:true},{a:"description"}]},
  "classification": {objectname:"CLASSIFICATION",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"classificationid",k:true},{a:"description"},{a:"orgid"},{a:"siteid"}]},
  "classspec": {objectname:"CLASSSPEC",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"assetattrid",k:true},{a:"section"}]},
  "classstructure": {objectname:"CLASSSTRUCTURE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"classstructureid",k:true},{a:"description"},{a:"classificationid"},{a:"PARENT.classificationid",l:"Parent Classification",s:false},{a:"siteid",s:false}]},
  "classstructureid": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"classstructureid",k:true},{a:"description"},{a:"classificationid"},{a:"siteid"}]},
  "collection": {objectname:"COLLECTION",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"collectionnum",k:true},{a:"description"}]},
  "columns": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"columnname",k:true}]},
  "commodities": {objectname:"COMMODITIES",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"commodity",l:"Commodity",k:true},{a:"description"},{a:"parent",l:"Group"}]},
  "commoditycode": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"commodity",l:"Commodity Code",k:true},{a:"description"}]},
  "commoditygroup": {selectmode:"single",inputmode:"readonly",orderby:"commodity asc",whereclause:"",relationship:"",columns:[{a:"commodity",l:"Commodity Group",k:true},{a:"description"}]},
  "commtmplt": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"templateid",k:true},{a:"description"}]},
  "companies": {objectname:"COMPANIES",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"company",k:true},{a:"name"},{a:"type"},{a:"orgid"}]},
  "compcontact": {objectname:"COMPCONTACT",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"contact",k:true},{a:"position"},{a:"voicephone"},{a:"orgid"}]},
  "computersystemnodename": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"nodename",k:true}]},
  "conditioncode": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"conditioncode",k:true},{a:"description"},{a:"condrate"},{a:"itemsetid"}]},
  "connectionpoollist": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"extsysname",k:true},{a:"value"}]},
  "contract": {objectname:"CONTRACT",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"contractnum",k:true},{a:"description"},{a:"contracttype"},{a:"vendor"},{a:"orgid"}]},
  "contractasset": {objectname:"CONTRACTASSET",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"assetnum",k:true},{a:"description"},{a:"location"},{a:"siteid"}]},
  "contractauth": {objectname:"CONTRACTAUTH",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"contractnum",l:"Reference Contract",k:true},{a:"contract.description",s:false},{a:"contract.contracttype",s:false},{a:"vendor"},{a:"orgid"}]},
  "countbookitem": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"itemnum",k:true},{a:"location"}]},
  "craft": {objectname:"CRAFT",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"craft",k:true},{a:"description"},{a:"orgid"}]},
  "craftcontract": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"vendor",k:true},{a:"contractnum"},{a:"revisionnum"},{a:"status"},{a:"orgid"}]},
  "craftonly": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"craft",k:true},{a:"craft.description"},{a:"orgid"}]},
  "craftrate": {objectname:"CRAFTRATE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"craft",k:true},{a:"skilllevel"},{a:"description"},{a:"vendor"},{a:"contractnum"},{a:"orgid"}]},
  "craftratenovendor": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"craft",k:true},{a:"skilllevel"},{a:"description"},{a:"standardrate"},{a:"orgid"}]},
  "craftrateskill": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"skilllevel",k:true},{a:"description"},{a:"vendor"},{a:"contractnum"},{a:"orgid"}]},
  "craftratestatus": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"craft",k:true},{a:"skilllevel"},{a:"description"},{a:"vendor"},{a:"contractnum"},{a:"contractrev.status",s:false},{a:"contractrev.startdate",s:false},{a:"contractrev.enddate",s:false},{a:"orgid",s:false}]},
  "craftskill": {objectname:"CRAFTSKILL",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"craft",k:true},{a:"skilllevel"},{a:"description"},{a:"orgid"}]},
  "craftskillvaluelist": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"skilllevel",k:true},{a:"description"},{a:"orgid"}]},
  "craftstandardrate": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"craft",k:true},{a:"skilllevel"},{a:"description"},{a:"vendor"},{a:"contractnum"},{a:"standardrate"},{a:"orgid"}]},
  "crewposition": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"position",k:true},{a:"craft"},{a:"skilllevel"},{a:"craftskill.description",s:false},{a:"remarks"},{a:"orgid"}]},
  "crewtemplate": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"crewtemplate",k:true},{a:"description"},{a:"crewtype"},{a:"orgid"}]},
  "crontask": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"crontaskname",k:true},{a:"description"}]},
  "crontaskinstance": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"instancename",k:true},{a:"description"},{a:"schedule"},{a:"active"}]},
  "currency": {objectname:"CURRENCY",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"currencycode",k:true},{a:"description"}]},
  "datelookup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[]},
  "defaultstartapps": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"app",k:true},{a:"description"}]},
  "defmethod": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"servicename",l:"Class name",k:true},{a:"methodname"}]},
  "deploymentgroups": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"cfgobjgroup",k:true},{a:"description"}]},
  "deploymentobjects": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"intobjectname",k:true},{a:"description"}]},
  "destinterfaces": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ifacename",k:true},{a:"description"}]},
  "disttarget": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"targetname",k:true},{a:"description"},{a:"type"},{a:"url"}]},
  "dmcollapps": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"app",k:true},{a:"description"}]},
  "dmcollection": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"collection",k:true},{a:"description"}]},
  "dmcollectionjob": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"collection",k:true},{a:"description"}]},
  "dmpackages": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"package",k:true}]},
  "dmpkgdef": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"pkgdefname",k:true},{a:"description",s:false}]},
  "docinfo": {objectname:"DOCINFO",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"document",k:true},{a:"description"}]},
  "doctype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"doctype",k:true},{a:"description"}]},
  "document": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"document",k:true},{a:"description"},{a:"urlname"},{a:"doctype"},{a:"urltype"}]},
  "domain": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"domainid",k:true},{a:"description"},{a:"domaintype"}]},
  "dpamadapter": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"adaptername",k:true}]},
  "dpamadaptermakemodel": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"adaptername",l:"Make/Model",k:true}]},
  "dpamadptvariant": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"adaptervariant",k:true}]},
  "dpammanufacturer": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"manufacturername",k:true}]},
  "dpammanufacturermanufacturer": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"manufacturername",l:"Manufacturer",k:true}]},
  "dpammanuvariant": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"manufacturervar",k:true}]},
  "dpamos": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"osname",k:true}]},
  "dpamosoperatingsystem": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"osname",l:"Operating System",k:true}]},
  "dpamosvariant": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"osvariant",k:true}]},
  "dpamprocessor": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"processorname",k:true}]},
  "dpamprocessormakemodel": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"processorname",l:"Make/Model",k:true}]},
  "dpamprocvariant": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"processorvar",k:true}]},
  "dpamsoftware": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"softwarename",k:true}]},
  "dpamsoftwareapplicationname": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"softwarename",l:"Application",k:true}]},
  "dpamswscomprequiredversionhigh": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"requiredversionhi",k:true}]},
  "dpamswscomprequiredversionlow": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"requiredversionlow",k:true}]},
  "dpamswscompsoftwarename": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"softwarename",k:true}]},
  "dpamswssuitename": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"suitename",k:true}]},
  "dpamswsversion": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"version",k:true}]},
  "dpamswusagerangedisplaytext": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"displaytext",k:true}]},
  "dpamswusagerangefrom": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"rangefrom",k:true}]},
  "dpamswusagerangefromusagecount": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"rangefrom",k:true},{a:"rangeto"},{a:"displaytext"},{a:"PARENT.swdetectiontool"}]},
  "dpamswusagerangeto": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"rangeto",k:true}]},
  "dpamswusageswdetectiontool": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"swdetectiontool",k:true}]},
  "dpamswvariant": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"softwarevariant",k:true}]},
  "eaudit": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:" entityname in (select tablename from maxtable where isaudittable = 1) ",relationship:"",columns:[{a:"entityname",k:true},{a:"description"}]},
  "emailfiletype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"LABEL",k:true},{a:"DESCRIPTION"}]},
  "emailtype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"DISPNAME",k:true},{a:"DESCRIPTION"}]},
  "endpoint": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"endpointname",k:true},{a:"handlername"},{a:"description"}]},
  "endpointname": {objectname:"MAXENDPOINTDTL",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"endpointname",k:true},{a:"description"}]},
  "escsitelookup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"siteid",k:true},{a:"description"},{a:"orgid"}]},
  "extcontrolacc": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"company",k:true},{a:"name"},{a:"orgid"}]},
  "extsysinifacelist": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ifacename",s:false,h:true,k:true},{a:"description",s:false,h:true}]},
  "extsysoutifacelist": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ifacename",s:false,h:true,k:true},{a:"description",s:false,h:true}]},
  "extsysoutifacetype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ifacetype",k:true},{a:"description"}]},
  "extsystem": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"extsysname",k:true},{a:"description"}]},
  "extsystemtype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ifacetype",k:true},{a:"description"},{a:"userdefined"}]},
  "failureclass": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"failurecode",l:"Failure Class",k:true},{a:"failurecode.description",s:false},{a:"orgid",s:false}]},
  "failurecode": {objectname:"FAILURECODE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"failurecode",k:true},{a:"description"},{a:"orgid"}]},
  "features": {objectname:"FEATURES",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"feature",k:true},{a:"description"},{a:"featuretype"}]},
  "fileformatdisplay": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"DISPNAME",k:true},{a:"DESCRIPTION"}]},
  "financialperiod": {objectname:"FINANCIALPERIODS",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"financialperiod",k:true},{a:"periodstart"},{a:"periodend"},{a:"orgid"}]},
  "fsnschemahw": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"strvalue",l:"Hardware Detection Tool",k:true}]},
  "fsnschemasw": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"strvalue",l:"Software Detection Tool",k:true}]},
  "glconfigure": {objectname:"GLCONFIGURE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"glaccountfield"},{a:"glorder",k:true},{a:"orgid"}]},
  "glnavigator": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"compvalue",k:true},{a:"comptext"},{a:"orgid"}]},
  "glnavigatorcoa": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"compvalue",k:true},{a:"comptext"},{a:"orgid"}]},
  "hazards": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"hazardid",k:true},{a:"description"},{a:"orgid"}]},
  "ibm_ProjectPack": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"projectcode",s:false,k:true},{a:"description"},{a:"priority"}]},
  "ibm_company": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"company",k:true},{a:"name"},{a:"type"},{a:"orgid"}]},
  "ibm_customer": {objectname:"IBM_CUSTOMER",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"custnum",k:true},{a:"description",l:"客户名称"}]},
  "ibm_customeradd": {objectname:"IBM_CUSTOMERADD",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ADDRNUM",w:70,s:false,k:true},{a:"DESCRIPTION",w:150},{a:"ADDRESS",w:80},{a:"IBM_CUSTOMER.DESCRIPTION",l:"客户",w:150}]},
  "ibm_line": {objectname:"IBM_LINE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"linenum",k:true},{a:"description",w:120},{a:"deliverypoint",w:80},{a:"portofdest",w:80},{a:"viapoint",w:80}]},
  "ibm_notify": {objectname:"IBM_NOTIFY",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"description",k:true},{a:"contact",w:80},{a:"address",w:150},{a:"phone",w:80}]},
  "ifacecontrol": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ifacecontrol",k:true},{a:"description"},{a:"controltype"}]},
  "ifaceoutintpoint": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"intpointname",k:true}]},
  "ifaceproc": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"procname",k:true},{a:"description"},{a:"proctype"}]},
  "ifacetypeslist": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ifacetype",k:true},{a:"description"}]},
  "inspectionform": {objectname:"INSPECTIONFORM",selectmode:"single",inputmode:"readonly",orderby:"NAME",whereclause:"",relationship:"",columns:[{a:"INSPFORMNUM",k:true},{a:"NAME"},{a:"REVISION"}]},
  "intfilelookup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"filename",k:true}]},
  "intobjects": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"intobjectname",k:true},{a:"description"}]},
  "intpoint": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"intpointname",k:true},{a:"description"}]},
  "invballot": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"lotnum",k:true},{a:"binnum"},{a:"conditioncode"},{a:"curbal"},{a:"UseBy"},{a:"siteid"}]},
  "invconditioncode": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"conditioncode",k:true},{a:"itemcondition.condrate"},{a:"binnum"},{a:"lotnum"},{a:"curbal"},{a:"siteid"}]},
  "invgroup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"invgroupnum",k:true},{a:"description"}]},
  "invlot": {objectname:"INVLOT",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"lotnum",k:true},{a:"mfglotnum"},{a:"itemnum"},{a:"siteid"}]},
  "invoice": {objectname:"INVOICE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"invoicenum",k:true},{a:"description"},{a:"status"},{a:"siteid"}]},
  "invoiceleaseasset": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"assetnum",k:true},{a:"description"}]},
  "invrescode": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"commodity",k:true},{a:"description"}]},
  "item": {objectname:"ITEM",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"itemnum",k:true},{a:"description"},{a:"commoditygroup"},{a:"commodity"}]},
  "itemsallwithitemset": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"itemnum",k:true},{a:"description"},{a:"itemtype"},{a:"rotating"},{a:"commoditygroup"},{a:"commodity"},{a:"itemsetid"}]},
  "itemset": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"setid",k:true},{a:"description"}]},
  "itemwithitemset": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"itemnum",k:true},{a:"description"},{a:"commoditygroup"},{a:"commodity"},{a:"itemsetid"}]},
  "jobplan": {objectname:"JOBPLAN",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"jpnum",k:true},{a:"pluscrevnum"},{a:"status"},{a:"description"},{a:"templatetype"},{a:"orgid"},{a:"siteid"}]},
  "jptask": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"jptask",s:false,k:true},{a:"description",s:false},{a:"siteid",s:false}]},
  "jpworktype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"worktype",k:true},{a:"wtypedesc"},{a:"orgid"}]},
  "jsonmapping": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"mapname",k:true},{a:"description"}]},
  "jsonmbopath": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"hierarchypath",s:false,k:true}]},
  "jsonobject": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"objectname",k:true},{a:"resourcename"}]},
  "jsonobjectpath": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"hierarchypath",s:false,k:true},{a:"objectname",s:false},{a:"parentobjname",s:false}]},
  "jsonpropname": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"propertyname",k:true}]},
  "jsonproppath": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"relproppath",k:true},{a:"relpropname"}]},
  "jsonresource": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"resourcename",k:true},{a:"description"}]},
  "jsonresourcetype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"resourcetype",k:true},{a:"description"}]},
  "kpimain": {objectname:"KPIMAIN",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"kpiname",k:true},{a:"description"}]},
  "labor": {objectname:"LABOR",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"laborcode",k:true},{a:"person.displayname",s:false},{a:"laborcraftratedefault.craft",s:false},{a:"laborcraftratedefault.skilllevel",s:false},{a:"laborcraftratedefault.vendor",s:false},{a:"laborcraftratedefault.contractnum",s:false},{a:"orgid"}]},
  "laborcodecraftskill": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"laborcode",k:true},{a:"labor.person.displayname",s:false},{a:"craft"},{a:"skilllevel"},{a:"orgid"}]},
  "laborcraftrate": {objectname:"LABORCRAFTRATE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"laborcode",k:true},{a:"labor.person.displayname",s:false},{a:"craft"},{a:"skilllevel"},{a:"vendor"},{a:"contractnum"},{a:"orgid"}]},
  "laborcraftskill": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"craft",k:true},{a:"skilllevel"},{a:"orgid"}]},
  "laborcraftvendor": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"vendor",k:true},{a:"companies.name"},{a:"orgid"}]},
  "laboronly": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"laborcode",k:true},{a:"person.displayname",s:false},{a:"orgid",s:false}]},
  "language": {objectname:"LANGUAGE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"languagename"},{a:"maxlangcode",k:true}]},
  "licensenum": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"licensenum",k:true},{a:"maxlicprdkeylist.productname"},{a:"licmetrictype"},{a:"licqty"}]},
  "linearasset": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"assetnum",k:true},{a:"description"},{a:"islinear"},{a:"location"},{a:"siteid"}]},
  "linearrefmethod": {objectname:"LINEARREFMETHOD",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"lrm",k:true},{a:"description"},{a:"measureunitid"},{a:"basemeasureunitid"},{a:"offsetmeasureunitid"},{a:"yoffsetmeasureunitid"},{a:"yoffsetref"},{a:"zoffsetmeasureunitid"},{a:"zoffsetref"}]},
  "lmolist": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"lmoname",k:true},{a:"lmonamespace"},{a:"description"}]},
  "locations": {objectname:"LOCATIONS",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"location",k:true},{a:"description"},{a:"type"},{a:"siteid"}]},
  "locations_saadvsearch": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"addresscode",k:true},{a:"description"},{a:"streetaddress"},{a:"city"},{a:"stateprovince"},{a:"postalcode"}]},
  "lockout": {objectname:"LOCKOUT",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"lockoutid",k:true},{a:"devicedescription"},{a:"siteid"}]},
  "lumgroupnamelookup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"groupname",k:true},{a:"maxgroup.description"}]},
  "malocci": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"assetnum",k:true},{a:"feature"},{a:"featurelabel"},{a:"startfeaturelabel",s:false},{a:"startoffset"},{a:"startmeasure"},{a:"startmeasureunitid"},{a:"endfeaturelabel",s:false},{a:"endoffset"},{a:"endmeasure"},{a:"endmeasureunitid"},{a:"sequence"}]},
  "masterpm": {objectname:"MASTERPM",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"masterpmnum",k:true},{a:"description"}]},
  "maxapps": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"app",k:true},{a:"description"}]},
  "maxattribute": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"attributename",k:true},{a:"objectname"}]},
  "maxattribute1": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"attributename",k:true},{a:"sourceelement"},{a:"objectname"}]},
  "maxattributeRemarks": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"attributename",k:true},{a:"remarks"}]},
  "maxcolumn": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"columnname",k:true},{a:"title"}]},
  "maxentity": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"entityname",k:true},{a:"description"}]},
  "maxextifaceout": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ifacename",k:true},{a:"MAXIFACEOUT.description"}]},
  "maxgroup": {objectname:"MAXGROUP",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"groupname",k:true},{a:"description"}]},
  "maxifacelist": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ifacename",k:true},{a:"description"}]},
  "maxlaunchentry": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"launchentryname",k:true},{a:"displayname"},{a:"consoleurl"}]},
  "maxlogger": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"logger",k:true},{a:"loglevel"},{a:"logkey"}]},
  "maxmenu": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"KEYVALUE",k:true},{a:"SIGOPTION.DESCRIPTION"}]},
  "maxmessages": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"msgkey",l:"Message Key",k:true},{a:"value",l:"Message Text"}]},
  "maxmodules": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"module",k:true},{a:"description"}]},
  "maxprop": {objectname:"MAXPROP",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"propname",k:true},{a:"description"}]},
  "maxservice": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"servicename",k:true},{a:"description",l:"Method Name"}]},
  "maxsysindexes": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"tbname",k:true},{a:"name"},{a:"unique"}]},
  "maxthreadloggerlist": {objectname:"MAXTHREADLOGGERLIST",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"contextname",s:false,k:true},{a:"description",s:false}]},
  "maxtype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"maxtype",k:true}]},
  "maxuser": {objectname:"MAXUSER",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"userid",k:true},{a:"PERSON.displayname",s:false}]},
  "maxvars": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"varname",k:true},{a:"varvalue"},{a:"orgid"},{a:"siteid"}]},
  "maxvartype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"varname",k:true},{a:"description"}]},
  "mborelation": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"RELATION",k:true},{a:"RELSQLWHERE"}]},
  "mbos": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"tbname",k:true}]},
  "meaqueuename": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"queuename",h:true,k:true}]},
  "measmetername": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"metername",k:true},{a:"description"},{a:"siteid"}]},
  "measpoint": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"pointnum",k:true},{a:"description"},{a:"siteid"}]},
  "measurementpoint": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"pointnum",k:true},{a:"description"},{a:"siteid"}]},
  "measureunit": {objectname:"MEASUREUNIT",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"MEASUREUNITID",k:true},{a:"ABBREVIATION"},{a:"DESCRIPTION"},{a:"siteid"}]},
  "membernode": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"title",k:true},{a:"description"}]},
  "messagetype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"messagetype",k:true}]},
  "meter": {objectname:"METER",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"metername",k:true},{a:"description"},{a:"metertype"}]},
  "metergroup": {objectname:"METERGROUP",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"groupname",k:true},{a:"description"}]},
  "metermetriclookup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"metricid",k:true},{a:"metrictype",s:false,h:true},{a:"deviceid"},{a:"devicename",s:false,h:true},{a:"devicetype",s:false,h:true}]},
  "metertypelist": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"value",l:"Meter Type",k:true},{a:"description"}]},
  "method": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"servicename",k:true},{a:"input",l:"Method Name"}]},
  "mmiparentctx": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"rescontext",k:true},{a:"description"}]},
  "mmipropns": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"nsuri",k:true},{a:"nsprefix"}]},
  "mmisec": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"optionname",k:true},{a:"description"}]},
  "mr": {objectname:"MR",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"mrnum",k:true},{a:"description"},{a:"siteid"}]},
  "msghubprovider": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"provider",k:true},{a:"description"}]},
  "msghubprovidertype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"providertype",k:true},{a:"description"}]},
  "msgidprefix": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"prefix",k:true},{a:"product"}]},
  "msgorglookup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"orgid",k:true},{a:"organization.description",s:false}]},
  "msgsitelookup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"siteid",k:true},{a:"site.description",s:false},{a:"orgid"}]},
  "netdevicenodename": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"nodename",k:true},{a:"siteid"}]},
  "netprinternodename": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"nodename",k:true},{a:"siteid"}]},
  "notiftemplate": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"name",k:true},{a:"description"}]},
  "numericattrlookup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"numvalue",k:true}]},
  "numericdomain": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"value",k:true},{a:"description"}]},
  "objectclass": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"classstructureid",k:true},{a:"description"}]},
  "objectname": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"objectname",k:true},{a:"description"}]},
  "odmappname": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"odmappname",k:true},{a:"description"},{a:"inputobj"}]},
  "omplist": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"displayname",s:false,k:true},{a:"productname"},{a:"version"},{a:"hostname"}]},
  "ompproduct": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"productname",k:true},{a:"manufacturer"}]},
  "ompversion": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"version",k:true}]},
  "orderunit": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"orderunit",k:true},{a:"conversion"}]},
  "org": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"orgid",k:true},{a:"description"}]},
  "originalpo": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"originalponum",k:true},{a:"description"},{a:"status"},{a:"siteid"}]},
  "oslcdomain": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"domainname",k:true},{a:"nsuri"}]},
  "oslcinteraction": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"interactionname",k:true},{a:"description"},{a:"providername"}]},
  "oslcintgroup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"intgroupname",k:true},{a:"description"}]},
  "oslcmaxmessagegroup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"msggroup",k:true}]},
  "oslcmaxmessages": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"msgid",k:true},{a:"msggroup"},{a:"msgkey"},{a:"value"}]},
  "oslcns": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"nsprefix",k:true},{a:"nsuri"}]},
  "oslcproperties": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"propertyname",k:true},{a:"title"}]},
  "oslcprops": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"oslcpropname",k:true},{a:"oslcpropns"}]},
  "oslcresourcetypes": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"resourcetype",k:true},{a:"description"},{a:"resourcetypeuri"}]},
  "oslcserviceprovider": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"serviceprovider",k:true},{a:"title"}]},
  "oslctabs": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"tabname",k:true},{a:"title"}]},
  "oslcusage": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"usage",k:true}]},
  "paraminputname": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"istemplate=1",relationship:"",columns:[{a:"inputname",k:true},{a:"inputdescription"}]},
  "parentmbos": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"objectid",k:true},{a:"tbname"}]},
  "parentproject": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"projectid",k:true},{a:"description"}]},
  "parenttask": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"taskid",k:true},{a:"description"}]},
  "patternday": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"patterndayseq",k:true},{a:"patternday"}]},
  "person": {objectname:"PERSON",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"personid",k:true},{a:"displayname"},{a:"title"},{a:"department"},{a:"location"},{a:"locationsite"},{a:"locationorg"}]},
  "persongroup": {objectname:"PERSONGROUP",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"persongroup",k:true},{a:"description"}]},
  "persongroupteam": {objectname:"PERSONGROUPTEAM",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"persongroup",k:true},{a:"respparty"}]},
  "pluscdsassetlink": {objectname:"PLUSCDSASSETLINK",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"dsplannum",k:true},{a:"description"}]},
  "plusctlassetlink": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"PLUSDSPLAN.dsplannum",k:true},{a:"PLUSDSPLAN.description"},{a:"PLUSDSPLAN.status"}]},
  "plusdsplanswo": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"dsplannum",k:true},{a:"description"},{a:"status"}]},
  "pm": {objectname:"PM",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"pmnum",k:true},{a:"description"},{a:"siteid"}]},
  "pmmeter": {objectname:"PMMETER",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"metername",k:true},{a:"meter.description"},{a:"meter.metertype"},{a:"siteid"}]},
  "pmtschedule": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"schedulenum",s:false,k:true},{a:"description",s:false},{a:"orgid",s:false}]},
  "pmworktype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"worktype",k:true},{a:"wtypedesc"},{a:"orgid"}]},
  "po": {objectname:"PO",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ponum",k:true},{a:"description"},{a:"status"},{a:"receipts"},{a:"siteid"}]},
  "poline": {objectname:"POLINE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"polinenum",k:true},{a:"description"},{a:"siteid"}]},
  "ppcraftrate": {objectname:"PPCRAFTRATE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"premiumpaycode",k:true},{a:"premiumpay.description"},{a:"orgid"}]},
  "pr": {objectname:"PR",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"prnum",k:true},{a:"description"},{a:"siteid"}]},
  "precaution": {objectname:"PRECAUTION",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"precautionid",k:true},{a:"description"},{a:"siteid"}]},
  "premiumpaycode": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"premiumpaycode",k:true},{a:"description"},{a:"orgid"}]},
  "premiumpayratetype": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"premiumpayratetype",k:true},{a:"description"},{a:"siteid"}]},
  "problemcode": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"failurecode",l:"Problem Code",k:true},{a:"failurecode.description",s:false},{a:"orgid",s:false}]},
  "procobj": {objectname:"PROCOBJ",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"process",k:true},{a:"description"}]},
  "procrule": {objectname:"PROCRULE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"procrule",k:true},{a:"description"}]},
  "prodnamesfromkeylist": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"PRODUCTNAME",k:true},{a:"PRODUCTDESC"}]},
  "propertylookuplist": {objectname:"PROPERTYLOOKUPLIST",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"lookupname",s:false,k:true}]},
  "qtemplate": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"templatename",k:true},{a:"description"}]},
  "qualifications": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"qualificationid",k:true},{a:"description"},{a:"qualcraft.craft",s:false},{a:"qualcraft.skilllevel",s:false},{a:"orgid",s:false}]},
  "quals": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"qualificationid",k:true},{a:"description"},{a:"qualtype"},{a:"orgid"}]},
  "query": {objectname:"QUERY",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"clausename",k:true},{a:"description"}]},
  "queues": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"queuename",k:true},{a:"isinbound"},{a:"issequential"},{a:"qconfactjndiname"},{a:"providerurl"},{a:"userdefined"}]},
  "rcnlinkruleattributename": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"attributename",l:"Attribute",k:true},{a:"title"}]},
  "rcntskfltval": {objectname:"RCNTSKFLTVAL",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"value",s:false,k:true}]},
  "readingtypelist": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"value",l:"Reading Type",k:true},{a:"description"}]},
  "reconcomprule": {objectname:"RECONCOMPRULE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"rulename",k:true},{a:"description"},{a:"recontype"},{a:"compset"}]},
  "reconlinkrule": {objectname:"RECONLINKRULE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"rulename",k:true},{a:"description"},{a:"recontype"},{a:"compset"}]},
  "recontaskfilter": {objectname:"RECONTASKFILTER",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"filtername",k:true},{a:"filtertype"},{a:"description"}]},
  "reffeatures": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"feature",k:true},{a:"label"},{a:"feature.description"},{a:"feature.featuretype"}]},
  "relatedwo": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"wonum",k:true},{a:"description"},{a:"woclass"},{a:"status"},{a:"siteid"}]},
  "relation": {objectname:"RELATION",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"relationnum",k:true},{a:"description"},{a:"type"}]},
  "relationship": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"NAME",k:true},{a:"WHERECLAUSE"}]},
  "relattribute": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"dataattribute",k:true},{a:"datatable"},{a:"relationship"}]},
  "repfacility": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"location",l:"Repair Facility",k:true},{a:"description"},{a:"type"},{a:"siteid"}]},
  "replyinterfaces": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ifacename",k:true},{a:"description"}]},
  "report": {objectname:"REPORT",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"description"},{a:"reportname",k:true},{a:"reporttoapp.description",l:"Application"}]},
  "reportdesign": {objectname:"REPORTDESIGN",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"reportname",k:true},{a:"description"}]},
  "reportlookuplist": {objectname:"REPORTLOOKUPLIST",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"lookupname",s:false,k:true}]},
  "reportobject": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"intobjectname",k:true},{a:"description",s:false}]},
  "reslevel": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"resourcelevelsnum",k:true},{a:"description"},{a:"source"},{a:"type"}]},
  "role": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"maxrole",k:true},{a:"description"}]},
  "rotatingasset": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"assetnum",k:true},{a:"description"},{a:"location"},{a:"conditioncode"},{a:"siteid"}]},
  "routes": {objectname:"ROUTES",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"route",k:true},{a:"description"},{a:"siteid"}]},
  "rptrotasset": {selectmode:"single",inputmode:"readonly",orderby:"assetnum",whereclause:" itemnum in (select itemnum from item where rotating=1) ",relationship:"",columns:[{a:"assetnum",k:true},{a:"description"},{a:"location"},{a:"itemnum"},{a:"conditioncode"},{a:"siteid"}]},
  "rsconfig": {objectname:"RSCONFIG",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"querycolumn",k:true},{a:"description",s:false}]},
  "safetyplan": {objectname:"SAFETYPLAN",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"safetyplanid",k:true},{a:"description"},{a:"siteid"}]},
  "schargecode": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"schargecode",k:true},{a:"description"},{a:"prorateservdflt"},{a:"gldebitacct"},{a:"tax1code"}]},
  "scriptengines": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"engineshortname",s:false,k:true},{a:"enginename",s:false},{a:"engineversion",s:false}]},
  "sctemplate": {objectname:"SCTEMPLATE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"sctemplateid",k:true},{a:"description"}]},
  "searchbar": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[]},
  "searchbarpopup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[]},
  "searchmultihelp": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[]},
  "searchselectallhelp": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[]},
  "sectemplate": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"template",k:true},{a:"description"}]},
  "selectcontrols": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"control_name",k:true},{a:"control_desc"}]},
  "selectsitefororg": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"siteid",k:true},{a:"description"}]},
  "sendingmode": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"handlername",k:true},{a:"handlerclassname"},{a:"userdefined"}]},
  "service": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"commodity",l:"Service",k:true},{a:"description"},{a:"parent",l:"Service Group"}]},
  "serviceaddress": {objectname:"SERVICEADDRESS",selectmode:"single",inputmode:"readonly",orderby:"objectname",whereclause:"",relationship:"",columns:[{a:"addresscode",k:true}]},
  "serviceaddress2": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"addresscode",k:true}]},
  "serviceaddresstable": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"addresscode",k:true},{a:"description"},{a:"streetaddress"},{a:"city"},{a:"stateprovince"},{a:"postalcode"}]},
  "servicegroup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"commodity",l:"Service Group",k:true},{a:"description"}]},
  "serviceitem": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"itemnum",k:true},{a:"description"},{a:"itemsetid"}]},
  "serviceobject": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"serviceobject",k:true},{a:"description"}]},
  "sets": {objectname:"SETS",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"setid",k:true},{a:"settype"},{a:"description"}]},
  "sfwitems": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"itemnum",k:true},{a:"description"},{a:"commoditygroup"},{a:"commodity"}]},
  "sfwlicense": {objectname:"SFWLICENSE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"sfwlicenseid",l:"License",k:true},{a:"description"},{a:"vendor"},{a:"licensetype"},{a:"associated"}]},
  "shifitmodify": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"SHIFT",columns:[{a:"shiftnum",k:true},{a:"description"},{a:"orgid"}]},
  "shift": {objectname:"SHIFT",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"shiftnum",k:true},{a:"description"},{a:"orgid"}]},
  "shiftpatternday": {objectname:"SHIFTPATTERNDAY",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"shiftnum",k:true},{a:"patterndayseq"},{a:"orgid"}]},
  "shipto": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"shiptoaddresscode",k:true},{a:"siteid"}]},
  "sigoption": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"OPTIONNAME",k:true},{a:"DESCRIPTION"}]},
  "simplecondition": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"conditionnum",k:true},{a:"expression"},{a:"classname"}]},
  "site": {objectname:"SITE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"siteid",k:true},{a:"description"},{a:"orgid"}]},
  "siteorgsets": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"attributename",k:true}]},
  "sitewithstatus": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"siteid",k:true},{a:"description"},{a:"orgid"},{a:"active"}]},
  "skdname": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:" name in (select name from skdproject where usewith='RLASSIGN' and (createby=:USER or (ispublic=1))) ",relationship:"",columns:[{a:"name",k:true},{a:"description"}]},
  "skdprojectshifts": {objectname:"SKDPROJECTSHIFTS",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"SKDPROJECTSHIFTS",columns:[{a:"shiftnum",k:true},{a:"skdprojectshiftsinfo.description"},{a:"orgid"}]},
  "skilllevel": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"skilllevel",k:true},{a:"description"},{a:"orgid"}]},
  "slas": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"slanum",k:true},{a:"description"},{a:"objectname"},{a:"slatype"},{a:"commoditygroup"},{a:"commodity"},{a:"vendor"},{a:"status"},{a:"siteid"}]},
  "solution": {objectname:"SOLUTION",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"solution",k:true},{a:"description"},{a:"CLASSSTRUCTURE.HIERARCHYPATH",s:false},{a:"orgid",s:false}]},
  "sourceinterfaces": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ifacename",k:true}]},
  "spellingform": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[]},
  "startcenter": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"startcenterid",k:true},{a:"description"}]},
  "storelocsite": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"location",l:"Storeroom",k:true},{a:"type"},{a:"description"},{a:"siteid",l:"Storeroom Site"}]},
  "storelocsiteorg": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"location",l:"Storeroom",k:true},{a:"type"},{a:"description"},{a:"siteid",l:"Storeroom Site"},{a:"orgid",l:"Storeroom Organization"}]},
  "storeroom": {objectname:"FAVITEM",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"location",l:"Storeroom"},{a:"type"},{a:"description",k:true},{a:"siteid",l:"Storeroom Site"}]},
  "structparenttable": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"tbname",k:true}]},
  "structrelation": {objectname:"STRUCTRELATION",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"relation",k:true},{a:"relsqlwhere"}]},
  "structtable": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"tbname",k:true}]},
  "structures": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"structurename",k:true},{a:"description"}]},
  "system": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"systemid",k:true},{a:"description"},{a:"network"}]},
  "table": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"autokeyname",k:true},{a:"seed"},{a:"prefix"}]},
  "tables": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"tbname",k:true}]},
  "tagout": {objectname:"TAGOUT",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"tagoutid",k:true},{a:"description"},{a:"siteid"}]},
  "taxcode": {objectname:"TAX",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"taxcode",k:true},{a:"taxrate"},{a:"effective"},{a:"orgid"}]},
  "tenantcode": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"tenantcode",k:true},{a:"description"},{a:"status"}]},
  "term": {objectname:"TERM",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"termid",k:true},{a:"description"},{a:"type"},{a:"orgid"}]},
  "ticket": {objectname:"TICKET",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ticketid",k:true},{a:"description"},{a:"class"},{a:"orgid"}]},
  "ticketwo": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"ticketid",k:true},{a:"description"},{a:"class"},{a:"wonum"},{a:"description"},{a:"woclass"},{a:"istask"},{a:"siteid"}]},
  "tkasset": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"assetnum",k:true},{a:"tkassetdescription"},{a:"location"},{a:"siteid"}]},
  "tklocation": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"location",k:true},{a:"tkassetdescription"},{a:"assetnum"},{a:"siteid"}]},
  "tktemplate": {objectname:"TKTEMPLATE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"templateid",k:true},{a:"description"}]},
  "tloamsoftware": {objectname:"TLOAMSOFTWARE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"swname",k:true},{a:"version",w:80},{a:"release",w:80},{a:"platformbase"},{a:"tloamdpammanuvariant.manufacturername",l:"Manufacturer",w:120}]},
  "toinvloc": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"location",k:true},{a:"locations.description",s:false},{a:"siteid",s:false}]},
  "tolerance": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"grpname",k:true}]},
  "tool": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"toolnum",k:true},{a:"description"}]},
  "toolasset": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"assetnum",k:true},{a:"description"},{a:"itemnum"},{a:"itemtype"},{a:"location"},{a:"siteid"}]},
  "toolitem": {objectname:"TOOLITEM",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:" itemnum in( select distinct itemnum from toolinv)",relationship:"",columns:[{a:"itemnum",k:true},{a:"description"},{a:"commoditygroup"},{a:"commodity"}]},
  "toolorg": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"orgid",k:true},{a:"description"}]},
  "tools": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"itemnum",k:true},{a:"description"}]},
  "usergroups": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"grpname",k:true}]},
  "uwostatus": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"value",k:true},{a:"description"}]},
  "valueid": {objectname:"MAXDOMVALCOND",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"valueid",k:true},{a:"value"},{a:"description"}]},
  "valuelist": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"value",k:true},{a:"description"}]},
  "valuelist2": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"value",k:true},{a:"description"}]},
  "valuelistmax": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"maxvalue",k:true},{a:"description"}]},
  "valuelistwithvalueonly": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"value",k:true}]},
  "vendors": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"company",k:true},{a:"name"},{a:"type"},{a:"orgid"}]},
  "wctemplate": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"template",k:true},{a:"description"}]},
  "weatherorg": {objectname:"WEATHERORG",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"org",k:true},{a:"description"}]},
  "weatherproduct": {objectname:"WEATHERPRODUCT",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"productcode",k:true},{a:"description"}]},
  "wfprocess": {objectname:"WFPROCESS",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"processname",k:true},{a:"description"}]},
  "wmlaboronly": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"laborcode",k:true},{a:"labor.person.displayname",s:false},{a:"orgid",s:false}]},
  "wo_selectsa": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"addresscode",k:true},{a:"description"},{a:"streetaddress"},{a:"city"},{a:"stateprovince"},{a:"postalcode"}]},
  "workcenter": {objectname:"USERTOURTRACKER",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"relworkcenter",k:true},{a:"maxapps.description"}]},
  "workgroup": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"wogroup",k:true},{a:"description"},{a:"taskid"},{a:"reportdate"},{a:"siteid"}]},
  "workorder": {objectname:"WORKORDER",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"wonum",k:true},{a:"description"},{a:"reportdate"},{a:"siteid"}]},
  "workorderistask": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"wonum",k:true},{a:"description"},{a:"woclass"},{a:"location"},{a:"assetnum"},{a:"status"},{a:"istask"}]},
  "worktype": {objectname:"WORKTYPE",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"worktype",l:"Type",k:true},{a:"wtypedesc"},{a:"orgid"}]},
  "workview": {objectname:"WORKVIEW",selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"recordkey",k:true},{a:"class"},{a:"description"},{a:"siteid"}]},
  "workzone": {objectname:"WORKZONE",selectmode:"single",inputmode:"readonly",orderby:"workzone asc",whereclause:"",relationship:"",columns:[{a:"workzone",k:true},{a:"description",s:false},{a:"type"},{a:"orgid"}]},
  "wptask": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"taskid",s:false,k:true},{a:"description",s:false},{a:"siteid",s:false}]},
  "wptaskassign": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"WORKORDER",columns:[{a:"taskid",s:false,k:true},{a:"description",s:false},{a:"siteid",s:false}]},
  "wsioattribute": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"attributename",s:false,k:true},{a:"hierarchypath",l:"Element Location",s:false},{a:"wsioobjname",l:"Object Name",s:false}]},
  "wsmethod": {selectmode:"single",inputmode:"readonly",orderby:"",whereclause:"",relationship:"",columns:[{a:"servicename",k:true},{a:"methodname"}]}
}
// lookup 名称小写下标(LOOKUPS.xml 中存在大小写混合的 id, 如 ibm_ProjectPack)
var lowerIndex = null

function ensureLowerIndex() {
  if (lowerIndex) {
    return lowerIndex
  }
  lowerIndex = {}
  for (var key in LOOKUP_MAP) {
    var lowerKey = key.toLowerCase()
    if (!lowerIndex[lowerKey]) {
      lowerIndex[lowerKey] = LOOKUP_MAP[key]
    }
  }
  return lowerIndex
}

/**
 * 查找 lookup 配置(大小写不敏感)
 * @param {string} lookup lookup 名称, 如 asset / item / workorder
 * @returns {Object|null} 配置对象
 */
export function getLookupConfig(lookup) {
  if (!lookup) {
    return null
  }
  var key = String(lookup)
  if (LOOKUP_MAP[key]) {
    return LOOKUP_MAP[key]
  }
  var index = ensureLowerIndex()
  return index[key.toLowerCase()] || null
}

/**
 * 把 _lookup.js 的简写列定义(a/l/w/k/s/f/h)转换成表格列格式
 * @param {Array} columns 列定义数组, 元素可为字符串或对象
 * @returns {Array} [{dataattribute,label,width,sortable,filterable,showFilterField,key}]
 */
export function normalizeLookupColumns(columns) {
  var result = []
  var list = []
  if (Array.isArray(columns)) {
    list = columns
  } else if (typeof columns === 'string' && columns) {
    list = columns.split(',')
  }
  for (var i = 0; i < list.length; i++) {
    var col = list[i]
    if (!col) {
      continue
    }
    if (typeof col === 'string') {
      var name = col.trim()
      if (!name) {
        continue
      }
      result.push({ dataattribute: name, label: name, width: '', sortable: true, filterable: true, showFilterField: false, key: false })
      continue
    }
    var dataattribute = col.dataattribute || col.a || col.attr || col.attribute
    if (!dataattribute) {
      continue
    }
    result.push({
      dataattribute: dataattribute,
      label: col.label || col.l || dataattribute,
      width: col.width || col.w || '',
      sortable: col.sortable !== undefined ? col.sortable !== false : col.s !== false,
      filterable: col.filterable !== undefined ? col.filterable !== false : col.f !== false,
      showFilterField: col.showFilterField === true || col.h === true,
      key: col.key === true || col.k === true
    })
  }
  return result
}

/**
 * 取 lookup 的列定义(表格列格式)
 * @param {string} lookup lookup 名称
 * @returns {Array} 表格列数组
 */
export function getLookupColumns(lookup) {
  var config = getLookupConfig(lookup)
  return config ? normalizeLookupColumns(config.columns) : []
}

/**
 * 取 lookup 的关键列(默认 srcKeys): 关键列 k=true, 没有关键列时取第一列
 * @param {string} lookup lookup 名称
 * @returns {Array} 字段名数组
 */
export function getLookupKeyColumns(lookup) {
  var columns = getLookupColumns(lookup)
  var keys = []
  for (var i = 0; i < columns.length; i++) {
    if (columns[i].key) {
      keys.push(columns[i].dataattribute)
    }
  }
  if (keys.length) {
    return keys
  }
  return columns.length ? [columns[0].dataattribute] : []
}

/**
 * 取 lookup 查询用的对象名: relationObject 优先, 其次配置中的 objectname, 都没有时返回空串(由服务端解析)
 * @param {string} lookup lookup 名称
 * @param {string} relationObject 关系对象名(调用方指定)
 * @returns {string} 对象名
 */
export function getLookupObjectName(lookup, relationObject) {
  if (relationObject) {
    return relationObject
  }
  var config = getLookupConfig(lookup)
  return config && config.objectname ? config.objectname : ''
}

/**
 * 注册/覆盖 lookup 定义(业务上非标准 lookup 可在页面中自行注册)
 * @param {string} lookup lookup 名称
 * @param {Object} config 配置(会与已有配置合并)
 */
export function registerLookup(lookup, config) {
  if (!lookup) {
    return
  }
  LOOKUP_MAP[lookup] = Object.assign({}, LOOKUP_MAP[lookup], config || {})
  lowerIndex = null
}

/**
 * 列出所有 lookup 名称
 * @returns {Array} lookup 名称数组
 */
export function listLookupNames() {
  return Object.keys(LOOKUP_MAP)
}

