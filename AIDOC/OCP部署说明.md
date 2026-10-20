# Maximo Management Panel — OCP 部署说明

## 1. 概述

本文档说明如何在 Red Hat OpenShift Container Platform（OCP）上部署 **Maximo Management Panel**（Vue 2 + Element UI 前端 + Solon / Java 17 后端的 Maximo 运维面板，用于对象结构浏览、菜单管理、自动化脚本诊断、实时日志监控等场景）。

容器镜像内置以下组件，由 supervisord 统一托管：

| 组件 | 说明 |
|------|------|
| nginx | 前端静态资源与 `/solonapi` 反向代理，监听 80 |
| Solon 后端 | Java 17 + Solon，监听 8081 |
| MySQL | 面板本地数据存储 |
| 运维工具 | vim、bash-completion、curl 等，便于容器内排障 |

部署使用的容器镜像已就绪，直接引用 3.1 的镜像地址即可；命名空间名称、集群域名、节点 IP、PVC 名称等仍请按实际环境替换。

## 2. 前置条件

- OCP 集群（示例基于 OCP 4.x，已配置默认 IngressController / Router）
- `oc` CLI 已登录目标集群
- 可用的容器镜像：阿里云 ACR 公开镜像（地址见 3.1），集群节点可访问公网即可直接拉取
- 若集群无法访问公网，需先将镜像搬运至企业内部私有仓库（见 3.2）

## 3. 镜像准备

### 3.1 已就绪的镜像地址

部署直接引用以下镜像地址（已推送至阿里云 ACR，公网可拉取即用）：

```
crpi-k3zustt1jsdfcw7c.cn-hangzhou.personal.cr.aliyuncs.com/shoukaiseki/jdk17_nginx_mysql:latest
```

集群节点能访问公网即可直接拉取，无需额外配置 Secret；4.4 的 Deployment YAML 中 `image` 已直接使用该地址。

### 3.2 私有镜像仓库场景（可选）

若集群无法访问公网、或镜像需经企业内部私有库拉取，先在本地从阿里云拉取镜像，再推送至私有库：

```bash
# 1) 从阿里云拉取公开镜像
docker pull crpi-k3zustt1jsdfcw7c.cn-hangzhou.personal.cr.aliyuncs.com/shoukaiseki/jdk17_nginx_mysql:latest

# 2) 打上私有库标签（<私有库地址> 按实际替换，例如 registry.example.com:5000/mas/jdk17_nginx_mysql:latest）
docker tag crpi-k3zustt1jsdfcw7c.cn-hangzhou.personal.cr.aliyuncs.com/shoukaiseki/jdk17_nginx_mysql:latest <私有库地址>/jdk17_nginx_mysql:latest

# 3) 推送至私有库
docker push <私有库地址>/jdk17_nginx_mysql:latest
```

> 私有库场景下：4.4 部署 YAML 的 `image` 改为私有库地址，并按 4.3 创建 ImagePullSecret。

## 4. OCP 部署

### 4.1 创建项目

```bash
oc new-project maximo-manager-panel
```

### 4.2 创建 ServiceAccount 并绑定 anyuid SCC

面板容器以 root 启动 supervisord，需要授予 `anyuid` SCC：

```bash
oc create sa maximo-manager-panel-sa -n maximo-manager-panel
oc adm policy add-scc-to-user anyuid -z maximo-manager-panel-sa -n maximo-manager-panel
```

### 4.3 创建 ImagePullSecret（仅私有仓库需要）

> 使用 3.1 的公开镜像时**无需**创建 Secret；仅当镜像存放在企业私有库时才需要，将 `--docker-server` 指向私有库地址：

```bash
oc create secret docker-registry acr-secret \
  --docker-server=<私有库地址> \
  --docker-username=<私有库账号> \
  --docker-password=<私有库访问凭证> \
  -n maximo-manager-panel

oc patch serviceaccount maximo-manager-panel-sa -n maximo-manager-panel \
  -p '{"imagePullSecrets": [{"name": "acr-secret"}]}'
```

### 4.4 部署 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: maximo-manager-panel
  namespace: maximo-manager-panel
  labels:
    app: maximo-manager-panel
