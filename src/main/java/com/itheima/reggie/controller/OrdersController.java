package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrdersService;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/order")
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    //GET
    //	http://localhost:8080/order/userPage?page=1&pageSize=1
    @GetMapping("/userPage")
    public R<Page<OrdersDto>> list(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "1") Integer pageSize) {
        //获取用户的id信息
        Long userId = BaseContext.getThreadLocal();
        //根据id查询订单基本信息
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = Wrappers.lambdaQuery(Orders.class)
                .eq(userId != null, Orders::getUserId, userId)
                .orderByDesc(Orders::getCheckoutTime);
        ordersService.page(pageInfo, ordersLambdaQueryWrapper);
        //根据id查询订单详细信息
        Page<OrdersDto> dtoPage = new Page<>();
        //获取ordersid
        dtoPage.setTotal(pageInfo.getTotal());
        //封装records
        List<Orders> records = pageInfo.getRecords();
        List<OrdersDto> ordersDtos = records.stream().map(orders -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(orders, ordersDto);
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = Wrappers.lambdaQuery(OrderDetail.class)
                    .eq(orders.getId() != null, OrderDetail::getOrderId, orders.getId());
            List<OrderDetail> orderDetails = orderDetailService.list(orderDetailLambdaQueryWrapper);
            ordersDto.setOrderDetails(orderDetails);
            return ordersDto;
        }).collect(Collectors.toList());
        dtoPage.setRecords(ordersDtos);
        //组装数据成dto数据
        return R.success(dtoPage);
    }

    //POST
    //	http://localhost:8080/order/submit
    @PostMapping("/submit")
    public R order(@RequestBody Orders orders) {
        log.info("订单数据：{}", orders);
        ordersService.submit(orders);
        return R.success(null);
    }

    //GET       订单分页查询
    //	http://localhost:8080/order/page?page=1&pageSize=10
    @GetMapping("/page")
    public R<Page<OrdersDto>> page(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "0") Integer pageSize,
            Long number,
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beginTime,
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = Wrappers.lambdaQuery(Orders.class);
        ordersLambdaQueryWrapper.like(number != null, Orders::getNumber, number)
                .gt(beginTime != null, Orders::getOrderTime, beginTime)
                        .lt(endTime!=null, Orders::getOrderTime, endTime);
        ordersService.page(pageInfo,ordersLambdaQueryWrapper);
        Page<OrdersDto> dtoPage = new Page<>();
        //封装total
        dtoPage.setTotal(pageInfo.getTotal());
        List<Orders> records = pageInfo.getRecords();
        List<OrdersDto> dtoList = records.stream().map(orders -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(orders, ordersDto);
            //根据id查询订单明细表
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = Wrappers.lambdaQuery(OrderDetail.class)
                    .eq(orders.getId() != null, OrderDetail::getOrderId, orders.getId());
            List<OrderDetail> orderDetailList = orderDetailService.list(orderDetailLambdaQueryWrapper);
            if (orderDetailList != null) {
                ordersDto.setOrderDetails(orderDetailList);
            }
            return ordersDto;
        }).collect(Collectors.toList());
        //封装records
        dtoPage.setRecords(dtoList);
        return R.success(dtoPage);
    }

    //PUT   修改状态
    //	http://localhost:8080/order
    @PutMapping
    public R updateStatus(@RequestBody Orders orders){
        LambdaUpdateWrapper<Orders> ordersLambdaUpdateWrapper = Wrappers.lambdaUpdate(Orders.class)
                .set(orders.getStatus() != null, Orders::getStatus, orders.getStatus())
                .eq(orders.getId() != null, Orders::getId, orders.getId());
        ordersService.update(ordersLambdaUpdateWrapper);
        return R.success(null);
    }

    //POST      再来一单
    //	http://localhost:8080/order/again
    @PostMapping("/again")
    public R again(@RequestBody Orders orders){//传递进来的是订单号
        //判断订单是否完成
        Orders one = ordersService.getById(orders);
        if (one!=null && one.getStatus() == 4) {
            //根据订单号查询订单明细
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = Wrappers.lambdaQuery(OrderDetail.class)
                    .eq(one.getId() != null, OrderDetail::getOrderId, one.getId());
            List<OrderDetail> orderDetails = orderDetailService.list(orderDetailLambdaQueryWrapper);
            //把查询出的数据加到shoppingcart表
            orderDetails.forEach(orderDetail -> {
                ShoppingCart cart = new ShoppingCart();
                cart.setId(IdWorker.getId());
                cart.setName(orderDetail.getName());
                cart.setImage(orderDetail.getImage());
                cart.setUserId(BaseContext.getThreadLocal());
                cart.setDishFlavor(orderDetail.getDishFlavor());
                cart.setNumber(orderDetail.getNumber());
                cart.setAmount(orderDetail.getAmount());
                cart.setCreateTime(LocalDateTime.now());
                if(orderDetail.getDishId() == null){
                    cart.setSetmealId(orderDetail.getSetmealId());
                }else {
                    cart.setDishId(orderDetail.getDishId());
                }
                shoppingCartService.save(cart);
            });
            return R.success(null);
        }
        return R.error("订单未完成");
    }

}
