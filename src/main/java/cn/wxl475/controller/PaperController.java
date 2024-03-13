package cn.wxl475.controller;

import cn.wxl475.pojo.Paper;
import cn.wxl475.pojo.Result;
import cn.wxl475.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/exam")
public class PaperController {

    @Autowired
    private PaperService paperService;

    @PostMapping("/createPaper")
    public Result createPaper(@RequestBody Paper paper) {
        return Result.success();
    }
    @PostMapping("/deletePaper")
    public Result deletePaper(@RequestBody Paper paper) {
        return Result.success();
    }
    @PostMapping("/updatePaper")
    public Result updatePaper(@RequestBody Paper paper) {
        return Result.success();
    }
    @PostMapping("/getPapers")
    public Result getPapers(@RequestBody Paper paper) {
        return Result.success();
    }
    @GetMapping("/getPaperById")
    public Result getPaperById(@RequestParam Long paperId) {
        return Result.success();
    }
}
