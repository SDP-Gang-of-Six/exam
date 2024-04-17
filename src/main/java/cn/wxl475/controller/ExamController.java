package cn.wxl475.controller;

import cn.wxl475.pojo.exam.Exam;
import cn.wxl475.pojo.exam.ExamCreater;
import cn.wxl475.pojo.Result;
import cn.wxl475.pojo.exam.ExamsWithUserId;
import cn.wxl475.service.ExamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    @PostMapping("/startExam")
    public Result startExam(@RequestBody Exam exam) {
        Long examId;
        try {
            examId = examService.startExam(exam);
        } catch (Exception e) {
            log.info(Arrays.toString(e.getStackTrace()));
            return Result.error(e.getMessage());
        }
        return Result.success(examId);
    }
    @PostMapping("/setExam")
    public Result setExam(@RequestBody Exam exam) {
        return Result.success(examService.setExam(exam));
    }
    @PostMapping("/setExams")
    public Result setExams(@RequestBody List<Exam> exams) {
        return Result.success(examService.setExams(exams));
    }
    @PostMapping("/setExamsForUsers")
    public Result setExamsForUsers(@RequestBody ExamsWithUserId exams) {
        return Result.success(examService.setExamsForUsers(exams));
    }

    @PostMapping("/saveExam")
    public Result saveExam(@RequestBody ExamCreater examCreater) {
        try {
            examService.saveExam(examCreater);
        } catch (Exception e) {
            log.info(Arrays.toString(e.getStackTrace()));
            return Result.error(e.getMessage());
        }
        return Result.success();
    }
    @PostMapping("/submitPaper")
    public Result submitPaper(@RequestBody ExamCreater examCreater) {
        Integer score;
        try {
            score = examService.submitPaper(examCreater);
        } catch (Exception e) {
            log.info(Arrays.toString(e.getStackTrace()));
            return Result.error(e.getMessage());
        }
        return Result.success(score);
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
    @PostMapping("/deleteExams")
    public Result deleteExams(@RequestBody List<Long> examIds) {
        examService.deleteExams(examIds);
        return Result.success();
    }
}
