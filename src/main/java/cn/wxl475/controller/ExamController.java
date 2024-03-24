package cn.wxl475.controller;

import cn.wxl475.pojo.Exam;
import cn.wxl475.pojo.ExamCreater;
import cn.wxl475.pojo.Result;
import cn.wxl475.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    @PostMapping("/startExam")
    public Result startExam(@RequestBody Exam exam) {
        return Result.success(examService.startExam(exam));
    }
    @PostMapping("/setExam")
    public Result setExam(@RequestBody Exam exam) {
        return Result.success(examService.setExam(exam));
    }
    @PostMapping("/setExams")
    public Result setExams(@RequestBody List<Exam> exams) {
        return Result.success(examService.setExams(exams));
    }
    @PostMapping("/saveExam")
    public Result saveExam(@RequestBody ExamCreater examCreater) {
        examService.saveExam(examCreater);
        return Result.success();
    }
    @PostMapping("/submitPaper")
    public Result submitPaper(@RequestBody ExamCreater examCreater) {
        return Result.success(examService.submitPaper(examCreater));
    }
    @GetMapping("/getExamDetail")
    public Result getExamDetail(@RequestParam("examId") Long examId) {
        return Result.success(examService.getExamDetail(examId));
    }
    @PostMapping("/getExams")
    public Result getExams(@RequestBody Exam paper) {
        return Result.success();
    }
}
