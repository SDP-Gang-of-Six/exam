package cn.wxl475.repo;

import cn.wxl475.pojo.Paper;
import cn.wxl475.pojo.Question;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PaperEsRepo extends ElasticsearchRepository<Paper, Long> {
}