spec:
  replicas: 1
  selector:
    matchLabels:
      app: maximo-manager-panel
  template:
    metadata:
      labels:
        app: maximo-manager-panel
    spec:
      serviceAccountName: maximo-manager-panel-sa
      initContainers:
        # 如需在启动前初始化共享目录，可在此声明 init 容器
        - name: init-dirs
          image: crpi-k3zustt1jsdfcw7c.cn-hangzhou.personal.cr.aliyuncs.com/shoukaiseki/jdk17_nginx_mysql:latest
          command: ["sh", "-c", "mkdir -p /shared && chmod -R 777 /shared"]
          volumeMounts:
            - name: shared
              mountPath: /shared
      containers:
        - name: panel
          image: crpi-k3zustt1jsdfcw7c.cn-hangzhou.personal.cr.aliyuncs.com/shoukaiseki/jdk17_nginx_mysql:latest
          ports:
            - name: http
              containerPort: 80
          volumeMounts:
            - name: shared
              mountPath: /shared
          # 按需开启存活/就绪探针
          # readinessProbe:
          #   httpGet:
          #     path: /
          #     port: 80
          #   initialDelaySeconds: 10
          #   periodSeconds: 10
      volumes:
        - name: shared
          # 若使用 ReadWriteOnce(RWO) 的共享 PVC，请配合 podAffinity 固定到 PVC 所在节点，
          # 避免跨节点挂载产生 Multi-Attach 错误，详见 6.2
          persistentVolumeClaim:
            claimName: <共享PVC名称>
```

> 若不需要共享存储，直接删除 `initContainers`、`volumeMounts`、`volumes` 部分即可。

### 4.5 创建 Service

```bash
oc create service clusterip maximo-manager-panel \
  --tcp=80:80 \
  -n maximo-manager-panel
```

或使用 YAML：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: maximo-manager-panel
  namespace: maximo-manager-panel
spec:
  selector:
    app: maximo-manager-panel
  ports:
    - name: http
      port: 80
      targetPort: 80
  type: ClusterIP
```

### 4.6 创建 Route（edge 终止）

```bash
oc create route edge maximo-manager-panel \
  --service=maximo-manager-panel \
  --port=http \
  -n maximo-manager-panel
```

创建后查看访问地址：

```bash
oc get route maximo-manager-panel -n maximo-manager-panel
# NAME                   HOST/PORT                                            ...
# maximo-manager-panel   maximo-manager-panel-maximo-manager-panel.apps.<集群域名>
```

### 4.7 NetworkPolicy 放行入口流量（关键）

如果项目内存在 **默认拒绝全部流量** 的 NetworkPolicy（例如项目其他组件带有的 deny-all 策略），Router 转发到 Pod 的流量会被拦截，访问会出现 `Application is not available`（503）。

> 注意：OCP Router 为 **hostNetwork** 部署，源 IP 是节点 IP 而非 Pod IP。仅用 `kubernetes.io/metadata.name: openshift-ingress` 的 namespaceSelector，在 Router 与 Pod 跨节点场景下可能匹配不到流量，需使用 OpenShift 官方 `network.openshift.io/policy-group: ingress` 标签，或使用 Router 节点 IP 的 ipBlock 兜底。

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-from-openshift-ingress-to-panel
  namespace: maximo-manager-panel
spec:
  podSelector:
    matchLabels:
      app: maximo-manager-panel
  policyTypes:
    - Ingress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              network.openshift.io/policy-group: ingress
      ports:
        - port: 80
          protocol: TCP
    # 兜底：放行 Router 所在节点 IP（按实际节点 IP 替换，多副本时需列出所有节点）
    - from:
        - ipBlock:
            cidr: <Router节点IP>/32
      ports:
        - port: 80
          protocol: TCP
```

应用：

```bash
oc apply -f allow-ingress.yaml
```

## 5. 访问验证

```bash
# 1) 检查 Pod 就绪
oc get pods -n maximo-manager-panel -o wide

# 2) 集群内通过 Service 访问（应返回 200）
oc exec -n maximo-manager-panel deploy/maximo-manager-panel -- \
  curl -s -o /dev/null -w '%{http_code}\n' \
  http://maximo-manager-panel.maximo-manager-panel.svc:80/

# 3) 通过 Router 侧验证（模拟外部访问链路，应返回 200）
oc exec -n openshift-ingress deploy/router-default -- \
  curl -s -o /dev/null -w '%{http_code}\n' \
  -H 'Host: maximo-manager-panel-maximo-manager-panel.apps.<集群域名>' \
  http://<Pod IP>:80/

