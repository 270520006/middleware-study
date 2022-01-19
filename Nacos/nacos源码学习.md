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

​	这一块源码，我们从nacos的测试的demo入手进行学习，先总概整体过程再拆分出来一个个剖析。

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

下面把上面的部分拆开细讲。

#### 获取配置文件

* ctrl+鼠标左键点击getConfig进入源码查看

```java
        Thread.sleep(3000);
        content = configService.getConfig(dataId, group, 5000);
        System.out.println(content);
```

* 点击查看实现类

![image-20220119100333574](nacos源码学习/image-20220119100333574.png)

* 进入getConfigInner进去继续查看

```java
    @Override
    public String getConfig(String dataId, String group, long timeoutMs) throws NacosException {
        return getConfigInner(namespace, dataId, group, timeoutMs);
    }
```

* 开始剖析getConfigInner，直接看注释即可
  * 特别注意下：tenant和namespace是一个东西，从nacos的显示的日志可以看出。
  * 整个过程如下：

>* 先从本地磁盘中加载配置，因为应用在启动时，会加载远程配置缓存到本地，如果本地文件的内容不为空，直接返回。
>* 如果本地文件的内容为空，则调用worker.getServerConfig加载远程配置
>* 如果出现异常，则调用本地快照文件加载配置

```java
    private String getConfigInner(String tenant, String dataId, String group, long timeoutMs) throws NacosException {
        //判断group是否为空，为空则设为默认
        group = blank2defaultGroup(group);
        //校验dataId、group保证不为空
        ParamUtils.checkKeyParam(dataId, group);
        //创建配置响应对象
        ConfigResponse cr = new ConfigResponse();
        
    	//设置文件id、命名空间、组
        cr.setDataId(dataId);
        cr.setTenant(tenant);
        cr.setGroup(group);
        
        //优先加载本地配置
        String content = LocalConfigInfoProcessor.getFailover(worker.getAgentName(), dataId, group, tenant);
        if (content != null) {
            //如果本地内容不为空，则告知从本地加载成功（后面细说）
            LOGGER.warn("[{}] [get-config] get failover ok, dataId={}, group={}, tenant={}, config={}",
                    worker.getAgentName(), dataId, group, tenant, ContentUtils.truncateContent(content));
           //将加载到的内容放入配置对象
            cr.setContent(content);
            //获取容灾配置的EncryptedDataKey
            String encryptedDataKey = LocalEncryptedDataKeyProcessor
                    .getEncryptDataKeyFailover(agent.getName(), dataId, group, tenant);
            //放入容灾配置的EncryptedDataKey
            cr.setEncryptedDataKey(encryptedDataKey);
            //过滤链
            configFilterChainManager.doFilter(null, cr);
            //从响应对象重新获取配置内容
            content = cr.getContent();
            //返回配置内容
            return content;
        }
        
        try {
            //加载远程配置，获取配置对象（后面细说）
            ConfigResponse response = worker.getServerConfig(dataId, group, tenant, timeoutMs, false);
            //从远程配置里获取配置内容，并设置
            cr.setContent(response.getContent());
            //从配置响应对象获取EncryptedDataKey，并设置EncryptedDataKey
            cr.setEncryptedDataKey(response.getEncryptedDataKey());
            //过滤链
            configFilterChainManager.doFilter(null, cr);
            //从响应对象重新获取配置内容
            content = cr.getContent();
            //返回配置内容
            return content;
        } catch (NacosException ioe) {
            //出现问题的情况：
            //请求失败、配置正在被删除、配置已不存在
            if (NacosException.NO_RIGHT == ioe.getErrCode()) {
                throw ioe;
            }
            LOGGER.warn("[{}] [get-config] get from server error, dataId={}, group={}, tenant={}, msg={}",
                    worker.getAgentName(), dataId, group, tenant, ioe.toString());
        }
        
        LOGGER.warn("[{}] [get-config] get snapshot ok, dataId={}, group={}, tenant={}, config={}",
                worker.getAgentName(), dataId, group, tenant, ContentUtils.truncateContent(content));
        //从快照中获取（后面细说）
        content = LocalConfigInfoProcessor.getSnapshot(worker.getAgentName(), dataId, group, tenant);
        //放入获取到的配置内容
        cr.setContent(content);
        //获取encryptedDataKey
        String encryptedDataKey = LocalEncryptedDataKeyProcessor
                .getEncryptDataKeyFailover(agent.getName(), dataId, group, tenant);
        //向配置响应对象里放入encryptedDataKey
        cr.setEncryptedDataKey(encryptedDataKey);
        //过滤链
        configFilterChainManager.doFilter(null, cr);
        //重新获取配置内容
        content = cr.getContent();
        //返回配置内容
        return content;
    }

```

