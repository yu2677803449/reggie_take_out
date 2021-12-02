package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    //GET       查询购物车
    //	http://localhost:8080/shoppingCart/list
    @GetMapping("/list")
    public R list() {
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = Wrappers.lambdaQuery(ShoppingCart.class)
                .eq(ShoppingCart::getUserId, BaseContext.getThreadLocal())
                .orderByDesc(ShoppingCart::getCreateTime);
        List<ShoppingCart> list = shoppingCartService.list(shoppingCartLambdaQueryWrapper);
        return R.success(list);
    }

    //POST      添加购物车
    //	http://localhost:8080/shoppingCart/add
    @Transactional
    @PostMapping("/add")
    public R add(@RequestBody ShoppingCart shoppingCart) {
        //封装当前操作用户的id
        shoppingCart.setUserId(BaseContext.getThreadLocal());
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = Wrappers.lambdaQuery(ShoppingCart.class);
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, shoppingCart.getUserId());
        if (shoppingCart.getDishId() != null) {
            //点的是菜品
            shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        } else {
            shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }
        //查询数据库
        ShoppingCart shoppingCartDB = shoppingCartService.getOne(shoppingCartLambdaQueryWrapper);
        if (shoppingCartDB == null) {
            //封装对象
            shoppingCart.setId(IdWorker.getId());
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            shoppingCartDB = shoppingCart;
        } else {
            shoppingCartDB.setNumber(shoppingCartDB.getNumber() + 1);
            shoppingCartService.updateById(shoppingCartDB);
        }
        return R.success(shoppingCartDB);
    }

    //DELETE    清空购物车
    //	http://localhost:8080/shoppingCart/clean
    @DeleteMapping("/clean")
    public R removeByUserId() {
        //获取userid
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = Wrappers.lambdaQuery(ShoppingCart.class)
                .eq(ShoppingCart::getUserId, BaseContext.getThreadLocal());
        shoppingCartService.remove(shoppingCartLambdaQueryWrapper);
        return R.success("删除成功");
    }

    //Post
    //http://localhost:8080/shoppingCart/sub
    @PostMapping("/sub")
    public R sub(@RequestBody ShoppingCart shoppingCart) {
        //获取当前用户id
        Long userId = BaseContext.getThreadLocal();
        //根据id查询购物车物品数量，如果数量大于1执行修改
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = Wrappers.lambdaQuery(ShoppingCart.class)
                .eq(userId != null, ShoppingCart::getUserId, userId)
                .eq(shoppingCart.getDishId() != null, ShoppingCart::getDishId, shoppingCart.getDishId())
                .eq(shoppingCart.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        ShoppingCart cart = shoppingCartService.getOne(shoppingCartLambdaQueryWrapper);
        if (cart.getNumber() > 1) {
            cart.setNumber(cart.getNumber()-1);
            shoppingCartService.updateById(cart);
            cart = shoppingCartService.getOne(shoppingCartLambdaQueryWrapper);
            return R.success(cart);
        }
        //，如果数量等于1，执行删除操作
        if(cart.getNumber() == 1){
            shoppingCartService.removeById(cart.getId());
            cart.setNumber(0);
            return R.success(cart);
        }
        return R.error("失败");
    }

}
