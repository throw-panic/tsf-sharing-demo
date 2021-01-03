---
title: tsf 部分组件介绍
date: 2020-12-07 15:39:59
tags: springCloud
categories: 微服务
---


>
> tsf 相关组件介绍：ribbon、feign、resilience4j、zuul & scg、sleuth
>


 <!--more-->




# 一、 ribbon 负载均衡器

为了保证服务的可用性，通常每个服务部署多个实例，避免因为单个实例挂掉之后，导致整个服务不可用。同时，提升了服务的承载能力，处理更多的请求。此时，就要考虑到请求均衡合理的分配，保证每个服务实例的负载。


## 1.1 负载均衡实现方式

1. 通常，有两种方式实现服务的负载均衡，分别是**客户端级别和服务端级别**。
2. 服务端级别的负载均衡，客户端通过外部的代理服务器，将请求转发到后端的多个服务。比较常见的有 Nginx 服务器。
3. 客户端级别的负载均衡，客户端通过内嵌的“代理”，将请求转发到后端的多个服务。比较常见的有 Dubbo、Ribbon 框架提供的负载均衡功能。
4. 客户端级别的负载均衡可以有更好的性能，因为不需要多经过一层代理服务器。并且，服务端级别的负载均衡需要额外考虑代理服务的高可用，以及请求量较大时的负载压力。因此，在微服务场景下，一般采用客户端级别的负载均衡为主。


### 示例 1 ribbon 负载均衡

在默认配置下，Ribbon 采用 ZoneAvoidanceRule 负载均衡策略，在未配置所在区域的情况下，和轮询负载均衡策略是相对等价的。所以服务消费者调用服务提供者时，顺序将请求分配给每个实例。


## 1.2 ribbon 提供的7种负载均衡策略

1. RandomRule: **随机选择一个 server**(在 index 上随机，选择 index 对应位置的 Server);
2. RoundRobinRule：**轮询选择 server**(轮询 index，选择 index 对应位置的 server);
3. BestAvailableRule：**选择一个最小并发请求的 server**逐个考察 server，如果 server 被 tripped 了则忽略，在选择其中activeRequestsCount 最小的 server。
4. AvailabilityFilteringRule：**过滤掉那些因为一直连接失败的被标记为 circuit tripped 的后端 server**，并过滤掉那些高并发的的后端 server ,activeConnections 超过配置的阈值)使用一个 AvailabilityPredicate 来包含过滤 server 的逻辑，其实就就是检查 status 里记录的各个 server 的运行状态。
5. WeightedResponseTimeRule：**根据 server 的响应时间分配一个 weight，响应时间越长，weight 越小，被选中的可能性越低**。	一个后台线程定期的从 status 里面读取评价响应时间，为每个 server 计算一个 weight。weight 的计算也比较简单，responseTime 减去每个 server 自己平均的 responseTime 是 server 的权重。当刚开始运行，没有形成 status 时，使用 RoundRobinRule 策略选择 server。

### 1.2.1 ZoneAvoidanceRule（默认策略）

**复合判断 server 所在区域的性能和 server 的可用性选择 server。**使用 ZoneAvoidancePredicate 和 AvailabilityPredicate 来判断是否选择某个 server。ZoneAvoidancePredicate 判断判定一个 zone 的运行性能是否可用，剔除不可用的 zone（剔除不可用 zone 的所有 server）; AvailabilityPredicate 用于过滤掉连接数过多的 server。


1. **选择区域 zone**
  - 所属实例数为 0 的 zone, 实例故障率（断路器断开次数/实例数）大于等于阈值（默认0.99999），直接剔除选择;
  - 根据zone 区域的实例平均负载，计算出来最差的区域 zone，这里的最差指定是负载最高的区域;(往后应该是负载越高，越少的路由过来)；
  - 如果没有符合剔除要求的区域，同时实例最大平均负载小于阈值（默认20%），就直接返回所有区域为可用区。否则，从最坏 zone 区域集合中随机选择一个，将它从 zone 可用区域中剔除，
  - 当获得可用区域不为空，并且个数小于 zone 区域总数，就随机选一个 zone 区域返回。

2. 将返回选中 zone 区中的 server，过滤掉连接数过多的 server，（选择连接数最小的 server）。

### 1.2.2. RetryRule：

**对选定的负载均衡策略（默认 RoundRobinRule）机上重试机制，在一个配置时间段内当选择 server 不成功**，则一直尝试使用 subRule 策略（所选的策略，默认选RoundRobinRule）的方式一直选 server，直到选到一个可用的 server，或者到达配置阈值（maxRetryMillis）没选到之后返回 null。


