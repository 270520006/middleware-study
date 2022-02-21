# apollo的学习

​	apollo类似于Nacos，都是注册中心的一种，但对于权限

## 安装使用

​	apollo的安装很麻烦，需要自身三个服务，并且还需要mysql支持，也就是一共需要四个服务，四个服务如下：

>- mysql
>- apollo-configservice
>- apollo-adminsrevice
>- apollo-portal

### 配置mysql

* 部署之前先将对应环境的Mysql服务启动起来，这里使用docker-compose启动Mysql服务，文件名为apollo-compose.yaml。

```yml
version: "3"
services:
 mysql-dev:
  image: mysql
  # restart: always
  environment:
   - MYSQL_ROOT_PASSWORD=123456
  expose:
   - "3306"
  volumes:
    - /home/apollo/apollo/scripts/sql:/sql     
```

*  mysql-portal启动并配置完成后开始启动apollo-portal服务：

  ```shell
   docker-compose -f apollo-compose.yaml up -d mysql-dev
  ```

apollo-compose.yaml是你机上docker-compose配置文件，mysql-dev是mysql服务的名称。

* 进入mysql的docker容器内读取配置文件

```shell
#进入docker容器
[root@localhost apollo]# docker exec -it 182e sh
#进入容器的数据库中
mysql -p123456
#导入两个表信息
#用于部署apollo-configservice和apollo-apolloportaldb
source /sql/apolloconfigdb.sql
#用于部署apollo-portal
source /sql/apolloportaldb.sql
```

* 导入完毕后，需要对数据库进行修改

```sql
use ApolloConfigDB；
update ServerConfig set Value="http://apollo-configservice-dev:8080/eureka/" where `key`="eureka.service.url";
```

apollo-configservice-dev是稍后我们需要发布的`apollo-configservice`服务的名称。修改完成之后可运行查询语句是否修改成功：

```sql
select * from ServerConfig;
```

修改完成之前退出dockre容器的连接就行了， 接下来部署`apollo-configservice`。

### 创建Config Service

* 直接连上数据库即可

```shell
docker pull apolloconfig/apollo-configservice

docker run -d \
    --name apollo-configservice \
    --net=host \
    -v /tmp/logs:/opt/logs \
    -e SPRING_DATASOURCE_URL="jdbc:mysql://8.129.217.148:3310/ApolloConfigDB?characterEncoding=utf8" \
    -e SPRING_DATASOURCE_USERNAME=root \
    -e SPRING_DATASOURCE_PASSWORD=123456 \
    -p 8080:8081 \
    -d \
    apolloconfig/apollo-configservice

```

* 创建Admin Service

```shell
docker pull apolloconfig/apollo-adminservice

docker run -d \
    --name apollo-adminservice \
    --net=host \
    -v /tmp/logs:/opt/logs \
    -e SPRING_DATASOURCE_URL="jdbc:mysql://8.129.217.148:3310/ApolloConfigDB?characterEncoding=utf8" \
    -e SPRING_DATASOURCE_USERNAME=root \
    -e SPRING_DATASOURCE_PASSWORD=123456 \
    apolloconfig/apollo-adminservice
```

* 创建Portal Server

```shell
docker pull apolloconfig/apollo-portal

docker run -d \
    --name apollo-portal \
    --net=host \
    -v /tmp/logs:/opt/logs \
    -e SPRING_DATASOURCE_URL="jdbc:mysql://8.129.217.148:3310/ApolloPortalDB?characterEncoding=utf8" \
    -e SPRING_DATASOURCE_USERNAME=root \
    -e SPRING_DATASOURCE_PASSWORD=123456 \
    -e APOLLO_PORTAL_ENVS=dev \
    -e DEV_META=http://192.168.56.101:8080 \
    apolloconfig/apollo-portal
```

至此，apollo搭建完成

