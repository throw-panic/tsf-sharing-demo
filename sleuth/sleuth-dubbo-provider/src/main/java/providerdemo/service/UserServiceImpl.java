package providerdemo.service;


import com.unionpay.dubboApi.api.UserService;

/**
 * @author MyPC
 */
@org.apache.dubbo.config.annotation.Service(protocol = "dubbo", version = "1.0.0")
public class UserServiceImpl implements UserService {

    @Override
    public String get(Integer id) {
        return "user:" + id;
    }

}