默认情况下，Ribbon 采用 ZoneAvoidanceRule 规则。因为大多数公司是单机房，所以一般只有一个 zone，而 ZoneAvoidanceRule 在仅有一个 zone 的情况下，会退化成轮询的选择方式，所以会和 RoundRobinRule 规则类似。


## 1.3 自定义 Ribbon 配置

在自定义 Ribbon 配置的时候，会有全局和客户端两种级别。相比来说，客户端级别是更细粒度的配置。针对每个服务，Spring Cloud Netflix Ribbon 会创建一个 Ribbon 客户端，并且使用服务名作为 Ribbon 客户端的名字。可以通过**配置文件**的方式，实现 Ribbon 自定义配置，。

### 示例 2 ribbon 配置


## 1.4 ribbon 饥饿加载

默认配置下，Ribbon 客户端是在首次请求服务时，才创建该服务的对应的 Ribbon 客户端。
- 好处是项目在启动的时候，能够更加快速，因为 Ribbon 客户端创建时，需要从注册中心获取服务的实例列表，需要有网络请求的消耗。
- 坏处是首次请求服务时，因为需要 Ribbon 客户端的创建，会导致请求比较慢，严重情况下会导致请求超时。

因此，Spring Cloud Netflix Ribbon 提供了 ribbon.eager-load 配置项，允许我们在项目启动时，提前创建 Ribbon 客户端。就是**饥饿加载**。


### 示例 3 ribbon 饥饿加载


### 1.4.1 使用建议

- 在本地开发环境时，可能会频繁重启项目，为了项目启动更快，可以考虑关闭 Ribbon 饥饿加载。（项目启动更快）
- 在生产环境下，一定要开启 Ribbon 饥饿加载。（访问更快）

## 1.5 ribbon 负载均衡请求重试

一般情况下，我们在 HTTP 请求远程服务时，都能够正常返回。但是极端情况下，可能会存在请求失败的情况下，例如说：

- 请求的服务执行逻辑过久，导致超过请求的等待时间
- 请求的服务异常挂掉了，未从注册中心中移除，导致服务消费者还是会调用该服务网络一个抖动，导致请求失败

此时，我们通过重试请求到当前服务实例或者其它服务实例，以获得请求的结果，实现更高的可用性。在 Spring Cloud 中，提供 spring.cloud.loadbalancer.retry 配置项，通过设置为 true，开启负载均衡的重试功能。超时重试调用返回日志如下：

```log
{
    "timestamp": "2020-12-16T08:21:15.885+0000",
    "status": 500,
    "error": "Internal Server Error",
    "message": "I/O error on GET request for \"http://demo-provider/echo\": com.netflix.client.ClientException: Number of retries on next server exceeded max 1 retries, while making a call for: 172.20.249.49:15950; nested exception is java.io.IOException: com.netflix.client.ClientException: Number of retries on next server exceeded max 1 retries, while making a call for: 172.20.249.49:15950",
    "path": "/hello02"
}

```



# 二、feign 声明式服务调用

Feign 是一个声明式的 REST 客户端，它的目的就是让 **REST 调用更加简单**。Feign 提供了 HTTP 请求的模板，通过编写简单的接口和插入注解，就可以定义好 HTTP 请求的参数、格式、地址等信息。相比使用 RestTemplate 实现服务的调用，**Feign 简化了代码的编写，提高了代码的可读性，提升开发的效率**。

## 2.1 示例1 使用 feign 接口实现调用

## 2.2 示例2 自定义 Feign 配置

对 Feign 进行自定义配置。例如说，自定义 Feign 的日志配置，将 Feign 的请求信息打印出来，方便排查问题。在自定义 Feign 配置的时候，会有全局和客户端两种级别。相比来说，客户端级别是更细粒度的配置。针对每个服务，Spring Cloud OpenFeign 会创建一个 Feign 客户端，并且使用服务名作为 Feign 客户端的名字。实现 Feign 自定义配置，可以通过**配置文件**的方式。


在 Feign 中，定义了四种日志级别：

- NONE：不打印日志
- BASIC：只打印基本信息，包括请求方法、请求地址、响应状态码、请求时长
- HEADERS：在 BASIC 基础信息的基础之上，增加请求头、响应头
- FULL：打印完整信息，包括请求和响应的所有信息。


修改 application.yaml 配置文件，额外添加如下配置：

```yml

logging:
  level:
    com/unionpay/feign/feigndemo/consumer/feign: DEBUG  # logger-level 配置项，设置 Feign 的日志级别。

feign:
  # Feign 客户端配置，对应 FeignClientProperties 配置属性类
  client:
    # config 配置项是 Map 类型。key 为 Feign 客户端的名字，value 为 FeignClientConfiguration 对象
    config:
      # 全局级别配置
      default:                          # default 为特殊的 key，用于全局级别的配置。
        logger-level: BASIC
      # 客户端级别配置
      demo-provider:
        logger-level: FULL


```


