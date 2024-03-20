package cn.wxl475.controller;

import cn.wxl475.pojo.Exam;
import cn.wxl475.pojo.ExamCreater;
import cn.wxl475.pojo.Result;
import cn.wxl475.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    @PostMapping("/startExam")
    public Result startExam(@RequestBody Exam exam) {
        return Result.success(examService.startExam(exam));
    }
    @PostMapping("/getExams")
    public Result getExams(@RequestBody Exam paper) {
        return Result.success();
    }
    @PostMapping("/saveExam")
    public Result saveExam(@RequestBody Exam exam) {

        return Result.success();
    }
    @PostMapping("/submitPaper")
    public Result submitPaper(@RequestBody ExamCreater examCreater) {
        examService.submitPaper(examCreater);
        return Result.success();
    }
    @PostMapping("/getExamDetail")
    public Result getExamDetail(@RequestBody Long examId) {
        return Result.success();
    }
}
