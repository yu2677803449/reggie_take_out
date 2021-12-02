package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetMealDishService;
import com.itheima.reggie.service.SetMealService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetMealController {

    @Autowired
    private SetMealService setMealService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetMealDishService setMealDishService;

    //POST
    //	http://localhost:8080/setmeal
    @PostMapping
    public R save(@RequestBody SetmealDto dto) {
        setMealService.saveWithDish(dto);
        return R.success("添加成功");
    }

    //GET
    //	http://localhost:8080/setmeal/page?page=1&pageSize=10
    @GetMapping("/page")
    public R<Page<SetmealDto>> page(@RequestParam(required = false, defaultValue = "1") Integer page,
                                    @RequestParam(required = false, defaultValue = "10") Integer pageSize, String name) {
        //查询Setmeal表的基本信息
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        //构建查询条件
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = Wrappers.lambdaQuery(Setmeal.class);
        setmealLambdaQueryWrapper.like(StringUtils.isNotEmpty(name), Setmeal::getName, name);
        setmealLambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        setMealService.page(pageInfo, setmealLambdaQueryWrapper);

        //构建dto数据
        Page<SetmealDto> dtoPage = new Page<>();
        //封装total属性
        dtoPage.setTotal(pageInfo.getTotal());
        //封装records
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> setmealDtoList = records.stream().map(setmeal -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(setmeal, setmealDto);
            //根据id查询categoryName
            Category category = categoryService.getById(setmeal.getCategoryId());
            if (category != null) {
                setmealDto.setCategoryName(category.getName());
            }
            return setmealDto;
        }).collect(Collectors.toList());
        dtoPage.setRecords(setmealDtoList);
        return R.success(dtoPage);
    }

    //DELETE
    //	http://localhost:8080/setmeal?ids=1464545816587837441
    @DeleteMapping
    public R delete(@RequestParam List<Long> ids) {
        setMealService.removeWithDish(ids);
        return R.success("成功");
    }

    //GET
    //	http://localhost:8080/setmeal/list?categoryId=1413342269393674242&status=1
    @GetMapping("/list")
    public R list(Setmeal setmeal) {
        //构造查询条件
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = Wrappers.lambdaQuery(Setmeal.class);
        setmealLambdaQueryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId())
                .eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus())
                .orderByDesc(Setmeal::getUpdateTime);
        //执行查询
        List<Setmeal> list = setMealService.list(setmealLambdaQueryWrapper);
        return R.success(list);
    }

    //POST
    //	http://localhost:8080/setmeal/status/0?ids=1465557801370677249
    @PostMapping("/status/{status}")
    public R updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
        //直接根据ids修改套餐的status信息
        LambdaUpdateWrapper<Setmeal> setmealLambdaUpdateWrapper = Wrappers.lambdaUpdate(Setmeal.class)
                .in(ids != null, Setmeal::getId, ids)
                .set(status != null, Setmeal::getStatus, status);
        setMealService.update(setmealLambdaUpdateWrapper);
        return R.success(null);
    }

    //GET       数据回显
    //	http://localhost:8080/setmeal/1465528343100157953
    @GetMapping("/{id}")
    public R<SetmealDto> getById(@PathVariable Long id) {
        //根据id查询套餐基本信息
        Setmeal setmealInfo = setMealService.getById(id);
        //根据id查询口味信息
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = Wrappers.lambdaQuery(SetmealDish.class)
                .eq(id != null, SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishList = setMealDishService.list(setmealDishLambdaQueryWrapper);
        //拷贝基本信息
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmealInfo, setmealDto);
        //拷贝categoryName，根据id查询categoryId
        Category category = categoryService.getById(setmealInfo.getCategoryId());
        if (category != null) {
            setmealDto.setCategoryName(category.getName());
        }
        //拷贝list
        setmealDto.setSetmealDishes(setmealDishList);
        //返回dto数据
        return R.success(setmealDto);
    }

    //PUT       修改套餐
    //	http://localhost:8080/setmeal
    @PutMapping
    public R update(@RequestBody SetmealDto setmealDto){
        setMealService.update(setmealDto);
        return R.success(null);
    }

}
