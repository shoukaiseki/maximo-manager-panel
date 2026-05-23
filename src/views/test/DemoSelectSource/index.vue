<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="源名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入源名称"
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
          v-hasPermi="['test:DemoSelectSource:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['test:DemoSelectSource:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['test:DemoSelectSource:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['test:DemoSelectSource:export']"
        >导出</el-button>
      </el-col>
	  <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="DemoSelectSourceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="源主键" align="center" prop="demoSelectSourceId" />
      <el-table-column label="源名称" align="center" prop="name" />
      <el-table-column label="备用字符串01" align="center" prop="var01" />
      <el-table-column label="备用字符串01" align="center" prop="var02" />
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
            v-hasPermi="['test:DemoSelectSource:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['test:DemoSelectSource:remove']"
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

    <!-- 添加或修改实例选择源对话框 -->
    <el-dialog :close-on-click-modal="false" :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="源名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入源名称" />
        </el-form-item>
        <el-form-item label="备用字符串01" prop="var01">
          <el-input v-model="form.var01" placeholder="请输入备用字符串01" />
        </el-form-item>
        <el-form-item label="备用字符串01" prop="var02">
          <el-input v-model="form.var02" placeholder="请输入备用字符串01" />
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
import { lookupTreeDemoSelectSource,  lookupDemoSelectSource,  listDemoSelectSource, getDemoSelectSource, delDemoSelectSource, addDemoSelectSource, updateDemoSelectSource, exportDemoSelectSource } from "@/api/test/DemoSelectSource";
import { generateUUID } from '@/utils/wangbao';

export default {
  name: "DemoSelectSource",
  components: {
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
      // 实例选择源表格数据
      DemoSelectSourceList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: null,
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
    /** 查询实例选择源列表 */
    getList() {
      this.loading = true;
      listDemoSelectSource(this.queryParams).then(response => {
        this.DemoSelectSourceList = response.rows;
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
        demoSelectSourceId: null,
        name: null,
        var01: null,
        var02: null,
        status: "0",
        delFlag: null
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
      this.ids = selection.map(item => item.demoSelectSourceId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加实例选择源";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const demoSelectSourceId = row.demoSelectSourceId || this.ids
      getDemoSelectSource(demoSelectSourceId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改实例选择源";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.demoSelectSourceId != null) {
            updateDemoSelectSource(this.form).then(response => {
              this.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addDemoSelectSource(this.form).then(response => {
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
      const demoSelectSourceIds = row.demoSelectSourceId || this.ids;
      this.$confirm('是否确认删除实例选择源编号为"' + demoSelectSourceIds + '"的数据项?', "警告", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }).then(function() {
          return delDemoSelectSource(demoSelectSourceIds);
        }).then(() => {
          this.getList();
          this.msgSuccess("删除成功");
        })
    },
    /** 导出按钮操作 */
    handleExport() {
      const queryParams = this.queryParams;
      this.$confirm('是否确认导出所有实例选择源数据项?', "警告", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }).then(function() {
          return exportDemoSelectSource(queryParams);
        }).then(response => {
          this.download(response.msg);
        })
    },
    // // 状态修改
    // handleStatusChange(row) {
    //     row.status = row.status === "0" ? "1" : "0";
    // },

  }
};
</script>
