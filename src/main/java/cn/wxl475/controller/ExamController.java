package cn.wxl475.controller;

import cn.wxl475.pojo.exam.Exam;
import cn.wxl475.pojo.exam.ExamCreater;
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
    @GetMapping("/getExams")
    public Result getExams(@RequestParam(value = "userId", required = false) Long userId,
                           @RequestParam(value = "paperId", required = false) Long paperId,
                           @RequestParam(value = "status", required = false) Boolean status,
                           @RequestParam(value = "pageNum", required = false) Integer pageNum,
                           @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        if(pageNum<=0||pageSize<=0){
            return Result.error("页码或页大小不合法");
        }
        return Result.success(examService.getExams(userId, paperId, status, pageNum, pageSize));
    }
}