**其他配置项**

通过 FeignClientConfiguration 配置属性类，可以看到配置文件所支持的 FeignClient 的所有配置项。
```java
// Feign 日志级别。默认为 NONE
private Logger.Level loggerLevel;
// 请求的连接超时时长，单位：毫秒。默认为 10 * 1000 毫秒
private Integer connectTimeout;
// 请求的读取超时时长，单位：毫秒。默认为 60 * 1000 毫秒
private Integer readTimeout;
// 重试策略。默认为不重试
private Class<Retryer> retryer;
// 错误解码器
private Class<ErrorDecoder> errorDecoder;
// 请求拦截器
private List<Class<RequestInterceptor>> requestInterceptors;
// 是否对响应状态码为 404 时，进行解码。默认为 false 
private Boolean decode404;
// 解码器。
// 为空时，默认创建 SpringDecoder Bean
private Class<Decoder> decoder;
// 解码器。默认为 SpringEncoder
// 为空时，默认创建 SpringEncoder Bean
private Class<Encoder> encoder;
// 契约。
// 为空时，默认创建 SpringMvcContract Bean，提供对 SpringMVC 注解的支持
private Class<Contract> contract;

```


## 2.3 feign 请求重试

Feign 和 Ribbon 都有请求重试的功能（可配），两者都启用该功能的话，会产生冲突的问题。因此，有且只能启动一个的重试。目前比较推荐的是使用 Ribbon 来提供重试，来自 Spring Cloud 开发者的说法：https://github.com/spring-cloud/spring-cloud-netflix/issues/467 在 Spring Cloud OpenFeign 中，默认创建的是 NEVER_RETRY 不进行重试。所以，只需要配置 Ribbon 的重试功能即可。


# 三、服务网关 zuul & scg

1. 什么是服务网关？

服务网关 = 路由转发 + 过滤器
- 路由转发：接收一切外界请求，转发到后端的微服务上去；
- 过滤器：在服务网关中可以完成一系列的横切功能，例如权限校验、限流以及监控等，这些都可以通过过滤器完成（其实路由转发也是通过过滤器实现的）。

2. 为什么需要服务网关？

横切功能（以权限校验为例）的实现，可以写在三个位置：

- 每个服务自己实现一遍
- 写到一个公共的服务中，然后其他所有服务都依赖这个服务
- 写到服务网关的前置过滤器中，所有请求过来进行权限校验


第一种，缺点太明显，生产基本不用；第二种，相较于第一点好很多，代码开发不会冗余，但是有两个缺点：


- 由于每个服务引入了这个公共服务，那么相当于在每个服务中都引入了相同的权限校验的代码，使得每个服务的jar包大小无故增加了一些，尤其是对于使用docker 镜像进行部署的场景，jar越小越好；
- 由于每个服务都引入了这个公共服务，那么我们后续升级这个服务可能就比较困难，而且公共服务的功能越多，升级就越难，而且假设我们改变了公共服务中的权限校验的方式，想让所有的服务都去使用新的权限校验方式，我们就需要将之前所有的服务都重新引包，编译部署。


而服务网关恰好可以解决这样的问题：


- 将权限校验的逻辑写在网关的过滤器中，后端服务不需要关注权限校验的代码，所以服务的jar包中也不会引入权限校验的逻辑，不会增加jar包大小；
- 如果想修改权限校验的逻辑，只需要修改网关中的权限校验过滤器即可，而不需要升级所有已存在的微服务。所以，需要服务网关。


## 3.1 zuul 网关

Zuul 是由 Netflix 开源的微服务网关，提供都动态路由、监控、熔断、安全等等功能。Zuul 有 1.X 和 2.X 两个版本，前者基于同步阻塞模式的编程模型实现，后者基于异步非阻塞模式的编程模型实现。目前，Spring Cloud Netflix Zuul 采用的是 Zuul 1.X 版本。并且，Zuul 2.X 没有被集成到 Spring Cloud 中，参考：https://github.com/spring-cloud/spring-cloud-gateway/issues/86


### 3.1.1 示例1 zuulDemo

创建 application.yaml 配置文件，添加 Spring Cloud Zuul 相关配置。配置如下：

```yml
server:
  port: 8888    # 访问 127.0.0.1:8888

spring:
  application:
    name: zuul-application

# Zuul 配置项，对应 ZuulProperties 配置类
zuul:
  servlet-path: / # ZuulServlet 匹配的路径，默认为 /zuul
                  # Zuul 和 SpringMVC 一样，都是通过实现自定义的 Servlet，从而进行请求的转发。

  # 路由配置项，对应 ZuulRoute 数组
  routes:
    route_archives:
      path: /archives/**
      url: https://blog.yarwen.com
    route_oschina:
      path: /oschina/**
      url: https://www.oschina.net

```


