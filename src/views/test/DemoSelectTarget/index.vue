<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="源主键1" prop="demoSelectSourceId">
        <el-input
          v-model="queryParams.demoSelectSourceId"
          placeholder="请输入源主键1"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="源名称1" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入源名称1"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="源主键2" prop="demoSelectSourceId2">
        <el-input
          v-model="queryParams.demoSelectSourceId2"
          placeholder="请输入源主键2"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="源名称2" prop="name2">
        <el-input
          v-model="queryParams.name2"
          placeholder="请输入源名称2"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="源主键3" prop="demoSelectSourceId3">
        <el-input
          v-model="queryParams.demoSelectSourceId3"
          placeholder="请输入源主键3"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="源名称3" prop="name3">
        <el-input
          v-model="queryParams.name3"
          placeholder="请输入源名称3"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="源主键4" prop="demoSelectSourceId4">
        <el-input
          v-model="queryParams.demoSelectSourceId4"
          placeholder="请输入源主键4"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="源名称4" prop="name4">
        <el-input
          v-model="queryParams.name4"
          placeholder="请输入源名称4"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
        <el-form-item v-if="isSystem" label="状态" prop="status">
            <el-select
                    v-model="queryParams.status"
                    placeholder="状态"
                    clearable
                    size="small"
                    style="width: 240px"
            >
                <el-option
                        key=""
                        label="全部"
                        value=""
                />
                <el-option
                        key="0"
                        label="启用"
                        value="0"
                />
                <el-option
                        key="1"
                        label="禁用"
                        value="1"
                />
            </el-select>
        </el-form-item>
        <el-form-item>
        <el-button type="cyan" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['test:DemoSelectTarget:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['test:DemoSelectTarget:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['test:DemoSelectTarget:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['test:DemoSelectTarget:export']"
        >导出</el-button>
      </el-col>
	  <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="DemoSelectTargetList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="demoSelectTargetId" />
      <el-table-column label="源主键1" align="center" prop="demoSelectSourceId" />
      <el-table-column label="源名称1" align="center" prop="name" />
      <el-table-column label="备用字符串01" align="center" prop="var01" />
      <el-table-column label="备用字符串01" align="center" prop="var02" />
      <el-table-column label="备用字符串01" align="center" prop="var03" />
      <el-table-column label="源主键2" align="center" prop="demoSelectSourceId2" />
      <el-table-column label="源名称2" align="center" prop="name2" />
      <el-table-column label="源主键3" align="center" prop="demoSelectSourceId3" />
      <el-table-column label="源名称3" align="center" prop="name3" />
      <el-table-column label="源主键4" align="center" prop="demoSelectSourceId4" />
      <el-table-column label="源名称4" align="center" prop="name4" />
        <el-table-column
                v-if="isSystem"
                label="状态" align="center">
            <template slot-scope="scope">
                <el-switch
                        :disabled="true"
                        v-model="scope.row.status"
                        active-value="0"
                        inactive-value="1"
                ></el-switch>
            </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['test:DemoSelectTarget:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['test:DemoSelectTarget:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改实例选择对话框 -->
    <el-dialog :close-on-click-modal="false" :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="源主键1" prop="demoSelectSourceId">
          <el-input v-model="form.demoSelectSourceId" placeholder="请输入源主键1" />
          <SelectDemoSelectSourceSetName
            @change="handleChangeDemoSelectSourceId"
            v-model="form.demoSelectSourceId" placeholder="请输入名称" ></SelectDemoSelectSourceSetName>

        </el-form-item>
        <el-form-item label="源名称1" prop="name">
          <el-input v-model="form.name" placeholder="请输入源名称1" />
        </el-form-item>

        <el-form-item label="源主键2" prop="demoSelectSourceId2">
          <el-input v-model="form.demoSelectSourceId2" placeholder="请输入源主键2" />
          <SelectDemoSelectSourceSetName
            :rowData="form"
            labelName="name2"
            v-model="form.demoSelectSourceId2"
            placeholder="请输入名称" ></SelectDemoSelectSourceSetName>
        </el-form-item>
        <el-form-item label="源名称2" prop="name2">
          <el-input v-model="form.name2" placeholder="请输入源名称2" />
        </el-form-item>

        <el-form-item label="源主键3" prop="demoSelectSourceId3">
          <el-input v-model="form.demoSelectSourceId3" placeholder="请输入源主键3" />
        </el-form-item>
        <el-form-item label="源名称3" prop="name3">
          <el-input v-model="form.name3" placeholder="请输入源名称3" />
        </el-form-item>
        <el-form-item label="源主键4" prop="demoSelectSourceId4">
          <el-input v-model="form.demoSelectSourceId4" placeholder="请输入源主键4" />
        </el-form-item>
        <el-form-item label="源名称4" prop="name4">
          <el-input v-model="form.name4" placeholder="请输入源名称4" />
        </el-form-item>
        <el-form-item label="备用字符串01" prop="var01">
          <el-input v-model="form.var01" placeholder="请输入备用字符串01" />
        </el-form-item>
        <el-form-item label="备用字符串01" prop="var02">
          <el-input v-model="form.var02" placeholder="请输入备用字符串01" />
        </el-form-item>
        <el-form-item label="备用字符串01" prop="var03">
          <el-input v-model="form.var03" placeholder="请输入备用字符串01" />
        </el-form-item>
      <el-form-item
              v-if="isSystem"
              label="状态" prop="status">
          <el-switch
                  v-model="form.status"
                  active-value="0"
                  inactive-value="1"
          ></el-switch>
      </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { lookupTreeDemoSelectTarget,  lookupDemoSelectTarget,  listDemoSelectTarget, getDemoSelectTarget, delDemoSelectTarget, addDemoSelectTarget, updateDemoSelectTarget, exportDemoSelectTarget } from "@/api/test/DemoSelectTarget";
import { generateUUID } from '@/utils/sks';
import SelectDemoSelectSourceSetName from '@/views/test/DemoSelectSource/SelectDemoSelectSourceSetName'

export default {
  name: "DemoSelectTarget",
  components: {
    SelectDemoSelectSourceSetName
  },
  data() {
    return {
      //是否为超管
      isAdmin: false,
      //是否为管理员
      isSystem: false,
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 实例选择表格数据
      DemoSelectTargetList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        demoSelectSourceId: null,
        name: null,
        demoSelectSourceId2: null,
        name2: null,
        demoSelectSourceId3: null,
        name3: null,
        demoSelectSourceId4: null,
        name4: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
      }
    };
  },
  created() {
    this.initPage();
  },
  activated() {
    this.initPage();
  },
  methods: {
    initPage(){
        //main.js 需要增加原型方法引入
        //import {checkRole,checkPermi} from "@/utils/permission";
        // Vue.prototype.checkRole = checkRole
       this.isAdmin=this.checkRole(['admin']);
       this.isSystem=this.checkRole(['system'])||this.isAdmin;
       this.getList();
    },
    /** 查询实例选择列表 */
    getList() {
      this.loading = true;
      listDemoSelectTarget(this.queryParams).then(response => {
        this.DemoSelectTargetList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        demoSelectTargetId: null,
        demoSelectSourceId: null,
        name: null,
        var01: null,
        var02: null,
        var03: null,
        status: "0",
        delFlag: null,
        rowstamp: null,
        demoSelectSourceId2: null,
        name2: null,
        demoSelectSourceId3: null,
        name3: null,
        demoSelectSourceId4: null,
        name4: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.demoSelectTargetId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加实例选择";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const demoSelectTargetId = row.demoSelectTargetId || this.ids
      getDemoSelectTarget(demoSelectTargetId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改实例选择";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.demoSelectTargetId != null) {
            updateDemoSelectTarget(this.form).then(response => {
              this.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addDemoSelectTarget(this.form).then(response => {
              this.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const demoSelectTargetIds = row.demoSelectTargetId || this.ids;
      this.$confirm('是否确认删除实例选择编号为"' + demoSelectTargetIds + '"的数据项?', "警告", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }).then(function() {
          return delDemoSelectTarget(demoSelectTargetIds);
        }).then(() => {
          this.getList();
          this.msgSuccess("删除成功");
        })
    },
    /** 导出按钮操作 */
    handleExport() {
      const queryParams = this.queryParams;
      this.$confirm('是否确认导出所有实例选择数据项?', "警告", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }).then(function() {
          return exportDemoSelectTarget(queryParams);
        }).then(response => {
          this.download(response.msg);
        })
    },
    // // 状态修改
    // handleStatusChange(row) {
    //     row.status = row.status === "0" ? "1" : "0";
    // },
    handleChangeDemoSelectSourceId(row){
      this.form.name2=row.name
    }

  }
};
</script>
