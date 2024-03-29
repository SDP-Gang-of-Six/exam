package cn.wxl475.mapper;

import cn.wxl475.pojo.exam.ExamDetail;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface ExamDetailMapper extends BaseMapper<ExamDetail> {
    @Select("select * from exam.examdetail where exam_id = #{examId}")
    List<ExamDetail> selectByExamId(Long examId);

    void deleteByExamIds(Long[] examIds);
}