# 4) 浏览器访问 Route 地址
open https://maximo-manager-panel-maximo-manager-panel.apps.<集群域名>
```

## 6. 常见问题排查

### 6.1 访问出现 "Application is not available"

页面来自 Router 的 503 默认页，说明 Route/Service/Pod 通常正常，但 **Router 到后端 Pod 的流量被拦截**。

排查顺序：

```bash
# 看 Pod/Service/Endpoints 是否都有
oc get pods,svc,endpoints -n maximo-manager-panel

# 看项目内是否默认拒绝全部流量的 NetworkPolicy
oc get networkpolicy -n maximo-manager-panel

# 从 Router 侧测试到 Pod 的连通性（对比同/跨节点）
oc exec -n openshift-ingress deploy/router-default -- \
  curl -s -o /dev/null -w '%{http_code}\n' --max-time 5 http://<Pod IP>:80/
```

- 若返回 `000`（超时）且 Pod 本身正常：按 4.7 补 NetworkPolicy。
- 跨节点仍不通时，优先使用 `network.openshift.io/policy-group: ingress` 标签 + Router 节点 IP `ipBlock`。

### 6.2 Pod 卡在 Init:0/1，事件报 Multi-Attach error

现象：事件提示 `Multi-Attach error for volume "pvc-xxx" ... Volume is already used by pod(s) ...`

原因：共享 PVC 为 ReadWriteOnce（RWO），同一时间只能被一个节点挂载；新 Pod 与已占用 PVC 的 Pod 调度到不同节点时挂载失败。

解决：

1. 确认 PVC 的 accessModes 为 RWO；
2. 给 Deployment 增加 podAffinity，让新 Pod 与占用 PVC 的 Pod 落在同一节点：

```yaml
spec:
  template:
    spec:
      affinity:
        podAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchLabels:
                  # 换成实际占用共享 PVC 的 Pod 标签
                  app: <占用共享卷的应用名>
              topologyKey: kubernetes.io/hostname
```

### 6.3 容器内无 vim / Tab 补全

- 构建镜像时安装 vim 与 bash-completion；
- Docker Desktop / OCP 交互终端默认可能使用 `/bin/sh`（dash），无补全；
- 建议在镜像中执行 `ln -sf /bin/bash /bin/sh`，并通过 `ENV ENV=/etc/bash_completion.env` 使 bash-completion 在交互式 sh 会话中自动加载。

## 7. 附录：常用命令

```bash
# 查看部署状态与事件
oc get deploy,rs,pods -n maximo-manager-panel
oc get events -n maximo-manager-panel --sort-by=.lastTimestamp | tail -20

# 查看日志
oc logs deploy/maximo-manager-panel -n maximo-manager-panel

# 进入容器
oc exec -it deploy/maximo-manager-panel -n maximo-manager-panel -- /bin/bash

# 滚动更新
oc rollout restart deploy/maximo-manager-panel -n maximo-manager-panel

# 回滚
oc rollout undo deploy/maximo-manager-panel -n maximo-manager-panel

# 删除
oc delete project maximo-manager-panel
```

## 修改代码后更新到ocp
```cmd
rem 1) 查看 dev Pod 名称，复制输出中的 Pod 名，替换下面命令里的 <POD>
set OCP_NAMESPACE=mas-dev-manage
oc get pod -n %OCP_NAMESPACE% -l app=dev-sksmas-tool

rem 根据查询的pod名称,设置实际的值
set OCP_PODNAME=dev-sksmas-tool-7cfb776f69-t8fqg

rem 2) 上传前端产物 dist -> 容器内 /usr/share/nginx/html
oc rsync .\dist\ %OCP_PODNAME%:/usr/share/nginx/html/ -n %OCP_NAMESPACE% -c panel

rem 3) 上传后端产物 app -> 容器内 /app
oc rsync .\server\target\lib %OCP_PODNAME%:/app/lib -n %OCP_NAMESPACE% -c panel
oc cp .\server\target\classes\app.yml %OCP_PODNAME%:/app/config/app-dev.yml -n %OCP_NAMESPACE% -c panel
oc cp .\server\target\solon-server-1.0.0.jar %OCP_PODNAME%:/app/solon-server-1.0.0.jar -n %OCP_NAMESPACE% -c panel

rem 4) 重建 Pod，让 entrypoint 重新渲染 nginx 配置、后端找到 jar 启动
oc delete pod %OCP_PODNAME% -n %OCP_NAMESPACE%
```

