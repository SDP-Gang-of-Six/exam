package cn.wxl475.controller;

import cn.wxl475.pojo.Question;
import cn.wxl475.pojo.Result;
import cn.wxl475.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/exam")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @PostMapping("/createQuestion")
    public Result createQuestion(@RequestBody Question question) {
        return Result.success(questionService.createQuestion(question));
    }
    @PostMapping("/deleteQuestion")
    public Result deleteQuestion(@RequestBody Question question) {
        return Result.success();
    }
    @PostMapping("/updateQuestion")
    public Result updateQuestion(@RequestBody Question question) {
        return Result.success();
    }
    @PostMapping("/getQuestions")
    public Result getQuestions(@RequestBody Question question) {
        return Result.success();
    }
    @GetMapping("/getQuestionById")
    public Result getQuestionById(@RequestParam Long questionId) {
        return Result.success();
    }
}
