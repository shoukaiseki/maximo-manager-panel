//暴露mixins对象

import { resetForm } from '../../utils/ruoyi'

export const autumnFormMixin = {
    data(){
        return{
            allLoading: false,

            form:{},
            defaultForm:{},

            title: '',
            //未使用
            showDialog: false,

            //字典数据都放这里
            dictData:{

            },

            rules:{
            },
        }
    },
    beforeCreate(){
        console.log("混入的beforeCreated");

    },
    created() {
        console.log("autumn-form.created")
        this.autumnFormInit();
    },
    mounted() {
        console.log("autumn-form.mounted")
        this.autumnFormInit();
    },
    methods: {
        show({form={},title='新建',virtualChangeBitFlag=this.CHANGE_BIT_FLAG.none}={}){
            form.virtualChangeBitFlag=virtualChangeBitFlag
            this.title=title
            this.showDialog=true
            console.log("autumn.show")
            this.resetMainForm(form)
        },
        autumnFormInit({form}={}){
            this.resetMainForm(form)
        },
        resetMainForm(data){
            let form = this.mergeFromObject({},this.defaultForm,data)
            form=this.resetMainFormBefore(form)
            this.form= form
            this.resetForm('form')
            console.log("autumn-form.this.form=",this.form)
        },
        //form 重新赋值前处理
        resetMainFormBefore(form){
           return form;
        },
    },
}
