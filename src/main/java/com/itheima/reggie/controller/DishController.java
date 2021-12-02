package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishFlavorService dishFlavorService;

    //POST
    //	http://localhost:8080/dish
    @PostMapping
    public R saveDish(@RequestBody DishDto dishDto) {
        //多表操作，在业务层实现
        dishService.saveWithFlavor(dishDto);
        return R.success("添加成功");
    }

    //GET
    //	http://localhost:8080/dish/page?page=1&pageSize=10
    @GetMapping("/page")
    public R<Page<DishDto>> page(@RequestParam(required = false, defaultValue = "1") Integer page,
                                 @RequestParam(required = false, defaultValue = "10") Integer pageSize, String name) {
        //构建page对象
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        //构建查询条件
        LambdaQueryWrapper<Dish> dtoLambdaQueryWrapper = Wrappers.lambdaQuery(Dish.class);
        dtoLambdaQueryWrapper.like(name != null && !name.equals(""), Dish::getName, name);
        //排序
        dtoLambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);
        //执行查询操作
        dishService.page(pageInfo, dtoLambdaQueryWrapper);

        //构建新的dto容器，存放旧数据
        Page<DishDto> dtoPage = new Page<>();
        //设置dtoPage 的total
        dtoPage.setTotal(pageInfo.getTotal());
        //设置dtoPage的records
        List<Dish> records = pageInfo.getRecords();
        List<DishDto> dishDtoList = records.stream().map(dish -> {
            //new一个dto对象
            DishDto dishDto = new DishDto();
            //把原数据拷过来
            BeanUtils.copyProperties(dish, dishDto);
            //根据id查询categoryId
            Category category = categoryService.getById(dish.getCategoryId());
            if (category != null) {
                dishDto.setCategoryName(category.getName());
            }
            return dishDto;
        }).collect(Collectors.toList());
        dtoPage.setRecords(dishDtoList);
        return R.success(dtoPage);
    }

    //GET       修改数据回显
    //	http://localhost:8080/dish/1464193125474902017
    @GetMapping("/{id}")
    public R<DishDto> getById(@PathVariable Long id) {
        //调用service方法
        DishDto byIdWithFlavor = dishService.getByIdWithFlavor(id);
        return R.success(byIdWithFlavor);
    }

    //PUT       修改操作
    //	http://localhost:8080/dish
    @PutMapping
    public R updateDish(@RequestBody DishDto dishDto) {
        dishService.updateWithFlavor(dishDto);
        return R.success(null);
    }

    //GET   查询菜品列表
    //	http://localhost:8080/dish/list?categoryId=1397844263642378242
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        //构造条件
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = Wrappers.lambdaQuery(Dish.class);
        dishLambdaQueryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        dishLambdaQueryWrapper.eq( Dish::getStatus, 1);
        dishLambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);
        //根据条件查询数据
        List<Dish> list = dishService.list(dishLambdaQueryWrapper);
        //组装数据，封装口味信息
        List<DishDto> dishDtos = list.stream().map(dish1 -> {
            //把dish的属性拷贝到dto中
            DishDto dto = new DishDto();
            BeanUtils.copyProperties(dish1, dto);
            //根据CategoryId查询CategoryName
            Category category = categoryService.getById(dto.getCategoryId());
            if (category != null) {
                dto.setCategoryName(category.getName());
            }
            //根据id查询口味儿信息
            LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = Wrappers.lambdaQuery(DishFlavor.class);
            dishFlavorLambdaQueryWrapper.eq(dto.getId() != null, DishFlavor::getDishId, dto.getId());
            List<DishFlavor> dishFlavors = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
            dto.setFlavors(dishFlavors);
            return dto;
        }).collect(Collectors.toList());
        //返回数据
        return R.success(dishDtos);
    }

    //POST      批量停售操作
    //	http://localhost:8080/dish/status/0?ids=1464193125474902017
    @PostMapping("/status/{status}")
    public R updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
        dishService.updateStatus(status, ids);
        return R.success(null);
    }

    //DELETE        批量删除操作
    //	http://localhost:8080/dish?ids=1413385247889891330
    @DeleteMapping
    public R remove(@RequestParam List<Long> ids){
        dishService.remove(ids);
        return R.success(null);
    }




}
