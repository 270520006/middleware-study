# dubbo源码解析

​	这里的dubbo源码是基于dubbo2.6.2的，用其他版本可能会有所不同，请注意！尤其dubbo3.0和2.0的差别很大，请注意！特别注意，这里的进入方法就是：ctrl+鼠标左键

## dubbo配置解析

* 首先找到程序入口，在dubbo-demo下的dubbo-demo-consumer中的consumer文件中：

![image-20211105163013383](dubbo源码解析/image-20211105163013383.png)

​	进入查看代码发现依赖了一个配置类，META-INF/spring/dubbo-demo-consumer.xml。

```java
    public static void main(String[] args) {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();
        DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy

        while (true) {
            try {
                Thread.sleep(1000);
                String hello = demoService.sayHello("world"); // call remote method
                System.out.println(hello); // get result

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }
```

* 追踪META-INF/spring/dubbo-demo-consumer.xml这个配置类，我们发现它引用的xsd文件包括spring的**spring-beans-4.3.xsd文件和dubbo.xsd，**这两个xsd文件分别定义了xml文件的解析规则**。 例如看dubbo: reference，在xsd文件中可以看到reference对应的配置，对应ReferenceConfig类。**所以我们现在要追踪的是：
  * dubbo.xsd
  * spring-beans-4.3.xsd

```xml
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"
       xmlns="http://www.springframework.org/schema/beans"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.3.xsd
       http://dubbo.apache.org/schema/dubbo http://dubbo.apache.org/schema/dubbo/dubbo.xsd">

    <!-- consumer's application name, used for tracing dependency relationship (not a matching criterion),
    don't set it same as provider -->
    <dubbo:application name="dub-consumer"/>

    <!-- use multicast registry center to discover service -->
    <dubbo:registry address="multicast://224.5.6.7:1234"/>

    <!-- generate proxy for the remote service, then demoService can be used in the same way as the
    local regular interface -->
    <dubbo:reference id="demoService" check="false" interface="com.alibaba.dubbo.demo.DemoService"/>
</beans>
```

* ctrl+鼠标左键进入到dubbo.xsd，这里主要查看

  * service
  * reference

  注：这里的\<xsd:annotation>使用DubboBeanDefinitionParser方法转译成的bean，所以看不懂，往下看即可。

```xml
    <xsd:element name="service" type="serviceType">
        <xsd:annotation>
            <xsd:documentation><![CDATA[ Export service config ]]></xsd:documentation>
        </xsd:annotation>
    </xsd:element>

    <xsd:element name="reference" type="referenceType">
        <xsd:annotation>
            <xsd:documentation><![CDATA[ Reference service config ]]></xsd:documentation>
        </xsd:annotation>
    </xsd:element>
```

* 找到dubbo.xsd的上级目录下的spring.handles

spring.handles

```properties
http\://dubbo.apache.org/schema/dubbo=com.alibaba.dubbo.config.spring.schema.DubboNamespaceHandler
http\://code.alibabatech.com/schema/dubbo=com.alibaba.dubbo.config.spring.schema.DubboNamespaceHandler
```

* 因为handles这里配置了DubboNamespaceHandler，进入这个类查看：可以发现这个类使用了DubboBeanDefinitionParser帮我们解析上面的标签,并且自定义了ServiceBean和ReferenceBean。

内部机制都是依托于<dubbo:annotation />标签。 通过源码分析，Dubbo对于Spring xml解析处理由      com.alibaba.dubbo.config.spring.schema.DubboNamespaceHandler提供：DubboNamespaceHandler.java

```java
public class DubboNamespaceHandler extends NamespaceHandlerSupport {

    static {
        Version.checkDuplicate(DubboNamespaceHandler.class);
    }	
    @Override
    public void init() {
        registerBeanDefinitionParser("application", new DubboBeanDefinitionParser(ApplicationConfig.class, true));
        registerBeanDefinitionParser("module", new DubboBeanDefinitionParser(ModuleConfig.class, true));
        registerBeanDefinitionParser("registry", new DubboBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("monitor", new DubboBeanDefinitionParser(MonitorConfig.class, true));
        registerBeanDefinitionParser("provider", new DubboBeanDefinitionParser(ProviderConfig.class, true));
        registerBeanDefinitionParser("consumer", new DubboBeanDefinitionParser(ConsumerConfig.class, true));
        registerBeanDefinitionParser("protocol", new DubboBeanDefinitionParser(ProtocolConfig.class, true));
        registerBeanDefinitionParser("service", new DubboBeanDefinitionParser(ServiceBean.class, true));
        registerBeanDefinitionParser("reference", new DubboBeanDefinitionParser(ReferenceBean.class, false));
        registerBeanDefinitionParser("annotation", new AnnotationBeanDefinitionParser());
    }
}
```

* 进入registerBeanDefinitionParser这个类继续查看， 

