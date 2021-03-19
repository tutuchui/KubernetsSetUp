## 在Linux Server上搭建Kubernets

**本文档介绍了如何在Linux Server上使用kubeadm工具搭建自己的kubernets集群，该集群有两个节点，一个Master Node, 一个Worker Node。**



**前置准备：**两台或者两台以上Linux based的server

本文中使用的为google cloud中的compute engine实例，版本为：ubuntu 18.04,

CPU Core: 2, CPU memory: 4GB。

### 1. 在虚拟机实例上安装[容器进行时] - Docker

你需要在集群内每个节点上安装一个[容器运行时](https://kubernetes.io/zh/docs/setup/production-environment/container-runtimes) 以使 Pod 可以运行在上面。本文概述了所涉及的内容并描述了与节点设置相关的任务。

其他可选容器进行时包括

- containerd
- CRI-O



参考文档：

https://kubernetes.io/zh/docs/setup/production-environment/container-runtimes/#docker

https://docs.docker.com/engine/install/ubuntu/

**a. Update the `apt` package index and install packages to allow `apt` to use a repository over HTTPS:**

```bash
 sudo apt-get update
 
 sudo apt-get install \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release
```



**b. Add Docker’s official GPG key:**

```bash
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
```



**c. Use the following command to set up the stable repository. To add the nightly or test repository, add the word `nightly` or `test` (or both) after the word `stable` in the commands below. [Learn about nightly and test channels](https://docs.docker.com/engine/install/).**

```bash
 sudo add-apt-repository \
  "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) \
  stable"
```



**d. install docker CE**

```bash
sudo apt-get update && sudo apt-get install -y \
  containerd.io=1.2.13-2 \
  docker-ce=5:19.03.11~3-0~ubuntu-$(lsb_release -cs) \
  docker-ce-cli=5:19.03.11~3-0~ubuntu-$(lsb_release -cs)
```



**e. Set Docker daemon**

```bash
cat <<EOF | sudo tee /etc/docker/daemon.json
{
  "exec-opts": ["native.cgroupdriver=systemd"],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m"
  },
  "storage-driver": "overlay2"
}
EOF
```



**f. Create /etc/systemd/system/docker.service.d**

```bash
sudo mkdir -p /etc/systemd/system/docker.service.d
```



**g.  restart docker**

```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
```

**注意：**在默认情况下，都需要用管理员权限来运行docker相关命令，即所有的docker命令前都需要加上sudo才能正确运行，若需要需用sudo命令执行docker命令，需要执行以下代码

Create the docker group if it does not exist

```sh
$ sudo groupadd docker
```

Add your user to the docker group.

```sh
$ sudo usermod -aG docker $USER
```

Run the following command or Logout and login again and run (that doesn't work you may need to reboot your machine first)

```sh
$ newgrp docker
```





### 2. 安装&配置K8S集群

**1. 安装kubeadm, kubectl 和 kubelet工具**

```
sudo apt-get update && sudo apt-get install -y apt-transport-https curl
curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -
cat <<EOF | sudo tee /etc/apt/sources.list.d/kubernetes.list
deb https://apt.kubernetes.io/ kubernetes-xenial main
EOF
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
```



**2. 给每个节点设置hostname**

```bash
## 在master node上执行
sudo hostnamectl set-hostname master-node

## 在worker node上执行
sudo hostnamectl set-hostname worker01
```



**3. 初始化Master Node**

在Master Node上执行以下命令初始化master node

```bash
sudo kubeadm init --pod-network-cidr=10.244.0.0/16
```

将会得到类似于如下的输出：

```bash
Your Kubernetes control-plane has initialized successfully!

To start using your cluster, you need to run the following as a regular user:

  mkdir -p $HOME/.kube
  sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
  sudo chown $(id -u):$(id -g) $HOME/.kube/config

Alternatively, if you are the root user, you can run:

  export KUBECONFIG=/etc/kubernetes/admin.conf

You should now deploy a pod network to the cluster.
Run "kubectl apply -f [podnetwork].yaml" with one of the options listed at:
  https://kubernetes.io/docs/concepts/cluster-administration/addons/

Then you can join any number of worker nodes by running the following on each as root:

kubeadm join 10.170.0.2:6443 --token z5eyd6.wt8hg54u7wgiuyov \
    --discovery-token-ca-cert-hash sha256:22f77a9d8cd002e0ac552c7722a846f13b2ab01feecb9e3b01489b9c1283d6e7
```

在master node上运行

```bash
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```



**4. 在Cluster上部署Pod Network**

可选的podnetwork配置可在 https://kubernetes.io/docs/concepts/cluster-administration/addons/上找到，本文档中选用kube-flannel.yaml配置。

注：kube-flannel.yaml是从互联网上下载到虚拟机实例本地的文件，如果需要用线上版本需要使用正确的url

```bash
sudo kubectl apply -f kube-flannel.yaml
```

可以通过命令`kubectl get pods --all-namespaces` 来确认network pods是否成功工作，得到输出类似如下：

```bash
NAMESPACE     NAME                                     READY   STATUS    RESTARTS   AGE
kube-system   coredns-74ff55c5b-8zfkv                  1/1     Running   0          6m32s
kube-system   coredns-74ff55c5b-c5zlh                  1/1     Running   0          6m32s
kube-system   etcd-kubernets-demo                      1/1     Running   0          6m49s
kube-system   kube-apiserver-kubernets-demo            1/1     Running   0          6m49s
kube-system   kube-controller-manager-kubernets-demo   1/1     Running   0          6m49s
kube-system   kube-flannel-ds-645p2                    1/1     Running   0          18s
kube-system   kube-flannel-ds-xps9k                    1/1     Running   0          18s
kube-system   kube-proxy-h26dk                         1/1     Running   0          6m32s
kube-system   kube-proxy-pnlzp                         1/1     Running   0          4m12s
kube-system   kube-scheduler-kubernets-demo            1/1     Running   0          6m49s
```



**5. 将worker node加入到cluster之中**

初始化master node之后，命令行会输出将worker node加入的cluster指令，在worker node上输入相应指令

```bash
### 本次配置过程中使用的指令
sudo kubeadm join 10.170.0.2:6443 --token z5eyd6.wt8hg54u7wgiuyov \
    --discovery-token-ca-cert-hash sha256:22f77a9d8cd002e0ac552c7722a846f13b2ab01feecb9e3b01489b9c1283d6e7
```

可以在master node上通过执行`kubectl get nodes`来查看worker node是否成功加入cluster, 得到结果如下：

```bash
NAME             STATUS   ROLES                  AGE     VERSION
kubernets-demo   Ready    control-plane,master   6m56s   v1.20.4
worker01         Ready    <none>                 4m15s   v1.20.4
```



### 3. 部署Spring Boot项目

#### **1. 部署对外网暴露的Spring boot项目**

本文档中部署的能够被外网访问的spring boot项目名为api-gateway-demo,其作用是提供一个能够被外网访问的一个API接口。

该项目中提供了一个接口，其作用是调用另一个在cluster中不能够被外网访问的项目（decision-demo) 的接口

- Pull docker remote repository

  将对应的镜像文件从Docker remote repository上pull下来

  ```bash
  docker pull [IMAGE_NAME]
  
  ## docker pull tubichui/api-gateway-demo
  ```

- 编写deployment.yaml和service.yaml配置文件

```yaml
# api-gateway-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway-demo
  labels:
    app: api-gateway-demo
spec:
  selector:
    matchLabels:
      app: api-gateway-demo
  replicas: 2 # tells deployment to run 2 pods matching the template
  template:
    metadata:
      labels:
        app: api-gateway-demo
    spec:
      containers:
        - name: api-gateway-demo
          image: tubichui/api-gateway-demo
          ports:
            - containerPort: 8080
```

```yaml
# api-gateway-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: api-gateway-service
spec:
  type: NodePort #如果这个service需要被外网访问到，需要将type定义为NodePort类型
  selector:
    app: api-gateway-demo
  ports:
  		#在集群内部，这个service在哪个port对外暴露
    - port: 8080
    	# 对应的pod expose在哪个端口，与deployment.yaml中container port相对应
      targetPort: 8080
      #声明node port被分配的端口号
      #默认情况下，为了方便起见，Kubernetes 控制平面会从某个范围内分配一个端口号（默认：30000-32767）
      # Which port on the node is the service available through
      nodePort: 30940
```

service yaml中spec.selector.app的名字必须和deployment.yaml中定义的spec.template.labels.app名字保持一致，service.yaml正是通过这个selector来决定将请求交给哪一个pod来处理。

- 应用deployment.yaml和service.yaml文件

```bash
kubectl apply -f api-gateway-deployment.yaml
kubectl apply -f api-gateway-service.yaml
```

**注意：**生成的pods需要被分配到available的worker-node上才能正常的运行。在默认的情况下pods是不会被分配到master node上进行工作。

如果需要使在master node上部署pods，可以使用以下命令

```bash
kubectl taint nodes --all node-role.kubernetes.io/master-
```

- 查看deployments是否被正确的部署

```bash
kubectl get deployments

##output:
NAME               READY   UP-TO-DATE   AVAILABLE   AGE
api-gateway-demo   2/2     2            2           16m
```

- 查看service部署的情况

```bash
kubectl get services

##output:
NAME                  TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
api-gateway-service   NodePort    10.110.159.50   <none>        8080:30940/TCP   17m
kubernetes            ClusterIP   10.96.0.1       <none>        443/TCP          48m
```

现在对外暴露的接口应该能够在http://[VM_EXTERNAL_IP_ADDRESS]:30940被访问

e.g. http://34.92.185.187:30940/

**注意：务必放开虚拟机实例在该端口号TCP协议下的访问防火墙约束**



#### 2. 部署只能够在cluster中被访问的Spring Boot项目

本文档中部署的只能够被cluster集群中项目访问的spring boot项目名为decision-demo, 该项目的功能是提供只能够被集群中的项目访问的接口，在K8S中，只能在cluster中被访问的接口，K8S提供其内置的dns name机制来支持访问，详细文档：https://kubernetes.io/zh/docs/concepts/services-networking/dns-pod-service/

项目部署的流程与上面api-gateway-demo项目类似，需要注意的为service.yaml中配置

```bash
## decision-service.yaml

apiVersion: v1
kind: Service
metadata:
  name: decision-service # 该字段申明了该服务被内部访问的DNS name
spec:
  type: ClusterIP # 如果该服务只能被cluster内部访问，在service的type需要为声明为ClusterIP
  selector:
    app: decision-demo
  ports:
    # 默认情况下，为了方便起见，`targetPort` 被设置为与 `port` 字段相同的值。
    # 该字段定义了cluster内部访问的端口号
    - port: 9898
      targetPort: 9898
```

此时decision-demo可以被api-gateway-demo使用相应的dns name来访问。

Api-Gateway-Demo中的代码

```java
@GetMapping("/helloDecision")
public String helloDecision() throws IOException {
		URL decisionURL = new URL("http://decision-service:9898/testCommunicate?userName=Yutong"); //Use the DNS name defined in the service.yaml file
    String decisionResponse = ApiUtil.sendGetRequest(decisionURL); // Send a Http get request. 
    return decisionResponse;
}
```

Decision-Demo中的代码

```java
@GetMapping("/testCommunicate")
public String sendResponse(@RequestParam("userName") String userName){
    String response = "Receive Message From " + userName;
    return response;
}
```



当Decision Demo被成功部署之后，访问http://34.92.185.187:30940/helloDecision，能够对应的显示

"Receive Message From Yutong".

### 4. Scale the Application

参考文档：https://kubernetes.io/zh/docs/tutorials/kubernetes-basics/scale/scale-interactive/

**Checking the current pod of a specific application**

```bash
kubectl get pods -l=app=[application-name]
## kubectl get pods -l=app=decision-demo
```

**改变application的使用的Pods的数量**

```bash
kubectl scale deployments/[deployment-name] --replicas=#number-of-replicas
# kubectl scale deployments/api-gateway-demo --replicas=4
```

**Create an auto scale controller for a deployment**

```bash
kubectl autoscale deployment api-gateway-demo --min=#min_number --max=max_number
### kubectl autoscale deployment api-gateway-demo --min=6 --max=10
```

**Edit existed auto scale controller**

```bash
kubectl edit hpa [hpa-name]
### kubectl edit hpa api-gateway-demo
```

**Check the log of a specific pod**

```bash
kubectl logs [pod-name]

## kubectl logs api-gateway-demo-68454697d8-9gsvk
```



### 5. Kubernets Services

Kubernets中使用Pods来承载我们的我docker image instance (i.e., container)。**而如果需要对外暴露这个Application, 则需要使用kubernets service。** pod在kubernets的内部属于临时资源，由kubernets自己控制删除和重建，当一个Pod重建的时候，它的Internal IP address也会改变。而Service则相应的拥有一个稳定的IP地址。

Kubernets Service的类型：

- **ClusterIP** Services: default service type. 在一个kubernets集群中，每个worker node都会被分配到一定范围的IP地址，pod根据自身的internal IP地址来决定被分配到哪个具体的worker node上进行工作。该类型主要用于只需要在集群内部相互访问的服务。T
  - ClusterIP is accessible only inside the cluster, no external traffic can directly address the clusterIP service. 
- **Headless** Service：
  - Clients want to communicate with 1 specific Pod directly.
  - Pods want to talk directly with specific pod
  - Use case: Stateful application, like database (e.g., mysql, oracle, mongoDB)
- **NodePort** Service: 
  - NodePort is an extension of ClusterIP service.
  - Create a service that is accessible on a static port on the each worker node in the cluster. 
  - Make the external traffic accessible on static fixed port on each worker node
  - nodePort value has a pre-define range between 30000 - 32767
  - **Not Secure!** Outside client could talk to the Worker Node directly. Could used for some test cases, but not for production use cases. 
- **LoadBalancer** Service:
  - LoadBalancer is an extension of NodePort service
  - Become accessible externally through **cloud provider LoadBalancer**
  - **Secure.** Should be used for production case. 

### A. [Optioanal]使用本地Terminal连接虚拟机实例（Mac OS）

通过本地terminal使用ssh秘钥连接虚拟机实例

#### a. 生成ssh钥匙对

```bash
ssh-keygen -t rsa -f ~/.ssh/[KEY_FILENAME] -C [USERNAME]

## e.g. ssh-keygen -t rsa -f ~/.ssh/master-ssh-key -C kubernets-demo
```



#### b.在虚拟机实例中注册公钥

在~/.ssh/文件夹中找到生成的ssh key公钥（master-ssh-key.pub)，将公钥内容添加到虚拟机实例的公钥设置中。



#### c. 连接虚拟机实例

通过以下命令连接虚拟机实例

```bash
ssh -i PATH_TO_PRIVATE_KEY USERNAME@EXTERNAL_IP

## e.g. ssh -i /Users/yutongwang/.ssh/master-ssh-key kubernets-demo@34.92.185.187
```



### B. [Optional] 通过FileZilla连接上虚拟机实例进行文件传输

在FileZilla中按照如下配置配置一个新的站点：

![image-20210315145348181.png](http://34.92.185.187:8080/pictures/test-image.png)



IP地址：虚拟机external ip address

密钥文件：选择A步骤中生成的对应密钥文件

用户：虚拟机实例的用户名