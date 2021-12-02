package com.itheima.reggie.controller;


import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

    @Value("${reggie.img-path}")
    private String imgPath;

    //POST
    //	http://localhost:8080/common/upload
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file) {
        log.info("文件上传{}", file);
        //获取文件名，根据文件名获取文件后缀,再随机生成uuid
        String originalFilename = file.getOriginalFilename();
        String extend = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uuid = UUID.randomUUID().toString() + extend;
        //判断文件夹是否存在不存在就创建该文件夹
        File dir = new File(imgPath+uuid);
        if(!dir.exists()){
            dir.mkdirs();
        }
        //把文件转存到磁盘
        try {
            file.transferTo(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return R.success(uuid);
    }

    //GET
    //	http://localhost:8080/common/download?name=null
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response){
        //拼接文件的存放位置
        String Path = imgPath + name;
        //把把文件的数据拷贝成二进制数组
        try {
            byte[] bytes = FileCopyUtils.copyToByteArray(new File(Path));
            response.getOutputStream().write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
