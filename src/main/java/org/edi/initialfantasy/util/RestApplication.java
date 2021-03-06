package org.edi.initialfantasy.util;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.edi.initialfantasy.filter.RequestFilter;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Created by asus on 2018/5/31.
 */
public class RestApplication extends ResourceConfig {
    public RestApplication() {

        //服务类所在的包路径
        packages("org.edi.initialfantasy.service");
        //注册JSON转换器
        register(JacksonJsonProvider.class);
        register(RequestFilter.class);
    }
}
