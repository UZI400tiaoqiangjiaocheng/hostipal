package com.my.hostipal.config;

import com.alipay.easysdk.kernel.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliPayConfig {

    @Bean
    public Config config(AliPayProperties aliPayProperties) {
        Config config = new Config();
        config.protocol = aliPayProperties.getProtocol();
        config.gatewayHost = aliPayProperties.getGatewayHost();
        config.signType = aliPayProperties.getSignType();
        config.appId = aliPayProperties.getAppId();
        config.merchantPrivateKey = aliPayProperties.getMerchantPrivateKey();
        config.alipayPublicKey = aliPayProperties.getAlipayPublicKey();
        config.notifyUrl = aliPayProperties.getNotifyUrl();
        config.encryptKey = "";
        return config;
    }
}
