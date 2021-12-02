package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/addressBook")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    //GET       根据用户id查询地址信息
    //	http://localhost:8080/addressBook/list
    @GetMapping("/list")
    public R<List<AddressBook>> list(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getThreadLocal());
        log.info("addressBook:{}", addressBook);
        //获取用户id,组装查询条件
        LambdaQueryWrapper<AddressBook> addressBookLambdaQueryWrapper = Wrappers.lambdaQuery(AddressBook.class)
                .eq(addressBook.getUserId() != null, AddressBook::getUserId, addressBook.getUserId())
                .orderByDesc(AddressBook::getUpdateTime);
        List<AddressBook> list = addressBookService.list(addressBookLambdaQueryWrapper);
        return R.success(list);
    }

    //POST
    //	http://localhost:8080/addressBook
    @PostMapping
    public R save(@RequestBody AddressBook addressBook) {
        //获取当前用户id，根据id封装条件
        addressBook.setUserId(BaseContext.getThreadLocal());
        addressBookService.save(addressBook);
        return R.success(null);
    }

    //PUT      设置默认值
    //	http://localhost:8080/addressBook/default

    @Transactional
    @PutMapping("/default")
    public R updateDefault(@RequestBody AddressBook addressBook) {
        //根据用户id和参数id设置默认值，
        LambdaUpdateWrapper<AddressBook> addressBookLambdaUpdateWrapper = Wrappers.lambdaUpdate(AddressBook.class);
        addressBookLambdaUpdateWrapper.eq(AddressBook::getUserId, BaseContext.getThreadLocal())
                .set(AddressBook::getIsDefault, 0);
        //1. 将该用户所有地址的默认值都设置为0
        addressBookService.update(addressBookLambdaUpdateWrapper);
        //2. 再把要设置默认值的地址改为1
        addressBook.setIsDefault(1);
        addressBookService.updateById(addressBook);
        return R.success(null);
    }

    //GET       根据id查询，数据回显
    //	http://localhost:8080/addressBook/1465275729880137730
    @GetMapping("/{id}")
    public R<AddressBook> getById(@PathVariable Long id) {
        AddressBook addressBook = addressBookService.getById(id);
        if (addressBook != null) {
            return R.success(addressBook);
        }
        return R.error("没有找到该对象");
    }

    //PUT       修改数据
    //	http://localhost:8080/addressBook
    @PutMapping
    public R update(@RequestBody AddressBook addressBook) {
        addressBookService.updateById(addressBook);
        return R.success(null);
    }

    //GET       获取默认地址
    //	http://localhost:8080/addressBook/default
    @GetMapping("/default")
    public R<AddressBook> getByDefault() {
        //根据userid查询地址默认值
        LambdaQueryWrapper<AddressBook> addressBookLambdaQueryWrapper = Wrappers.lambdaQuery(AddressBook.class)
                .eq(AddressBook::getIsDefault, 1)
                .eq(BaseContext.getThreadLocal() != null, AddressBook::getUserId, BaseContext.getThreadLocal());
        AddressBook addressBook = addressBookService.getOne(addressBookLambdaQueryWrapper);
        if (addressBook != null){
            return R.success(addressBook);
        }
        return R.error("没有默认地址");
    }

    //DELETE        删除地址
    //	http://localhost:8080/addressBook?ids=1465599081169584130
    @DeleteMapping
    public R remove(Long ids){
        addressBookService.removeById(ids);
        return R.success(null);
    }


}
