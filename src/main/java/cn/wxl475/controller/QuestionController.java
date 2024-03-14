package cn.wxl475.controller;

import cn.wxl475.pojo.Question;
import cn.wxl475.pojo.Result;
import cn.wxl475.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
    public Result deleteQuestion(@RequestBody()  List<Long> questionIds) {
        ArrayList<Long> arrayList = new ArrayList<>(questionIds);
        questionService.deleteQuestion(arrayList);
        return Result.success();
    }
    @PostMapping("/updateQuestion")
    public Result updateQuestion(@RequestBody Question question) {
        return Result.success(questionService.updateQuestion(question));
    }
    @GetMapping("/getQuestions")
    public Result getQuestions(@RequestParam(value = "allField", required = false) String allField, @RequestParam(value = "tag", required = false) String tag) {
        return Result.success(questionService.getQuestions(allField, tag));
    }
    @GetMapping("/getQuestionById")
    public Result getQuestionById(@RequestParam("questionId") Long questionId) {
        return Result.success(questionService.getQuestionById(questionId));
    }
}
