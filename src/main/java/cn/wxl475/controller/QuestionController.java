package cn.wxl475.controller;

import cn.wxl475.pojo.Question;
import cn.wxl475.pojo.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

@RestController
@RequestMapping("/exam")
public class QuestionController {

    @PostMapping("/createQuestion")
    public Result createQuestion(@RequestBody Question question) {
        return Result.success();
    }
}
