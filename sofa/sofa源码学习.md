# sofa源码学习

​	要想更好的学习sofa源码，就得对sofa的整个框架体系有一定的了解：

![img](sofa源码学习/70.png)

## 远程调用源码

​	先举个远程调用的例子，然后再来慢慢说：

* 远程调用要使用的接口

```java
public class HelloSyncServiceImpl implements HelloSyncService {
    @Override
    public String saySync(String msg) {
        System.out.println("Server receive: " + msg);
        return "hello " + msg + " ！";
    }
}
```

* 建立客户端

```java
public class QuickStartServer {
    public static void main(String[] args) {
        ServerConfig serverConfig = new ServerConfig()
                .setProtocol("bolt") // 设置一个协议，默认bolt
                .setPort(12200) // 设置一个端口，默认12200
                .setDaemon(false); // 非守护线程

        ProviderConfig<HelloSyncService> providerConfig = new ProviderConfig<HelloSyncService>()
                .setInterfaceId(HelloSyncService.class.getName()) // 指定接口
                .setRef(new HelloSyncServiceImpl()) // 指定实现
                .setServer(serverConfig); // 指定服务端

        providerConfig.export(); // 发布服务
    }
}
```

* 建立服务端

```java
public class QuickStartClient {
    public static void main(String[] args) {
        ConsumerConfig<HelloSyncService> consumerConfig = new ConsumerConfig<HelloSyncService>()
                .setInterfaceId(HelloSyncService.class.getName()) // 指定接口
                .setProtocol("bolt") // 指定协议
                .setDirectUrl("bolt://127.0.0.1:12200"); // 指定直连地址
        // 生成代理类
        HelloSyncService helloService = consumerConfig.refer();
        while (true) {
            System.out.println(helloService.saySync("world"));
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
        }
    }
}

```

* 服务的入口是：接下来都围绕这个来说

```java
        HelloSyncService helloService = consumerConfig.refer();
```