我们在配置文件中，通过 path + url 的组合，添加了两个路由信息，这种我们一般称之为“静态路由”或者“传统路由”。


### 3.1.2 基于注册中心实现 动态路由

通过 path + service-id 的组合，添加路由信息，这种我们一般称之为“动态路由”。如此，每个路由转发的 URL 地址，将从 Spring Cloud 注册中心获取对应 service-id 服务名对应的服务实例列表，并通过 Ribbon 等等进行负载均衡。

#### 示例 2 Zuul 基于注册中心实现动态路由


Spring Cloud Zuul 集成 Spring Cloud 注册中心时，会给注册中心的每个服务在 Zuul 中自动创建对应的“动态路由”。对应配置文件的效果如下：
```yml

zuul:
  routes:
    user-service:
      path: /user-service/**
      service-id: user-service

```


访问 http://127.0.0.1:8888/user-service/user/get?id=1 地址，返回 JSON 结果如下：


```json
{
    "id": 1,
    "name": "没有昵称：1",
    "gender": 2
}
```

上述，在虽然通过修改配置文件，实现 Zuul “动态路由”。但是，如果每次进行路由的变更时，都需要修改配置文件，并重启 Zuul 实例，显然是不合适的。往往生产上是使用配置中心实现动态路由。（例如，采用nacos、apollo 作为配置中心）


### 3.1.3 Zuul 过滤器

Zuul 的两大核心是路由和过滤功能：

- 路由功能：负责匹配请求到指定 Route 路由，从而转发请求到该路由对应的 url 后端 URL 或 service-id 服务实例上。
- 过滤功能：负责对请求进行拦截，实现自定义的功能，例如说限流、熔断等等功能。


Zuul 定义了 IZuulFilter 接口，定义了 Zuul 过滤器的两个基础方法。代码如下：

```java
public interface IZuulFilter {

    /**
     * 是否进行过滤。如果是，则返回 true。
     */
    boolean shouldFilter();

    /**
     * 执行过滤器的逻辑。
     */
    Object run() throws ZuulException;

}

```

Zuul 定义了 ZuulFilter 抽象基类，定义了 Zuul 过滤器的类型和执行顺序方法。代码如下：

```java

public abstract class ZuulFilter implements IZuulFilter, Comparable<ZuulFilter> {

    /**
     * 执行类型。
     */
    abstract public String filterType();

    /**
     * 执行顺序。
     */
    abstract public int filterOrder();

}

```

过滤器一共分成四种类型，分别在不同阶段执行：

```java

// FilterConstants.java 类

/**
 * {@link ZuulFilter#filterType()} pre type.
 * 前置类型：在 route 之前调用
 * 使用场景：身份认证、记录请求日志、服务容错、服务限流
 */
public static final String PRE_TYPE = "pre";

/**
 * {@link ZuulFilter#filterType()} route type.
 * 路由类型：路由请求到后端 URL 或服务实例上
 * 使用场景：使用 Apache HttpClient 请求后端 URL、或使用 Netflix Ribbon 请求服务实例。
 */
public static final String ROUTE_TYPE = "route";

/**
 * {@link ZuulFilter#filterType()} post type.
 * 后置类型：
 *     1. 发生异常时，在 error 之后调用
 *     2. 正常执行时，最后调用
 * 使用场景：收集监控信息、响应结果给客户端。
 */
public static final String POST_TYPE = "post";

/**
 * {@link ZuulFilter#filterType()} error type.
 * 错误类型：处理请求时发生的错误时被调用。
 */
public static final String ERROR_TYPE = "error";


```

**自定义过滤器 demo**

过滤器是 Zuul 中的核心内容，很多高级的扩展都需要自定义过滤器来实现，在 Zuul 中自定义一个过滤器只需要继承 ZuulFilter，然后重写 ZuulFilter 的四个方法即可。

```java
@Component
public class myFilter extends ZuulFilter {  // 继承 ZuulFilter

    public boolean shouldFilter() {
        return true; // 需要过滤
    }

    public String filterType() {
        return "pre"; // 前置过滤器
    }

    public int filterOrder() {
        return 0;
    }

    public Object run() throws ZuulException {
        return null;
    }
}


```
1. 重写 shouldFilte 方法，shouldFilter 方法决定了是否执行该过滤器，true 为执行，false 为不执行，这个也可以利用配置中心来做，达到动态的开启或关闭过滤器。
2. filterType 方法是要返回过滤器的类型，可选值有 pre、route、post、error 四种类型。
3. 过滤器有多个，多个过滤器执行肯定有先后顺序，那么我们可以通过 filterOrder 来指定过滤器的执行顺序，数字越小，优先级越高。
4. 最重要的就是 run 方法了，所有的业务逻辑都写在 run 方法中。定义完后，只需要将过滤器交由 Spring 管理即可生效。在第一个过滤器中如果需要传递一些数据给后面的过滤器，我们可以获取 RequestContext，然后调用 set 方法进行值的设置，在后面的过滤器中还是通过 RequestContext 的 get 方法获取对应的值。