##### 本地配置加载

* 从getFailover进入查看本地配置源码

```java
        // use local config first
        String content = LocalConfigInfoProcessor.getFailover(worker.getAgentName(), dataId, group, tenant);
```

* 进入getFailover查看
  * 获取本地配置地址，根据命名空间进行拼串
    * 没有命名空间：/${serverName}_nacos/data/config-data
    * 存在命名空间：/${serverName}_nacos/data/config-data-tenant/${tenant}
  * 进入readFile，继续查看

```java
    public static String getFailover(String serverName, String dataId, String group, String tenant) {
        File localPath = getFailoverFile(serverName, dataId, group, tenant);
        if (!localPath.exists() || !localPath.isFile()) {
            //如果文件不存在则返空
            return null;
        }        
        try {
            //读取文件
            return readFile(localPath);
        } catch (IOException ioe) {
            LOGGER.error("[" + serverName + "] get failover error, " + localPath, ioe);
            return null;
        }
    }
```

* 进入readFile方法继续查看

```java
    protected static String readFile(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            //判断文件路径对应的文件是否存在，不存在则返空
            return null;
        }
        //判断是否为多实例，多实例则采取文件锁获取
        if (JvmUtil.isMultiInstance()) {
            return ConcurrentDiskUtil.getFileContent(file, Constants.ENCODE);
        } else {//否则使用正常的文件打开方式读取配置即可
            try (InputStream is = new FileInputStream(file)) {
                return IoUtils.toString(is, Constants.ENCODE);
            }
        }
    }
```

* 进入getFileContent方法查看

```java
    public static String getFileContent(File file, String charsetName) throws IOException {
        RandomAccessFile fis = null; //创建一个随机流
        FileLock rlock = null;	//创建一个文件锁
        try {
            //因为只读取配置，所以权限为“只读”
            fis = new RandomAccessFile(file, READ_ONLY);
            FileChannel fcin = fis.getChannel();
            int i = 0;
            do {
                try {
                    //尝试获取该文件的文件锁，如果获取不到则返回null
                    rlock = fcin.tryLock(0L, Long.MAX_VALUE, true);
                } catch (Exception e) {
                    //没有获取到则抛出异常
                    ++i;
                    //如果尝试10次还获取不到锁则抛出异常
                    if (i > RETRY_COUNT) {
                        LOGGER.error("read {} fail;retryed time:{}", file.getName(), i);
                        throw new IOException("read " + file.getAbsolutePath() + " conflict");
                    }
                    //休眠10ms*次数
                    sleep(SLEEP_BASETIME * i);
                    LOGGER.warn("read {} conflict;retry time:{}", file.getName(), i);
                }
            } while (null == rlock);//自旋锁，一直尝试获取锁
            //获取当前通道大小
            int fileSize = (int) fcin.size();
            //
            ByteBuffer byteBuffer = ByteBuffer.allocate(fileSize);
            fcin.read(byteBuffer);
            byteBuffer.flip();
            return byteBufferToString(byteBuffer, charsetName);
        } finally {
            if (rlock != null) {
                rlock.release();
                rlock = null;
            }
            if (fis != null) {
                IoUtils.closeQuietly(fis);
                fis = null;
            }
        }
    }

```

##### 远程配置加载

##### 使用快照加载

