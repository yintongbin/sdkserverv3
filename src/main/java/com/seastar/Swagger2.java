package com.seastar;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Created by wjl on 2016/8/19.
 */
@Configuration
@EnableSwagger2
public class Swagger2 {

    @Bean
    public Docket createRestApi() {
        // select()返回一个ApiSelectorBuilder实例用来控制哪些接口暴露给Swagger来展现，本例采用指定扫描的包路径来定义，
        // Swagger会扫描该包下所有Controller定义的API，并产生文档内容（除了被@ApiIgnore指定的请求）

        // 使用本文件后已经可以看到文档接口，还可以进一步美化
        // @ApiOperation注解来给API增加说明
        // @ApiImplicitParams、@ApiImplicitParam注解来给参数增加说明

        // http://localhost:端口/swagger-ui.html
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.seastar"))
                .paths(PathSelectors.any())
                .build();
    }

    // 用来创建该Api的基本信息
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Restful API 接口说明")
                .description("接口说明文档")
                .termsOfServiceUrl("")
                .contact("seastar")
                .version("1.0")
                .build();
    }
}