还有一个常见的需求就是进行请求的拦截，比如我们在网关中对请求的 Token 进行合法性的验证，如果不合法，通过 RequestContext 的 setSendZuulResponse 告诉 Zuul 不需要将当前请求转发到后端的服务，然后通过 setResponseBody 返回固定的数据给客户端。


### 示例3 接入认证服务。


**补充说明**： 在 Spring Cloud Zuul 中，我们可以通过在配置文件中，添加 zuul.<过滤器名>.<过滤器类型>.disable=true 配置项来禁用指定过滤器。例如，想要禁用 SendResponseFilter 后置过滤器，则可以添加 zuul.SendResponseFilter.post.disable=true 配置项来禁用。


## 3.2 scg 网关

### 3.2.1 scg 网关路由

```yml
server:
  port: 8888

spring:
  cloud:
    # Spring Cloud Gateway 配置项，对应 GatewayProperties 类
    gateway:
      # 路由配置项，对应 RouteDefinition 数组
      routes:
        - id: rrmai01 # 路由的编号
          uri: https://blog.yarwen.com # 路由到的目标地址
          predicates: # 断言，作为路由的匹配条件，对应 RouteDefinition 数组
            - Path=/archives
          filters:
            - StripPrefix=1
        - id: rrmai02 # 路由的编号
          uri: https://www.oschina.net # 路由的目标地址
          predicates: # 断言，作为路由的匹配条件，对应 RouteDefinition 数组
            - Path=/oschina
          filters: # 过滤器，对请求进行拦截，实现自定义的功能，对应 FilterDefinition 数组
            - StripPrefix=1

```

**说明**


1. ID：编号，路由的唯一标识。
2. URI：路由指向的目标 URI，即请求最终被转发的目的地。
    - 例如，这里配置的 https://blog.yarwen.com 或 https://www.oschina.net，就是被转发的地址。
3. Predicate：谓语，作为路由的匹配条件。Gateway 内置了多种 Predicate 的实现，提供了多种请求的匹配条件，比如说基于请求的 Path、Method 等等。
    - 例如，这里配置的 Path 匹配请求的 Path 地址。
4. Filter：过滤器，对请求进行拦截，实现自定义的功能。网关内置了多种 Filter 的实现，提供了多种请求的处理逻辑，比如说限流、熔断等等。


### 3.2.2 基于注册中心、配置中心实现路由 和 zuul 相似使用（略）

### 3.2.3 scg 灰度发布

    简言之，新版本在线上运行稳定后，逐步将所有流量都转发到新版本服务上。

#### 示例 1 灰度发布

（1） 执行 GatewayApplication 启动网关。
（2） 使用浏览器，访问 http://127.0.0.1:8888 地址，（浏览器访问查看返回）
    90% 的情况下返回 http://www.yarwen.cn，
    10% 的情况下返回 https://www.oschina.net，符合预期~


### 3.2.4 scg filter 和 zuul 网关区别不大，不再介绍。


### 3.2.5 scg 支持请求限流

scg 内置 RequestRateLimiterGatewayFilterFactory（工厂类） 提供请求限流的功能。该 Filter 是基于 Token Bucket Algorithm（令牌桶算法）实现的限流，同时可以搭配上 Redis 实现分布式限流。（**令牌桶算法的原理**是系统会以一个恒定的速度往桶里放入令牌，而如果请求需要被处理，则需要先从桶里获取一个令牌，当桶里没有令牌可取时，则拒绝服务。）


1. 针对编号为 yudaoyuanma 的路由，我们在 filter 配置项，添加了限流过滤器 RequestRateLimiter，其配置参数如下：
    - redis-rate-limiter.replenishRate：令牌桶的每秒放的数量。
    - redis-rate-limiter.burstCapacity：令牌桶的最大令牌数。
    - burstCapacity 参数，我们可以近似理解为是每秒最大的请求数。因此每请求一次，都会从桶里获取掉一块令牌。
    - replenishRate 参数，我们可以近似理解为是每秒平均的请求数。假设在令牌桶为空的情况下，一秒最多放这么多令牌，所以最大请求书当然也是这么多。
    - key-resolver：获取限流 KEY 的 Bean 的名字。

2. spring.redis 配置项，设置使用的 Redis 的配置。


## 3.3 zuul 和 scg 比较

1. 性能（4c8g qps）

- 300 并发时： Spring Cloud Gateway > Zuul （8725.78 > 8420.10） 
              Spring Cloud Gateway 略微优于 Zuul
