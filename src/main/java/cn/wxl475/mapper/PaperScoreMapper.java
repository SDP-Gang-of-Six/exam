package cn.wxl475.mapper;

import cn.wxl475.pojo.PaperScore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;

@Mapper
public interface PaperScoreMapper extends BaseMapper<PaperScore> {
    @Delete("delete from paperScore where paper_id in (#{arrayList})")
    void deleteByPaperIds(ArrayList<Long> arrayList);
}
