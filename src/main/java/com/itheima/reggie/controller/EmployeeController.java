package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     *  登录操作
     * @param employee 浏览器传过来的用户名密码数据
     * @param request   用来登录成功获取session
     * @return 响应浏览器的数据
     */
    @PostMapping("/login")
    public R<Employee> login(@RequestBody Employee employee, HttpServletRequest request){
        //获取用户信息,把明文密码加密成密文
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        //根据用户名查找用户
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getUsername,employee.getUsername());
        Employee one = employeeService.getOne(wrapper);
        //判断用户是否找到
        if(one == null){
            log.info("用户不存在");
            return R.error("用户名或密码错误");
        }
        //如果找到判断密码是否匹配,不匹配返回错误信息
        if(!password.equals(one.getPassword())){
            log.info("密码错误");
            return R.error("用户名或密码错误");
        }
        //判断用户是否被禁用
        if(one.getStatus() == 0) {
            return R.error("账户被禁用，请联系管理员");
        }
        //满足所有条件，登录成功
        request.getSession().setAttribute("employeeId",one.getId());
        return R.success(one);
    }

    @PostMapping("/logout")
    public R<Employee> logout(){
        log.info("用户退出登录");
        return R.success(null);
    }

//    @PostMapping
//    public R<Employee> addEmployee(@RequestBody Employee employee, HttpServletRequest request){
//        //设置前端页面传过来缺失的数据
//        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8)));
//        employee.setStatus(1);
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
//        //从session中获取当前操作者的id
//        Long empId = (Long) request.getSession().getAttribute("employeeId");
//        employee.setCreateUser(empId);
//        employee.setUpdateUser(empId);
//        //执行添加操作
//        employeeService.save(employee);
//        return R.success(null);
//    }
//
//    @GetMapping("/page")
//    public R<Page<Employee>> selectByPage(
//            @RequestParam(required = false,defaultValue = "1") int page,
//            @RequestParam(required = false,defaultValue = "10")int pageSize, String name){
//        //封装一个page对象
//        Page<Employee> pageInfo = new Page<>(page,pageSize);
//        //按照更新时间的降序排序
//        LambdaQueryWrapper<Employee> wrapper = Wrappers.lambdaQuery(Employee.class);
//        wrapper.like(name != null && !name.equals(""),Employee::getName,name);
//        wrapper.orderByDesc(Employee::getUpdateTime);
//        employeeService.page(pageInfo,wrapper);
//        log.info("查询成功");
//        return R.success(pageInfo);
//    }
//
//    @PutMapping
//    public R<Employee> setStatus(@RequestBody Employee employee,HttpServletRequest request){
//        //获取当前操作人的信息
//        Long empId = (Long) request.getSession().getAttribute("employeeId");
//        employee.setUpdateTime(LocalDateTime.now());
//        employee.setUpdateUser(empId);
//        //根据id来修改用户的状态
//        employeeService.updateById(employee);
//        return R.success(null);
//    }
//
//    @GetMapping("/{id}")
//    public R<Employee> getById(@PathVariable long id){
//        log.info("数据回显");
//        Employee emp = employeeService.getById(id);
//        if(emp != null){
//            return R.success(emp);
//        }
//        return R.error("该员工不存在，请刷新重试");
//
//    }


    ///employee/page?page=1&pageSize=5 GET      分页查询
    @GetMapping("/page")
    public R<Page<Employee>> selectByPage(
            @RequestParam(required = false,defaultValue = "1") Integer page,
            @RequestParam(required = false,defaultValue = "10") Integer pageSize,
            String name
    ){
        log.info("分页查询，分{}页，每页{}条数据",page,pageSize);
        Page<Employee> pageInfo = new Page<>(page,pageSize);
        LambdaQueryWrapper<Employee> wrapper = Wrappers.lambdaQuery(Employee.class);
        wrapper.like(StringUtils.isNotEmpty(name),Employee::getName,name);
        wrapper.orderByDesc(Employee::getUpdateTime);
        Page<Employee> employeePage = employeeService.page(pageInfo, wrapper);
        return R.success(employeePage);
    }

    //POST /employee        添加操作
    @PostMapping
    public R<Employee> addEmployee(@RequestBody Employee employee,HttpServletRequest request){
        log.info("完善提交信息");
        //完善提交信息
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8)));
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
//        Long empId = (Long) request.getSession().getAttribute("employeeId");
//        employee.setCreateUser(empId);
//        employee.setUpdateUser(empId);
        employeeService.save(employee);
        return R.success(null);
    }
    //PUT       修改操作
    //	http://localhost:8080/employee
    @PutMapping
    public R<Employee> update(@RequestBody Employee employee, HttpServletRequest request){
//        employee.setUpdateTime(LocalDateTime.now());
//        Long empId = (Long) request.getSession().getAttribute("employeeId");
//        employee.setUpdateUser(empId);
        employeeService.updateById(employee);
        return R.success(null);
    }

    //GET       根据id查找，数据回显
    //	http://localhost:8080/employee/1463036231616098305
    @GetMapping("/{id}")
    public R<Employee> selectById(@PathVariable Long id){
        Employee employee = employeeService.getById(id);
        if(employee != null){
            return R.success(employee);
        }
        return R.error("该用户不存在");
    }
}