- 1000 并发时：Spring Cloud Gateway > Zuul （8981.58 > 8319.87）
              Spring Cloud Gateway 略微优于 Zuul
- 3000 并发时：Spring Cloud Gateway > Zuul （8760.64 > 8420.10）
              Spring Cloud Gateway 略微优于 Zuul
- 5000 并发时：Zuul > Spring Cloud Gateway （8425.73 > 7862.68）
              Zuul 优于 Spring Cloud Gateway

2. scg 提供了 灰度发布、请求限流的支持。



# 四、断路器 resilience4j

Resilience4j 是一个轻量级的容错组件，其灵感来自于 Hystrix，但主要为 Java 8 和函数式编程所设计。轻量级体现在其只用 Vavr 库，没有任何外部依赖。而 Hystrix 依赖了 Archaius，Archaius 本身又依赖很多第三方包，例如 Guava、Apache Commons Configuration 等等。


## 4.1 CircuitBreaker 熔断器

CircuitBreaker 一共有 CLOSED、OPEN、HALF_OPEN 三种状态，通过状态机实现。转换关系如下图所示：


**图片**


- 当熔断器关闭(CLOSED)时，所有的请求都会通过熔断器。
- 如果失败率超过设定的阈值，熔断器就会从关闭状态转换到打开(OPEN)状态，这时所有的请求都会被拒绝。
- 当经过一段时间后，熔断器会从打开状态转换到半开(HALF_OPEN)状态，这时仅有一定数量的请求会被放入，并重新计算失败率。如果失败率超过阈值，则变为打开(OPEN)状态；如果失败率低于阈值，则变为关闭(CLOSE)状态。



### 4.1.1  CircuitBreaker 实现


Resilience4j 记录请求状态的数据结构和 Hystrix 不同：Hystrix 是使用滑动窗口来进行存储的，而 Resilience4j 采用的是 Ring Bit Buffer(环形缓冲区)。Ring Bit Buffer 在内部使用 BitSet 这样的数据结构来进行存储，结构如下图所示：


**图片**


- 每一次请求的成功或失败状态只占用一个 bit 位，与 boolean 数组相比更节省内存。BitSet 使用 long[] 数组来存储这些数据，意味着 16 个值(64 bit)的数组可以存储 1024 个调用状态。
- **计算失败率需要填满环形缓冲区。**例如，如果环形缓冲区的大小为 10，则必须至少请求满 10 次，才会进行故障率的计算。
- 当故障率高于设定的阈值时，熔断器状态会从由 CLOSE 变为 OPEN。这时所有的请求都会抛出 CallNotPermittedException 异常。
- 当经过一段时间后，熔断器的状态会从 OPEN 变为 HALF_OPEN。HALF_OPEN 状态下同样会有一个 Ring Bit Buffer，用来计算HALF_OPEN 状态下的故障率。如果高于配置的阈值，会转换为 OPEN，低于阈值则装换为 CLOSE。与 CLOSE 状态下的缓冲区不同的地方在于，HALF_OPEN 状态下的缓冲区大小会限制请求数，只有缓冲区大小的请求数会被放入。


除此以外，熔断器还会有两种特殊状态：DISABLED（始终允许访问）和 FORCED_OPEN（始终拒绝访问）。这两个状态不会生成熔断器事件（除状态装换外），并且不会记录事件的成功或者失败。退出这两个状态的唯一方法是触发状态转换或者重置熔断器。


**搭建一个 Feign 的快速入门示例。步骤如下：**

- 首先，使用 SpringMVC 搭建一个用户服务，提供 JSON 数据格式的 HTTP API。
- 然后，搭建一个使用 Feign 声明式调用用户服务 HTTP API 的示例项目。


**搭建一个 Resilience4j CircuitBreaker 的快速入门示例。步骤如下：**


- 首先，搭建一个 user-service 用户服务，提供获取用户信息的 HTTP API 接口。
- 然后，搭建一个用户服务的消费者，使用 Resilience4j 实现服务容错。


**简单测试：**


- 执行 UserServiceApplication 启动用户服务，执行 DemoApplication 启动 Resilience4j 示例项目。
- 使用浏览器，访问 http://127.0.0.1:8080/demo/get_user?id=1 地址，成功调用用户服务，返回结果为 User:1。
- 停止 UserServiceApplication 关闭用户服务。
- 使用浏览器，访问 http://127.0.0.1:8080/demo/get_user?id=1 地址，失败调用用户服务，返回结果为 mock:User:1。


观察日志，可以判断触发了 Resilience4j 的 fallback 服务降级的方法。


