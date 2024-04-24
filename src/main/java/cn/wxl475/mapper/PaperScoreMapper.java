package cn.wxl475.mapper;

import cn.wxl475.pojo.exam.PaperScore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.ArrayList;

@Mapper
public interface PaperScoreMapper extends BaseMapper<PaperScore> {

    void deleteByPaperIds(Long[] arrayList);

    @Select("select * from exam.paperScore where paper_id = #{paperId}")
    ArrayList<PaperScore> selectByPaperId(Long paperId);

    @Select("select * from exam.paperScore where question_id = #{questionId}")
    ArrayList<PaperScore> selectByQuestionId(Long questionId);
}
