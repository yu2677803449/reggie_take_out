package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrdersMapper;
import com.itheima.reggie.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private UserService userService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Transactional
    @Override
    public void submit(Orders orders) {
        //获取用户id信息
        User user = userService.getById(BaseContext.getThreadLocal());
        //根据id获取购物车信息
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = Wrappers.lambdaQuery(ShoppingCart.class)
                .eq(ShoppingCart::getUserId,user.getId());
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(shoppingCartLambdaQueryWrapper);
        //根据地址id获取地址信息
        LambdaQueryWrapper<AddressBook> addressBookLambdaQueryWrapper =
                Wrappers.lambdaQuery(AddressBook.class).eq(AddressBook::getId, orders.getAddressBookId());
        AddressBook addressBook = addressBookService.getOne(addressBookLambdaQueryWrapper);
        //组装详细订单数据
        Long id = IdWorker.getId();
        BigDecimal totalAmount = new BigDecimal("0");
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setNumber(cart.getNumber());
            orderDetail.setName(cart.getName());
            orderDetail.setImage(cart.getImage());
            orderDetail.setDishId(cart.getDishId());
            orderDetail.setSetmealId(cart.getSetmealId());
            orderDetail.setDishFlavor(cart.getDishFlavor());
            orderDetail.setAmount(cart.getAmount());
            orderDetail.setOrderId(id);
            totalAmount = totalAmount.add(cart.getAmount().multiply(new BigDecimal(cart.getNumber())));
            orderDetailList.add(orderDetail);
        }
        orderDetailService.saveBatch(orderDetailList);
        //设置属性
        orders.setId(id);//订单表主键ID
        orders.setNumber(String.valueOf(id));//订单号
        orders.setUserId(BaseContext.getThreadLocal());//当前用户ID
        orders.setAddress(addressBook.getDetail());//收件地址
        orders.setConsignee(addressBook.getConsignee());//收件人
        orders.setPhone(addressBook.getPhone());//收件人手机号
        orders.setStatus(2);//待派送
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setUserName(user.getName());
        orders.setAmount(totalAmount);//总金额
        this.save(orders);
        shoppingCartService.remove(shoppingCartLambdaQueryWrapper);
    }
}