-  疯狂使用浏览器，访问 http://127.0.0.1:8080/demo/get_user?id=1 地址，会触发熔断器熔断（打开），不再执行 #getUser(Integer id) 方法，而是直接 fallback 触发 #getUserFallback(Integer id, Throwable throwable) 方法。（观察日志）
- 重新执行 UserServiceApplication 启动用户服务。使用浏览器，多次访问 http://127.0.0.1:8080/demo/get_user?id=1 地址，熔断器的状态逐步从打开 => 半开 => 关闭。（观察日志）
```log

// （1）熔断器打开状态
2020-12-16 14:36:30.369  INFO 8948 --- [io-8080-exec-10] r.controller.DemoController              : [getUserFallback][id(1) exception(ResourceAccessException)]

// （2）熔断器半开状态
2020-12-16 14:39:33.892  INFO 8948 --- [nio-8080-exec-4] r.controller.DemoController              : [getUser][准备调用 user-service 获取用户(1)详情]

// （3）熔断器关闭状态
2020-12-16 14:39:37.257  INFO 8948 --- [nio-8080-exec-7] r.controller.DemoController              : [getUser][准备调用 user-service 获取用户(1)详情]
2020-12-16 14:39:38.091  INFO 8948 --- [nio-8080-exec-8] r.controller.DemoController              : [getUser][准备调用 user-service 获取用户(1)详情]
2020-12-16 14:39:39.236  INFO 8948 --- [io-8080-exec-10] r.controller.DemoController              : [getUser][准备调用 user-service 获取用户(1)详情]



```

## 4.2 RateLimiter 限流器

默认情况下，采用 AtomicRateLimiter 基于令牌桶限流算法实现限流。令牌桶算法的原理是系统会以一个恒定的速度往桶里放入令牌，而如果请求需要被处理，则需要先从桶里获取一个令牌，当桶里没有令牌可取时，则拒绝服务。


**示例简单测试：**


- 执行 DemoApplication 启动 Resilience4j 示例项目。
- 使用浏览器，访问 http://127.0.0.1:8080/rate-limiter-demo/get_user?id=1 地址，成功返回结果为 User:1。
- 立马使用浏览器再次访问，会阻塞等待 < 5 秒左右，降级返回 mock:User:1。同时，我们在 IDEA 控制台的日志中，可以看到被限流时抛出的 RequestNotPermitted 异常。

另外，我们将 @RateLimiter 和 @CircuitBreaker 注解添加在相同方法上，进行组合使用，来实现限流和断路的作用。但是要注意，需要添加 resilience4j.circuitbreaker.instances.<instance_name>.ignoreExceptions=io.github.resilience4j.ratelimiter.RequestNotPermitted 配置项，忽略限流抛出的 RequestNotPermitted 异常，避免触发断路器的熔断。


# 五、分布式跟踪链 sleuth


## 5.1 分布式链路跟踪使用场景

单体架构时，一个请求的调用链路非常清晰，一般由负载均衡器，比如 Nginx。将调用方的请求转发到后端服务，后端服务进行业务处理后返回给调用方。而当架构变成微服务架构时，可能带来一系列的问题。

1. **接口响应慢，怎么排查？**一个请求往往需要调用多个服务，而当接口响应比较慢时，我们无法知道是哪个服务出现了问题，在什么地方比较耗时，只有通过链路跟踪，将整个请求的链路信息收集起来，才可以知道请求在哪个环节耗时较长，从而快速发现接口响应慢的问题，并及时解决。
2. **服务间的依赖关系如何查看？**服务之间都存在相互调用的情况，如果不做梳理工作，随着时间的推移，整个调用关系将会变成一张蜘蛛网。梳理调用关系可以在前期将服务之间的关系整理清楚，当需要对一个服务做改动时，可以明确的知道影响的范围。链路跟踪会将请求的链路信息记录起来，通过这些信息可以分析出服务之间的依赖关系，并且可以绘制出可视化的依赖关系图。
3. **请求贯穿多个服务，如何将日志串起来？**记录详细的日志能够方便排查问题，在微服务架构下，一个请求经过了 N 个服务，输出 N 条日志，我们需要将日志统一收集起来，而存在的问题是日志是在各个服务节点中输出的，当服务器时间不一致时，无法获得正确的日志顺序的，最严重的是不知道这些日志间的关系，不知道某个请求对应的日志有哪些。链路跟踪会产生 tradeId，tradeId 会贯穿整个请求，将所有日志串联起来。


## 5.2 链路跟踪核心概念

