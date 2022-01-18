# Nacos源码学习

​	因为最近项目在做容器化处理，容器化后涉及到不同进程对同一个文件的读写，考虑到可能会存在同一文件的配置文件，可能会把彼此覆盖掉，所以这里学习一下Nacos源码。

## 整体结构图

​	这边主要看配置中心、服务注册中心的源码，其他也会说，但不会那么细致。

![image-20220118104200367](nacos源码学习/image-20220118104200367.png)

## 源码解析

​	先进到NacosFactory来查看，Nacos工厂定义了配置服务和命名空间：（这里它没有用到的我就没有粘出来）

* createNamingService：创建配置服务
* createNamingService：创建命名空间

下面重点围绕着两个方面来讲源码

```java
public class NacosFactory {
    /**
     * 创建配置服务
     *
     * @param 属性-初始化参数
     * @return 配置
     * @throws NacosException Exception
     */
    public static ConfigService createConfigService(Properties properties) throws NacosException {
        return ConfigFactory.createConfigService(properties);
    }
    /**
     * 创造命名服务
     *
     * @param 服务列表
     * @return 命名服务
     * @throws NacosException Exception
     */
    public static NamingService createNamingService(String serverAddr) throws NacosException {
        return NamingFactory.createNamingService(serverAddr);
    }
    
    /**
     * 创造命名服务
     *
     * @param 服务列表
     * @return 命名服务
     * @throws NacosException Exception
     */
    public static NamingService createNamingService(Properties properties) throws NacosException {
        return NamingFactory.createNamingService(properties);
    }
}
```

### 配置服务源码

​	这一块源码，我们从nacos的测试的demo入手进行学习，这样学起来比较清晰。

* 先找到ConfigExample，配置服务示例（建议起一个Nacos服务，对着学源码），重点看下以下代码
  * 获取配置文件过程：使用nacos地址--->获取配置对象--->使用配置对象、文件名、组名获取配置信息
  * 设置监听器：使用配置对象、文件名、组名配置--->必须重写两方法
  * 更新配置信息：使用配置对象，把文件名、组名给上即可
  * 删除配置：使用配置对象，把文件名、组名给上即可

```java
public class ConfigExample {
    public static void main(String[] args) throws NacosException, InterruptedException {
        //*************获取配置信息****************
        String serverAddr = "localhost";  //设置nacos的地址
        String dataId = "test"; //文件名或者叫id也可以
        String group = "DEFAULT_GROUP"; //对应的组
        Properties properties = new Properties();//创造一个配置类
        properties.put("serverAddr", serverAddr);//将nacos服务地址设置进去
        //使用Nacos的ip地址，创建nacos配置服务对象（后面细说）
        ConfigService configService = NacosFactory.createConfigService(properties);
       //从配置服务对象获取配置内容（传入文件名、组名、超时时间）
        String content = configService.getConfig(dataId, group, 5000);
        System.out.println(content); //拿到的配置内容
       	//*************增加服务配置的监听器****************
        configService.addListener(dataId, group, new Listener() {
            //输出配置信息
            @Override
            public void receiveConfigInfo(String configInfo) {
                System.out.println("receive:" + configInfo);
            }
           
            //可以获取线程，当坚挺到配置的时候，执行某些任务
            @Override
            public Executor getExecutor() {
                return null;
            }
        });
		//*************推送新配置信息****************
        //判断推送是否成功
        boolean isPublishOk = configService.publishConfig(dataId, group, "content");
        System.out.println(isPublishOk);
		
        //*************获取新的配置信息****************
        Thread.sleep(3000);
        content = configService.getConfig(dataId, group, 5000);
        System.out.println(content);
		
        //*************获取新的配置信息****************
        boolean isRemoveOk = configService.removeConfig(dataId, group);
        System.out.println(isRemoveOk);
        Thread.sleep(3000);
		
        //再次获取配置信息确保已经删除
        content = configService.getConfig(dataId, group, 5000);
        System.out.println(content);
        Thread.sleep(300000);
    }
}
```

