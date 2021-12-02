package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    //POST
    //	http://localhost:8080/user/sendMsg
    @PostMapping("/sendMsg")
    public R sendMsg(@RequestBody User user, HttpServletRequest request){
        //获取手机号
        String phone = user.getPhone();
        //判断手机号是否为空
        if(StringUtils.isNotEmpty(phone)){
            //生成验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("验证码为：{}",code);
            //设置验证码到session
            request.getSession().setAttribute("code",code);
            return R.success(null);
        }
        return R.error("验证码发送失败");
    }

    //POST
    //	http://localhost:8080/user/login
    @PostMapping("/login")
    public R login(@RequestBody Map map, HttpSession session){
        log.info(map.toString());
        String phone = map.get("phone").toString();
        String code = map.get("code").toString();
        //获取session中的code
        String msgCode = session.getAttribute("code").toString();
        if(msgCode!=null && msgCode.equals(code)){
            LambdaQueryWrapper<User> userLambdaQueryWrapper = Wrappers.lambdaQuery(User.class);
            userLambdaQueryWrapper.eq(User::getPhone,phone);
            User user = userService.getOne(userLambdaQueryWrapper);
            if(user==null){
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            session.setAttribute("user",user.getId());
            return R.success(user);
        }
        return R.error("登录失败");
    }

    //POST      退出登录
    //	http://localhost:8080/user/loginout
    @PostMapping("/loginout")
    public R logout(HttpSession Session){
        Session.invalidate();
        return R.success(null);
    }


}