1. Span 基本工作单元，例如，发送 RPC 请求是一个新的 Span，发送 HTTP 请求是一个新的 Span，内部方法调用也是一个新的 Span。
2. Trace 一次分布式调用的链路信息，每次调用链路信息都会在请求入口处生成一个 TraceId。
3. Annotation 用于记录事件的信息。在 Annotation 中会有 CS、SR、SS、CR 这些信息。
4. CS 也就是 Client Sent，客户端发起一个请求，这个 Annotation 表示 Span 的开始。
5. SR 也就是 Server Received，服务器端获得请求并开始处理它，用 SR 的时间戳减去 CS 的时间戳会显示网络延迟时间。
6. SS 也就是 Server Sent，在请求处理完成时将响应发送回客户端，用 SS 的间戳减去 SR 的时间戳会显示服务器端处理请求所需的时间。
7. CR 也就是 Client Received，表示 Span 的结束，客户端已成功从服务器端收到响应，用 CR 的时间戳减去 CS 的时间戳就可以知道客户端从服务器接收响应所需的全部时间。



## 5.3 Spring Cloud Sleuth

Spring Cloud Sleuth 是一种分布式的服务链路跟踪解决方案，通过使用 Spring Cloud Sleuth 可以让我们快速定位某个服务的问题，以及厘清服务间的依赖关系。

- Sleuth 可以添加链路信息到日志中，这样的好处是可以统一将日志进行收集展示，并且可以根据链路的信息将日志进行串联。
- Sleuth 中的链路数据可直接上报给 Zipkin，在 Zipkin 中就可以直接查看调用关系和每个服务的耗时情况。
- Sleuth 中内置了很多框架的埋点，比如：Zuul、Feign、Hystrix、RestTemplate 等。正因为有了这些框架的埋点，链路信息才能一直往下传递。
- sleuth 可以结合 zipkin，将信息发送到zipkin，利用zipkin的存储来存储信息，利用zipkin ui来展示数据。


集成 Spring Cloud Sleuth 后，会在原始的日志中加上一些链路的信息，总共有四个字段，分别是 application name、traceId、spanId、export。

- application name：应用的名称，也就是 application.properties 中的spring.application.name 参数配置的属性。
- traceId：为请求分配唯一请求号，用来标识一条请求链路。
- spanId：表示基本的工作单元，一个请求可以包含多个步骤，每个步骤都拥有自己的 spanId。一个请求包含一个 TraceId 和多个 SpanId。
- export：布尔类型。表示是否要将该信息输出到 Zipkin 进行收集和展示。


### 示例1 SpringMVC 接口链路追踪


建一个 Spring Cloud Sleuth 对 SpringMVC 的 API 接口的链路追踪。简单测试：

- 执行 SpringMVCApplication，启动该 Spring Cloud 应用。
- 首先，使用 curl http://127.0.0.1:8080/user/get?id=1 命令，请求下 Spring Cloud 应用提供的 API。因为，我们要追踪下该链路。
- 然后，继续使用浏览器，打开 http://127.0.0.1:9411/ 地址，查看链路数据。点击「查找」按钮，便可看到刚才我们调用接口的链路数据。


### 示例2 springmvc-feign


搭建一个 Spring Cloud Sleuth 对 Feign 的远程 HTTP 调用的链路追踪。简单测试:

- 使用 FeignApplication 和 UserServiceApplication 启动两个 Spring Cloud 应用。
- 首先，使用 curl http://127.0.0.1:8081/feign/get?id=1 命令，使用 Feign 调用 user-service 服务。因为，我们要追踪下该链路。
- 然后，继续使用浏览器，打开 http://127.0.0.1:9411/ 地址，查看链路数据。点击「查找」按钮，便可看到刚才我们调用接口的链路数据。


### 示例3 Spring Cloud Gateway 示例

搭建一个 Spring Cloud Sleuth 对 Spring Cloud Gateway 的代理请求的链路追踪。简单测试：

- 使用 FeignApplication、UserServiceApplication、GatewayApplication 启动三个 Spring Cloud 应用。
- 首先，使用 curl http://127.0.0.1:8888/feign/get?id=1 命令，请求 API 网关，从而转发请求到 feign-service 服务。（因为，要追踪该链路。）
- 然后，继续使用浏览器，打开 http://127.0.0.1:9411/ 地址，查看链路数据。点击「查找」按钮，便可看到刚才我们调用接口的链路数据。


一条链路经过 gateway-application feign-service、user-service 三个服务，一共有五个 Span。



### 示例4 dubbo 调用跟踪示例

搭建一个 Spring Cloud Sleuth 对 Dubbo 的远程 RPC 调用的链路追踪。简单测试：

- 使用 ProviderApplication 启动服务提供者，使用 ConsumerApplication 启动服务消费者。
- 首先，使用 curl http://127.0.0.1:8080/user/get?id=1 命令，使用 Dubbo 调用 user-service 服务。因为，我们要追踪下该链路。
- 然后，继续使用浏览器，打开 http://127.0.0.1:9411/ 地址，查看链路数据。点击「查找」按钮，便可看到刚才我们调用接口的链路数据。



