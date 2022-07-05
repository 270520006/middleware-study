# Spring扩展

​	spring扩展主要基于spring源码学习过程中发现预留下的bean操作的子类扩展方法。这份笔记主要讲述的就是这些方法，以此加固对spring的掌握。

## springboot扩展

​	spring留下了一些扩展点，我们可以用来做功能拓展，例如AbstractApplicationContext类的initPropertySources、postProcessBeanFactory、onRefresh，在spring-boot里也同样留了这样的扩展点。

* 原本的spring要注入bean，需要在主启动类指定需要解析的bean,解释一下大体意思：
  * 使用ClassPathXmlApplicationContext从指定的xml获取bean的位置
  * 使用context的getbean方法获取xml里对应的实现类，这边如果是其他类就把simple改掉即可

```java
public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
        Simple bean = context.getBean(Simple.class);
        bean.execute();
        context.close();
    }
```

* 上面就是最基本的spring使用bean注入的方法，下面来说一说spring boot的bean注入，先来找下springboot留的拓展点。

```java
@SpringBootApplication
public class SpringStudyApplication {
    public static void main(String[] args) {
   		//两种启动方式,但第一种较为直白
        //第一种方式
        SpringApplication springApplication = new SpringApplication(SpringStudyApplication.class);
        springApplication.run(args);
        //第二种方式
//        SpringApplication.run(SpringStudyApplication.class, args);
    }

}
```

* 直接进入run方法查看,会看到createApplicationContext方法，这个方法就是用于bean注册的。

```java
	public ConfigurableApplicationContext run(String... args) {
		StopWatch stopWatch = new StopWatch();
        //用于统计事件完成程度的，类似于log日志，start代表开始事件，stoop代表停止
		stopWatch.start();
		ConfigurableApplicationContext context = null;
		Collection<SpringBootExceptionReporter> exceptionReporters = new ArrayList<>();
		configureHeadlessProperty();
        //获取监听器
		SpringApplicationRunListeners listeners = getRunListeners(args);
		//springboot获取并开始监听
        listeners.starting();
		try {
           	//准备springboot运行环境
			ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
			ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);
			configureIgnoreBeanInfo(environment);
			Banner printedBanner = printBanner(environment);
            //创建ApplicationContext容器（最主要的）
			context = createApplicationContext();
			//创建ApplicationContext容器
			exceptionReporters = getSpringFactoriesInstances(SpringBootExceptionReporter.class,
					new Class[] { ConfigurableApplicationContext.class }, context);
			//准备容器
            prepareContext(context, environment, listeners, applicationArguments, printedBanner);
			//刷新容器
            refreshContext(context);
            //刷新后的处理
			afterRefresh(context, applicationArguments);
            
			stopWatch.stop();
			if (this.logStartupInfo) {
				new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), stopWatch);
			}
			listeners.started(context);
			callRunners(context, applicationArguments);
		}
		catch (Throwable ex) {
			handleRunFailure(context, ex, exceptionReporters, listeners);
			throw new IllegalStateException(ex);
		}

		try {
			listeners.running(context);
		}
		catch (Throwable ex) {
			handleRunFailure(context, ex, exceptionReporters, null);
			throw new IllegalStateException(ex);
		}
		return context;
	}

```

* 进入createApplicationContext方法查看：从代码来看，只要刚开始没注入，继承下面三个类中的任一个就可以

  * AnnotationConfigServletWebServerApplicationContext
  * AnnotationConfigReactiveWebServerApplicationContext
  * AnnotationConfigApplicationContext

  这三个类都实现了AnnotationConfigRegistry接口，同时实现initPropertySources、postProcessBeanFactory、onRefresh三种方法，初始化诶，

```java
protected ConfigurableApplicationContext createApplicationContext() {
    //从applicationContextClass中获取class对象
   Class<?> contextClass = this.applicationContextClass;
    //如果class对象为空为空，就从指定类里面获取
   if (contextClass == null) {
      try {
         switch (this.webApplicationType) {
//org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext
         case SERVLET:
            contextClass = Class.forName(DEFAULT_SERVLET_WEB_CONTEXT_CLASS);
            break;
//org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext
                 
         case REACTIVE:
            contextClass = Class.forName(DEFAULT_REACTIVE_WEB_CONTEXT_CLASS);
            break;
         default:
//org.springframework.context. annotation.AnnotationConfigApplicationContext
            contextClass = Class.forName(DEFAULT_CONTEXT_CLASS);
         }
      }
      catch (ClassNotFoundException ex) {
         throw new IllegalStateException(
               "Unable create a default ApplicationContext, please specify an ApplicationContextClass", ex);
      }
   }
   return (ConfigurableApplicationContext) BeanUtils.instantiateClass(contextClass);
}
```

* 下面举个例子说明，以下是项目结构：