import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import service.HelloService;
import service.impl.HelloServiceImpl;

import java.util.ArrayList;

public class NacosStartServer {
    public static void main(String[] args) {
//        # 构造服务注册中心配置
        RegistryConfig registryConfig = new RegistryConfig()
                .setProtocol("nacos")
                .setSubscribe(true)
                .setAddress("26.26.26.1:8848")
                .setRegister(true);

//# 构造服务端口配置
        ServerConfig serverConfig = new ServerConfig()
                .setProtocol("bolt")
                .setHost("0.0.0.0")
                .setPort(12200);

//# 构造服务发布者
        ArrayList<RegistryConfig> list = new ArrayList<>();
        list.add(registryConfig);
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
                .setInterfaceId(HelloService.class.getName())
                .setRef(new HelloServiceImpl())
                .setServer(serverConfig)
                .setRegister(true)
                .setRegistry(list);
        providerConfig.export();

    }
}
