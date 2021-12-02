package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 插入操作
     *
     * @param category
     * @return
     */
    @PostMapping
    public R<Category> insert(@RequestBody Category category) {
        log.info("新增菜品 {}", category);
        categoryService.save(category);
        return R.success(null);
    }

    //GET   分页查询
    //	http://localhost:8080/category/page?page=1&pageSize=10
    @GetMapping("/page")
    public R<Page<Category>> selectByPage(@RequestParam(required = false, defaultValue = "1") Integer page,
                                          @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        log.info("分页查询，分{}页，每页{}条数据", page, pageSize);
        //构建page对象
        Page<Category> pageInfo = new Page<>(page, pageSize);
        //准备查询条件
        LambdaQueryWrapper<Category> wrapper = Wrappers.lambdaQuery(Category.class);
        wrapper.orderByAsc(Category::getSort);
        Page<Category> categoryPage = categoryService.page(pageInfo, wrapper);
        return R.success(categoryPage);
    }

    //PUT       修改菜品
    //	http://localhost:8080/category
    @PutMapping
    public R<Category> update(@RequestBody Category category) {
        categoryService.updateById(category);
        return R.success(null);
    }

    //GET   根据id查询
    //	http://localhost:8080/category/1397844263642378242
    @GetMapping("{id}")
    public R<Category> selectById(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        return R.success(category);
    }

    //DELETE        删除操作，删除表需要先查有没有其他的关联表
    //	http://localhost:8080/category/1463784012450435074
    @DeleteMapping("/{id}")
    public R<Category> deleteById(@PathVariable Long id) {
        categoryService.remove(id);
        return R.success(null);
    }

    //GET       菜品管理显示菜品列表 type = 1
    //	http://localhost:8080/category/list?type=1
    @GetMapping("/list")
    public R<List<Category>> select(Category category) {
        //构建查询条件
        LambdaQueryWrapper<Category> wrapper = Wrappers.lambdaQuery(Category.class);
        wrapper.eq(category.getType() != null, Category::getType, category.getType());
        wrapper.orderByAsc(Category::getSort);
        wrapper.orderByDesc(Category::getUpdateTime);
        //执行查询操作
        List<Category> categories = categoryService.list(wrapper);
        return R.success(categories);
    }
}
